package com.ilseon.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class UsageStatsReader(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * A proxy for screen-on events or phone pickups. This counts how many times an activity
     * was resumed in the given time frame.
     */
    fun getPhonePickups(startTime: Long, endTime: Long): Int {
        if (!hasUsageStatsPermission()) {
            // Consider logging this or handling it more gracefully
            return -1
        }

        val events = usageStatsManager.queryEvents(startTime, endTime)
        var pickupCount = 0

        // This logic is a proxy. A "pickup" could be defined as the start of a new series of events
        // after a period of inactivity. For now, we'll count activity resumptions.
        while (events.hasNextEvent()) {
            val event = android.app.usage.UsageEvents.Event()
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                pickupCount++
            }
        }
        return pickupCount
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
