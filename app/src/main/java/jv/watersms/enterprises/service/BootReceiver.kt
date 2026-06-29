package jv.watersms.enterprises.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import jv.watersms.enterprises.data.AppDatabase
import jv.watersms.enterprises.data.CampaignRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for [android.intent.action.BOOT_COMPLETED] and resumes any campaigns
 * that were in SENDING or PAUSED state when the device shut down.
 *
 * Uses [goAsync] to keep the receiver alive while the Room DB query completes,
 * then fires [SmsSendingService.ACTION_RESUME] for each incomplete campaign.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i("BootReceiver", "Device boot completed — checking for incomplete campaigns")

        // goAsync extends the receiver lifecycle beyond the default ~10s window
        // so our coroutine has time to query Room DB
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = CampaignRepository(database.campaignDao())

                val incompleteCampaigns = repository.getCampaignsByStatus("SENDING") +
                        repository.getCampaignsByStatus("PAUSED")

                if (incompleteCampaigns.isEmpty()) {
                    Log.i("BootReceiver", "No incomplete campaigns found — nothing to resume")
                    return@launch
                }

                Log.i("BootReceiver", "Found ${incompleteCampaigns.size} incomplete campaign(s) — resuming")

                for (campaign in incompleteCampaigns) {
                    val resumeIntent = Intent(context, SmsSendingService::class.java).apply {
                        action = SmsSendingService.ACTION_RESUME
                        putExtra(SmsSendingService.EXTRA_CAMPAIGN_ID, campaign.id)
                    }
                    // startForegroundService is API 26+; fall back to startService for 24-25
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(resumeIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        context.startService(resumeIntent)
                    }
                    Log.i("BootReceiver", "Fired ACTION_RESUME for campaign #${campaign.id} (${campaign.name})")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to resume campaigns on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
