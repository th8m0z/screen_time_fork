package com.solusibejo.screen_time.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import com.solusibejo.screen_time.service.BlockAppService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "onReceive: ${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "onReceive: ACTION_BOOT_COMPLETED")
            val serviceIntent = Intent(context, BlockAppService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}