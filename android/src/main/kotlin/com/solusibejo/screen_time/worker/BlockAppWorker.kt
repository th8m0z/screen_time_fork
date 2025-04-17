package com.solusibejo.screen_time.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.solusibejo.screen_time.ScreenTimePlugin
import com.solusibejo.screen_time.service.BlockAppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class BlockAppWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        private const val TAG = "BlockAppWorker"
        const val KEY_PACKAGES = "packages"
        const val KEY_DURATION = "duration"
        
        fun createWorkRequest(
            packages: List<String>,
            duration: Duration
        ): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<BlockAppWorker>()
                .setInputData(workDataOf(
                    KEY_PACKAGES to packages.toTypedArray(),
                    KEY_DURATION to duration.toMillis()
                ))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val packages = inputData.getStringArray(KEY_PACKAGES)?.toList() ?: emptyList()
            val duration = inputData.getLong(KEY_DURATION, 0)
            
            if (packages.isEmpty() || duration <= 0) {
                Log.e(TAG, "Invalid input data: packages=$packages, duration=$duration")
                return@withContext Result.failure()
            }

            // Save block state
            applicationContext.getSharedPreferences(
                ScreenTimePlugin.PREF_NAME,
                Context.MODE_PRIVATE
            ).edit().apply {
                putBoolean(BlockAppService.KEY_IS_BLOCKING, true)
                putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, packages.toSet())
                putLong(BlockAppService.KEY_BLOCK_END_TIME, System.currentTimeMillis() + duration)
                apply()
            }

            // Start blocking service
            val intent = Intent(applicationContext, BlockAppService::class.java)
            applicationContext.startForegroundService(intent)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in BlockAppWorker", e)
            Result.failure()
        }
    }
}
