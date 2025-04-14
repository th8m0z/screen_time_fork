package com.solusibejo.screen_time.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import com.solusibejo.screen_time.service.BlockAppService

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            
            val sharedPreferences = context.getSharedPreferences(
                BlockAppService.PREF_NAME,
                Context.MODE_PRIVATE
            )

            val isBlocking = sharedPreferences.getBoolean(BlockAppService.KEY_IS_BLOCKING, false)
            val blockEndTime = sharedPreferences.getLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
            
            // Check if we should restore blocking
            if (isBlocking && blockEndTime > System.currentTimeMillis()) {
                Log.d(TAG, "Restoring block state after boot")
                val serviceIntent = Intent(context, BlockAppService::class.java)
                context.startForegroundService(serviceIntent)
            } else if (isBlocking) {
                // Clean up expired block
                Log.d(TAG, "Cleaning up expired block state")
                sharedPreferences.edit().apply {
                    putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
                    putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
                    putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
                    apply()
                }
            }
        }
    }
}