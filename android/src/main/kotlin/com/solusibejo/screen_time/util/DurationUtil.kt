package com.solusibejo.screen_time.util

import java.time.Duration

object DurationUtil {
    fun Duration.inString(): String {
        val hours = toHours()
        val minutes = toMinutes() % 60
        val seconds = seconds % 60

        return buildString {
            if (hours > 0) append("$hours hour${if (hours > 1) "s" else ""} ")
            if (minutes > 0) append("$minutes minute${if (minutes > 1) "s" else ""} ")
            if (seconds > 0 || (hours == 0L && minutes == 0L)) {
                append("$seconds second${if (seconds > 1) "s" else ""}")
            }
        }.trim()
    }
}