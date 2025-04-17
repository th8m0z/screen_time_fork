package com.solusibejo.screen_time.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solusibejo.screen_time.service.BlockAppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that periodically checks if the BlockAppService is running
 * and restarts it if it's not but should be running based on shared preferences
 */
class ServiceMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "ServiceMonitorWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking if BlockAppService should be running")
            
            // Check shared preferences to see if we should be blocking
            val sharedPreferences = applicationContext.getSharedPreferences(
                BlockAppService.PREF_NAME,
                Context.MODE_PRIVATE
            )
            
            val isBlocking = sharedPreferences.getBoolean(BlockAppService.KEY_IS_BLOCKING, false)
            val blockEndTime = sharedPreferences.getLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
            val blockedPackages = sharedPreferences.getStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf()) ?: setOf()
            
            // If we should be blocking but the service isn't running, restart it
            if (isBlocking && 
                System.currentTimeMillis() < blockEndTime && 
                blockedPackages.isNotEmpty() && 
                !BlockAppService.isServiceRunning(applicationContext)) {
                
                Log.d(TAG, "BlockAppService should be running but isn't. Restarting...")
                
                // Start the service
                val intent = Intent(applicationContext, BlockAppService::class.java)
                applicationContext.startForegroundService(intent)
                
                Log.d(TAG, "BlockAppService restarted")
            } else if (isBlocking && System.currentTimeMillis() >= blockEndTime) {
                // If blocking period has ended, update shared preferences
                Log.d(TAG, "Blocking period has ended, updating preferences")
                sharedPreferences.edit().apply {
                    putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
                    putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
                    putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
                    apply()
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ServiceMonitorWorker", e)
            Result.retry()
        }
    }
}
