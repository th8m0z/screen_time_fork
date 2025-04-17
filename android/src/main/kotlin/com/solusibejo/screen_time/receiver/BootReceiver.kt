package com.solusibejo.screen_time.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.solusibejo.screen_time.service.BlockAppService
import com.solusibejo.screen_time.worker.ServiceRestartWorker
import java.util.concurrent.TimeUnit

/**
 * Receiver that handles device boot events to restore the app blocking state
 * This ensures that app blocking continues even after device restarts
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")

        // Handle both boot completed and quick boot actions
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Device boot completed, checking if we need to restore blocking")
            
            // Delay the service start slightly to ensure system is fully booted
            Handler(Looper.getMainLooper()).postDelayed({
                restoreBlockingStateAfterBoot(context)
            }, 5000) // 5 second delay
        }
    }
    
    /**
     * Restores the blocking state after device boot if needed
     */
    private fun restoreBlockingStateAfterBoot(context: Context) {
        try {
            val sharedPreferences = context.getSharedPreferences(
                BlockAppService.PREF_NAME,
                Context.MODE_PRIVATE
            )

            val isBlocking = sharedPreferences.getBoolean(BlockAppService.KEY_IS_BLOCKING, false)
            val blockEndTime = sharedPreferences.getLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
            val blockedPackages = sharedPreferences.getStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf()) ?: setOf()
            
            Log.d(TAG, "Checking blocking state: isBlocking=$isBlocking, remaining time=${blockEndTime - System.currentTimeMillis()}ms")
            
            // Check if we should restore blocking
            if (isBlocking && blockEndTime > System.currentTimeMillis() && blockedPackages.isNotEmpty()) {
                Log.d(TAG, "Restoring block state after boot")
                
                // Try both methods to ensure service starts
                startBlockingService(context)
                scheduleServiceStart(context)
                
                // Also reschedule the unblock alarm
                AlarmReceiver.scheduleUnblock(context, blockEndTime - System.currentTimeMillis())
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
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring blocking state", e)
        }
    }
    
    /**
     * Starts the blocking service directly
     */
    private fun startBlockingService(context: Context) {
        try {
            val serviceIntent = Intent(context, BlockAppService::class.java)
            serviceIntent.action = BlockAppService.ACTION_START_BLOCKING
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "Started blocking service after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service directly", e)
        }
    }
    
    /**
     * Schedules a work request to start the service as a backup method
     */
    private fun scheduleServiceStart(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            
            // Create a one-time work request with a short delay
            val workRequest = OneTimeWorkRequestBuilder<ServiceRestartWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()
            
            workManager.enqueue(workRequest)
            Log.d(TAG, "Scheduled service start work after boot")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service start work", e)
        }
    }
}