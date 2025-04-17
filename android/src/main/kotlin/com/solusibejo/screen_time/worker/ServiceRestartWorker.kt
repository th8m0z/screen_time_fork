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
 * Worker responsible for restarting the BlockAppService after it has been killed
 */
class ServiceRestartWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "ServiceRestartWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to restart BlockAppService")
            
            // Check shared preferences to see if we should be blocking
            val sharedPreferences = applicationContext.getSharedPreferences(
                BlockAppService.PREF_NAME,
                Context.MODE_PRIVATE
            )
            
            val isBlocking = sharedPreferences.getBoolean(BlockAppService.KEY_IS_BLOCKING, false)
            val blockEndTime = sharedPreferences.getLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
            val blockedPackages = sharedPreferences.getStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf()) ?: setOf()
            
            // Only restart if we should still be blocking
            if (isBlocking && 
                System.currentTimeMillis() < blockEndTime && 
                blockedPackages.isNotEmpty()) {
                
                // Start the service
                val intent = Intent(applicationContext, BlockAppService::class.java).apply {
                    action = BlockAppService.ACTION_START_BLOCKING
                }
                applicationContext.startForegroundService(intent)
                
                Log.d(TAG, "BlockAppService restarted successfully")
                Result.success()
            } else {
                // If blocking period has ended, update shared preferences
                Log.d(TAG, "Blocking period has ended, updating preferences")
                sharedPreferences.edit().apply {
                    putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
                    putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
                    putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
                    apply()
                }
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting BlockAppService", e)
            Result.retry()
        }
    }
}
