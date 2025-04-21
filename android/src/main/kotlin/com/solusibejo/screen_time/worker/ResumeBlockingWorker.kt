package com.solusibejo.screen_time.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.solusibejo.screen_time.ScreenTimePlugin
import com.solusibejo.screen_time.const.Argument
import com.solusibejo.screen_time.service.BlockAppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * Worker class to resume app blocking after a pause period has expired.
 * This worker is scheduled by the pauseBlockApps method and will restart
 * the blocking service with the remaining duration from before the pause.
 */
class ResumeBlockingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ResumeBlockingWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Resuming app blocking after pause")
            
            // Get shared preferences
            val sharedPreferences = applicationContext.getSharedPreferences(
                ScreenTimePlugin.PREF_NAME, 
                Context.MODE_PRIVATE
            )
            
            // Check if we're still in paused state
            val isPaused = sharedPreferences.getBoolean("is_paused", false)
            if (!isPaused) {
                Log.d(TAG, "Not in paused state, nothing to resume")
                return@withContext Result.success()
            }
            
            // Get the paused data
            val pausedPackages = sharedPreferences.getStringSet("paused_blocked_packages", setOf()) ?: setOf()
            val remainingTime = sharedPreferences.getLong("paused_remaining_time", 0)
            
            if (pausedPackages.isEmpty() || remainingTime <= 0) {
                Log.e(TAG, "Invalid paused data, cannot resume blocking")
                // Clear pause state
                sharedPreferences.edit().apply {
                    remove("is_paused")
                    remove("paused_blocked_packages")
                    remove("paused_remaining_time")
                    remove("pause_end_time")
                    apply()
                }
                return@withContext Result.failure()
            }
            
            // Make sure any existing blocking service is stopped before starting a new one
            try {
                val stopIntent = Intent(applicationContext, BlockAppService::class.java)
                applicationContext.stopService(stopIntent)
                // Small delay to ensure the service has time to stop
                delay(200)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping existing service", e)
                // Continue anyway
            }
            
            // Get notification customization from input data
            val notificationTitle = inputData.getString("notification_title")
            val notificationText = inputData.getString("notification_text")
            
            // Start BlockAppService with the remaining duration
            val intent = Intent(applicationContext, BlockAppService::class.java).apply {
                putStringArrayListExtra(Argument.packagesName, ArrayList(pausedPackages))
                putExtra(Argument.duration, remainingTime)
                
                // Pass the package name to load the layout from
                putExtra(Argument.layoutPackage, applicationContext.packageName)
                putExtra(Argument.layoutName, BlockAppService.DEFAULT_LAYOUT_NAME)
                
                // Pass notification customization parameters
                putExtra(Argument.notificationTitle, notificationTitle)
                putExtra(Argument.notificationText, notificationText)
            }
            
            try {
                // Update the blocking state in SharedPreferences before starting the service
                // This ensures the UI will update correctly
                sharedPreferences.edit().apply {
                    putBoolean(BlockAppService.KEY_IS_BLOCKING, true)
                    putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, pausedPackages)
                    putLong(BlockAppService.KEY_BLOCK_END_TIME, System.currentTimeMillis() + remainingTime)
                    apply()
                }
                
                // Start the service
                applicationContext.startForegroundService(intent)
                Log.d(TAG, "Successfully resumed blocking for ${pausedPackages.size} apps with remaining time ${Duration.ofMillis(remainingTime).toMinutes()} minutes")
                
                // Clear pause state
                sharedPreferences.edit().apply {
                    remove("is_paused")
                    remove("paused_blocked_packages")
                    remove("paused_remaining_time")
                    remove("pause_end_time")
                    apply()
                }
                
                return@withContext Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume blocking", e)
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in ResumeBlockingWorker", e)
            return@withContext Result.failure()
        }
    }
}
