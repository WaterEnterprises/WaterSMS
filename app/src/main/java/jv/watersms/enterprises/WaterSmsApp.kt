package jv.watersms.enterprises

import android.app.Application
import jv.watersms.enterprises.service.SmsWatchdogReceiver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WaterSmsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Schedule the watchdog alarm that ensures SmsSendingService
        // stays alive even on aggressively battery-managed OEM ROMs
        SmsWatchdogReceiver.schedule(this)
    }
}
