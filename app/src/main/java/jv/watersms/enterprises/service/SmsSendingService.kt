package jv.watersms.enterprises.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import jv.watersms.enterprises.MainActivity
import jv.watersms.enterprises.R
import jv.watersms.enterprises.data.AppDatabase
import jv.watersms.enterprises.data.CampaignRepository
import jv.watersms.enterprises.data.Recipient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SmsSendingService : Service() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)
    private var activeSendingJob: Job? = null

    private lateinit var repository: CampaignRepository
    private var activeCampaignId: Long = -1L
    private var isSending = false
    private var isRestartingFromTaskRemoved = false

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SMS_SENT_ACTION) {
                val recipientId = intent.getLongExtra(EXTRA_RECIPIENT_ID, -1L)
                if (recipientId != -1L) {
                    val status = if (resultCode == Activity.RESULT_OK) "SENT" else "FAILED"
                    val errorMsg = when (resultCode) {
                        Activity.RESULT_OK -> null
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic Failure"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "No Service"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio Off"
                        else -> "Failed: $resultCode"
                    }
                    serviceScope.launch(Dispatchers.IO) {
                        repository.getRecipientsForCampaign(activeCampaignId)
                            .find { it.id == recipientId }
                            ?.let { recipient ->
                                val updated = recipient.copy(
                                    status = status,
                                    errorMessage = errorMsg,
                                    sentAt = System.currentTimeMillis()
                                )
                                repository.updateRecipient(updated)
                                updateNotificationAndProgress()
                            }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = CampaignRepository(database.campaignDao())

        // Register SMS sent receiver
        val filter = IntentFilter(SMS_SENT_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsSentReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(smsSentReceiver, filter)
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val campaignId = intent?.getLongExtra(EXTRA_CAMPAIGN_ID, -1L) ?: -1L

        Log.d("SmsSendingService", "onStartCommand action=$action, campaignId=$campaignId")

        // Re-foreground the service on EVERY command so Android can't hide it
        if (activeCampaignId != -1L) {
            startForeground(NOTIFICATION_ID, createNotification("Service is active..."))
        }

        when (action) {
            ACTION_START -> {
                if (campaignId != -1L) {
                    startSending(campaignId)
                }
            }
            ACTION_RESUME -> {
                if (campaignId != -1L) {
                    recreateNotificationChannel()
                    startSending(campaignId)
                }
            }
            ACTION_PAUSE -> {
                pauseSending()
            }
            ACTION_STOP -> {
                stopSending()
            }
            else -> {
                // Re-foreground with idle notification (system restart or sticky restart)
                recreateNotificationChannel()
                startForeground(NOTIFICATION_ID, createNotification("SMS Service running"))
            }
        }

        return START_STICKY
    }

    private fun startSending(campaignId: Long) {
        if (isSending && activeCampaignId == campaignId) return

        // If currently sending another campaign, pause it first
        if (isSending) {
            pauseSending()
        }

        activeCampaignId = campaignId
        isSending = true

        startForeground(NOTIFICATION_ID, createNotification("Initializing campaign sending..."))

        activeSendingJob = serviceScope.launch {
            repository.updateCampaignStatus(campaignId, "SENDING")

            // Crash recovery: fetch the last processed recipient ID to skip duplicates
            val campaign = repository.getCampaignById(campaignId)
            val skipUpToId = campaign?.lastSentRecipientId ?: -1L

            while (isSending) {
                val pendingRecipients = repository.getPendingRecipients(campaignId)
                if (pendingRecipients.isEmpty()) {
                    repository.updateCampaignStatus(campaignId, "COMPLETED")
                    showCompletionNotification()
                    stopSelf()
                    break
                }

                // Skip recipients already processed before a crash
                val recipient = pendingRecipients.firstOrNull { it.id > skipUpToId }
                    ?: pendingRecipients.first()

                sendSmsToRecipient(recipient)

                // Persist the last recipient ID for crash recovery
                repository.updateLastSentRecipientId(campaignId, recipient.id)

                val currentCampaign = repository.getCampaignById(campaignId)
                if (currentCampaign == null) {
                    stopSelf()
                    break
                }

                val minDelay = currentCampaign.minDelaySeconds.coerceAtLeast(1)
                val maxDelay = currentCampaign.maxDelaySeconds.coerceAtLeast(minDelay)
                val delaySec = if (minDelay == maxDelay) minDelay else Random.nextInt(minDelay, maxDelay + 1)

                updateNotificationAndProgress("Waiting ${delaySec}s before next SMS...")

                delay(delaySec * 1000L)
            }
        }
    }

    private suspend fun sendSmsToRecipient(recipient: Recipient) {
        val campaign = repository.getCampaignById(activeCampaignId) ?: return

        // Decode variations
        val variations = decodeVariations(campaign.variationsJson)
        val textToSend = if (variations.isNotEmpty()) {
            // Select randomly to add message variations to avoid spam filters!
            variations[Random.nextInt(variations.size)]
        } else {
            campaign.originalMessage
        }

        // Mark as SENDING in db to lock it
        repository.updateRecipient(recipient.copy(status = "SENDING", sentMessage = textToSend))
        updateNotificationAndProgress("Sending to ${recipient.name}...")

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val sentIntent = PendingIntent.getBroadcast(
                this,
                recipient.id.toInt(),
                Intent(SMS_SENT_ACTION).apply {
                    putExtra(EXTRA_RECIPIENT_ID, recipient.id)
                    setPackage(packageName)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            smsManager.sendTextMessage(recipient.phoneNumber, null, textToSend, sentIntent, null)
        } catch (e: Exception) {
            Log.e("SmsSendingService", "Error sending SMS to ${recipient.phoneNumber}", e)
            repository.updateRecipient(
                recipient.copy(
                    status = "FAILED",
                    errorMessage = e.message ?: "Exception: ${e.javaClass.simpleName}",
                    sentAt = System.currentTimeMillis()
                )
            )
            updateNotificationAndProgress()
        }
    }

    private fun decodeVariations(json: String): List<String> {
        return try {
            val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun pauseSending() {
        isSending = false
        activeSendingJob?.cancel()
        if (activeCampaignId != -1L) {
            serviceScope.launch {
                repository.updateCampaignStatus(activeCampaignId, "PAUSED")
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    private fun stopSending() {
        isSending = false
        activeSendingJob?.cancel()
        if (activeCampaignId != -1L) {
            serviceScope.launch {
                repository.updateCampaignStatus(activeCampaignId, "PAUSED")
                stopSelf()
            }
        } else {
            stopSelf()
        }
    }

    /**
     * Returns true if the app is allowed to post notifications.
     *
     * On API 33+ this checks the POST_NOTIFICATIONS runtime permission.
     * On older versions notifications are always allowed if the channel exists.
     *
     * Note: foreground services started via [startForeground] do NOT need
     * this check — only explicit [NotificationManager.notify] calls do.
     */
    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateNotificationAndProgress(customText: String? = null) {
        if (!canPostNotifications()) return
        serviceScope.launch {
            val campaign = repository.getCampaignById(activeCampaignId) ?: return@launch
            val stats = repository.getCampaignStats(activeCampaignId)
            val text = customText ?: "Progress: ${stats.sent + stats.failed} / ${stats.total} SMS sent"
            val notification = createNotification(
                contentTitle = "Campaign: ${campaign.name}",
                contentText = text,
                progressMax = stats.total,
                progressCurrent = stats.sent + stats.failed
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletionNotification() {
        if (!canPostNotifications()) return
        serviceScope.launch {
            val campaign = repository.getCampaignById(activeCampaignId) ?: return@launch
            val stats = repository.getCampaignStats(activeCampaignId)
            val notification = NotificationCompat.Builder(this@SmsSendingService, CHANNEL_ID)
                .setContentTitle("Campaign Sent successfully!")
                .setContentText("Bulk SMS send for campaign '${campaign.name}' completed. Total: ${stats.total}, Sent: ${stats.sent}, Failed: ${stats.failed}")
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(
        contentText: String,
        contentTitle: String = "Sending Bulk SMS",
        progressMax: Int = 0,
        progressCurrent: Int = 0
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause action
        val pauseIntent = Intent(this, SmsSendingService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Resume action (only shown when paused)
        val resumeIntent = Intent(this, SmsSendingService::class.java).apply {
            action = ACTION_RESUME
            putExtra(EXTRA_CAMPAIGN_ID, activeCampaignId)
        }
        val resumePendingIntent = PendingIntent.getService(
            this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = Intent(this, SmsSendingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        if (progressMax > 0) {
            builder.setProgress(progressMax, progressCurrent, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Sending Service",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Persistent notification for active SMS campaigns"
                    setShowBadge(true)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }
    }

    /**
     * Recreates the notification channel with HIGH importance if it already exists
     * at LOW importance (e.g. from a pre-update install).
     */
    private fun recreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null || existing.importance < NotificationManager.IMPORTANCE_HIGH) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Sending Service",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Persistent notification for active SMS campaigns"
                    setShowBadge(true)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Guard against re-entrant calls that could create a restart loop
        if (isRestartingFromTaskRemoved || activeCampaignId == -1L) return
        isRestartingFromTaskRemoved = true
        try {
            Log.d("SmsSendingService", "Task removed from recents — re-foregrounding campaign #$activeCampaignId")
            val restartIntent = Intent(this, SmsSendingService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_CAMPAIGN_ID, activeCampaignId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                @Suppress("DEPRECATION")
                startService(restartIntent)
            }
        } finally {
            isRestartingFromTaskRemoved = false
        }
    }

    override fun onDestroy() {
        isSending = false
        activeSendingJob?.cancel()
        job.cancel()
        try {
            unregisterReceiver(smsSentReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "SmsSendingServiceChannel"
        const val NOTIFICATION_ID = 2026
        const val COMPLETION_NOTIFICATION_ID = 2027

        const val ACTION_START = "jv.watersms.enterprises.action.START"
        const val ACTION_RESUME = "jv.watersms.enterprises.action.RESUME"
        const val ACTION_PAUSE = "jv.watersms.enterprises.action.PAUSE"
        const val ACTION_STOP = "jv.watersms.enterprises.action.STOP"

        const val EXTRA_CAMPAIGN_ID = "jv.watersms.enterprises.extra.CAMPAIGN_ID"
        const val EXTRA_RECIPIENT_ID = "jv.watersms.enterprises.extra.RECIPIENT_ID"
        const val SMS_SENT_ACTION = "jv.watersms.enterprises.SMS_SENT"
    }
}
