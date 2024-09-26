package org.muilab.noti.summary.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.muilab.noti.summary.service.NotiListenerService
import org.muilab.noti.summary.service.ReScheduleService
import org.muilab.noti.summary.service.SummaryService

class BootUpReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d("BootUpReceiver", "onReceive")

        if (intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d("BootUpReceiver", "onReceive: intent.action=BOOT_COMPLETED")
            val rescheduleServiceIntent = Intent(context, ReScheduleService::class.java)
            context.startService(
                rescheduleServiceIntent.apply {
                    setPackage(context.packageName)
                }
            )
            val notiServiceIntent = Intent(context, NotiListenerService::class.java)
            context.startService(
                notiServiceIntent.apply {
                    setPackage(context.packageName)
                }
            )
            val summaryServiceIntent = Intent(context, SummaryService::class.java)
            context.startService(
                summaryServiceIntent.apply {
                    setPackage(context.packageName)
                }
            )
        }
    }
}