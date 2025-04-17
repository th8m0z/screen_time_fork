package com.solusibejo.screen_time.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solusibejo.screen_time.ScreenTimePlugin
import com.solusibejo.screen_time.service.BlockAppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker responsible for unblocking apps after the block duration has expired
 * This serves as a backup mechanism to the AlarmManager
 */
class UnblockWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "UnblockWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "UnblockWorker executing")
            
            // Check if we should still be unblocking
            val sharedPreferences = applicationContext.getSharedPreferences(
                ScreenTimePlugin.PREF_NAME,
                Context.MODE_PRIVATE
            )
            
            val isBlocking = sharedPreferences.getBoolean(BlockAppService.KEY_IS_BLOCKING, false)
            val blockEndTime = sharedPreferences.getLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
            
            // If we're still blocking and the end time has passed, unblock
            if (isBlocking && System.currentTimeMillis() >= blockEndTime) {
                Log.d(TAG, "Block time has ended, unblocking apps")
                
                // Update shared preferences
                sharedPreferences.edit().apply {
                    putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
                    putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
                    putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
                    apply()
                }
                
                // Stop the service if it's running
                if (BlockAppService.isServiceRunning(applicationContext)) {
                    Log.d(TAG, "Stopping BlockAppService")
                    applicationContext.stopService(Intent(applicationContext, BlockAppService::class.java))
                }
            } else if (!isBlocking) {
                Log.d(TAG, "Already unblocked, no action needed")
            } else {
                Log.d(TAG, "Block time not yet ended, no action needed")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in UnblockWorker", e)
            Result.failure()
        }
    }
}
