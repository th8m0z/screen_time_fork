package com.solusibejo.screen_time.util

import android.app.ActivityManager
import android.content.Context

object ServiceUtil {
    fun isRunning(context: Context, serviceClassName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClassName == service.service.className) {
                return true
            }
        }
        return false
    }
}