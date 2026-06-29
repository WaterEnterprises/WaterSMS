package jv.watersms.enterprises.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import jv.watersms.enterprises.data.AppDatabase
import jv.watersms.enterprises.data.CampaignRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AlarmManager-based watchdog that periodically checks whether the
 * [SmsSendingService] is alive when there are active campaigns.
 *
 * This guards against OEM-specific aggressive background killing that
 * bypasses `START_STICKY` (common on Xiaomi, Huawei, OnePlus, Samsung).
 *
 * ## How it works
 * 1. Fires every [WATCHDOG_INTERVAL_MS] via [AlarmManager.setInexactRepeating]
 * 2. Queries Room DB for campaigns in SENDING or PAUSED state
 * 3. If active campaigns exist but the service isn't running, restarts it
 * 4. If no active campaigns, the alarm is a no-op (no unnecessary wakeups)
 */
class SmsWatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WATCHDOG_TICK) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = CampaignRepository(database.campaignDao())

                val activeCampaigns = repository.getCampaignsByStatus("SENDING") +
                        repository.getCampaignsByStatus("PAUSED")

                if (activeCampaigns.isEmpty()) {
                    // No active campaigns — nothing to watch
                    return@launch
                }

                // Fire resume intent for each active campaign.
                // SmsSendingService.startSending() already guards against
                // duplicate starts if the service is already running.
                Log.i("SmsWatchdog", "${activeCampaigns.size} campaign(s) active — ensuring service is alive")

                for (campaign in activeCampaigns) {
                    val resumeIntent = Intent(context, SmsSendingService::class.java).apply {
                        action = SmsSendingService.ACTION_RESUME
                        putExtra(SmsSendingService.EXTRA_CAMPAIGN_ID, campaign.id)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(resumeIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        context.startService(resumeIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsWatchdog", "Watchdog tick failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    companion object {
        const val ACTION_WATCHDOG_TICK = "jv.watersms.enterprises.action.WATCHDOG_TICK"
        const val WATCHDOG_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

        private const val REQUEST_CODE_WATCHDOG = 2028

        /**
         * Schedules the repeating watchdog alarm.
         * Call this once from [android.app.Application.onCreate].
         */
        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SmsWatchdogReceiver::class.java).apply {
                action = ACTION_WATCHDOG_TICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WATCHDOG,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use ELAPSED_REALTIME_WAKEUP so the alarm fires even when the device is asleep
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL_MS,
                WATCHDOG_INTERVAL_MS,
                pendingIntent
            )

            Log.i("SmsWatchdog", "Watchdog scheduled every ${WATCHDOG_INTERVAL_MS / 60000} minutes")
        }

        /**
         * Cancels the watchdog alarm.
         * Call this when no active campaigns remain (optional cleanup).
         */
        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SmsWatchdogReceiver::class.java).apply {
                action = ACTION_WATCHDOG_TICK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_WATCHDOG,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i("SmsWatchdog", "Watchdog cancelled")
        }
    }
}
