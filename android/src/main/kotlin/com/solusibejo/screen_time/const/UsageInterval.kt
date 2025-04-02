package com.solusibejo.screen_time.const

import android.app.usage.UsageStatsManager

enum class UsageInterval(val type: Int) {
    DAILY(UsageStatsManager.INTERVAL_DAILY),
    WEEKLY(UsageStatsManager.INTERVAL_WEEKLY),
    MONTHLY(UsageStatsManager.INTERVAL_MONTHLY),
    YEARLY(UsageStatsManager.INTERVAL_YEARLY),
    BEST(UsageStatsManager.INTERVAL_BEST)
}