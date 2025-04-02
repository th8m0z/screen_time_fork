package com.solusibejo.screen_time.util

object IntExtension {
    fun timeInString(hour: Int): String {
        return if(hour < 10){
            "0$hour"
        } else {
            hour.toString()
        }
    }
}