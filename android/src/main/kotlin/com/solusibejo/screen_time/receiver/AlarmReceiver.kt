package com.solusibejo.screen_time.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.solusibejo.screen_time.ScreenTimePlugin
import com.solusibejo.screen_time.service.BlockAppService
import com.solusibejo.screen_time.worker.ServiceRestartWorker
import com.solusibejo.screen_time.worker.UnblockWorker
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_STOP_BLOCKING = "${ScreenTimePlugin.PACKAGE_NAME}.STOP_BLOCKING"
        private const val UNBLOCK_REQUEST_CODE = 1001
        private const val BACKUP_UNBLOCK_REQUEST_CODE = 1002

        /**
         * Schedules the unblocking of apps after the specified duration
         * Uses multiple mechanisms for reliability
         */
        fun scheduleUnblock(context: Context, duration: Long) {
            Log.d(TAG, "Scheduling unblock after $duration ms")
            val triggerTime = System.currentTimeMillis() + duration
            
            // Save block end time in shared preferences
            val sharedPreferences = context.getSharedPreferences(
                ScreenTimePlugin.PREF_NAME,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit().apply {
                putLong(BlockAppService.KEY_BLOCK_END_TIME, triggerTime)
                apply()
            }
            
            // Method 1: Use AlarmManager with exact timing
            scheduleAlarmUnblock(context, triggerTime)
            
            // Method 2: Schedule a backup alarm 1 minute later in case the first one fails
            scheduleBackupAlarmUnblock(context, triggerTime + 60000)
            
            // Method 3: Use WorkManager as another backup mechanism
            scheduleWorkManagerUnblock(context, duration)
        }

        /**
         * Schedules the primary alarm for unblocking
         */
        private fun scheduleAlarmUnblock(context: Context, triggerTime: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                UNBLOCK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for unblocking at $triggerTime")
                } else {
                    // Fall back to inexact alarm if we can't schedule exact alarms
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm for unblocking at $triggerTime")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm for unblocking at $triggerTime")
            }
        }

        /**
         * Schedules a backup alarm for unblocking
         */
        private fun scheduleBackupAlarmUnblock(context: Context, triggerTime: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                BACKUP_UNBLOCK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled backup exact alarm for unblocking at $triggerTime")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled backup inexact alarm for unblocking at $triggerTime")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled backup exact alarm for unblocking at $triggerTime")
            }
        }

        /**
         * Uses WorkManager as another backup mechanism for unblocking
         */
        private fun scheduleWorkManagerUnblock(context: Context, delayMillis: Long) {
            try {
                val workManager = WorkManager.getInstance(context)
                
                // Convert to minutes and add a small buffer
                val delayMinutes = (delayMillis / 60000) + 1
                
                val workRequest = OneTimeWorkRequestBuilder<UnblockWorker>()
                    .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    .build()
                
                workManager.enqueue(workRequest)
                Log.d(TAG, "Scheduled WorkManager unblock after $delayMinutes minutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling WorkManager unblock", e)
            }
        }

        /**
         * Cancels all scheduled unblock alarms and work
         */
        fun cancelUnblock(context: Context) {
            Log.d(TAG, "Cancelling all unblock schedules")
            
            // Cancel alarms
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Cancel primary alarm
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                UNBLOCK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            
            // Cancel backup alarm
            val backupIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            val backupPendingIntent = PendingIntent.getBroadcast(
                context,
                BACKUP_UNBLOCK_REQUEST_CODE,
                backupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(backupPendingIntent)
            
            // Cancel WorkManager tasks
            try {
                WorkManager.getInstance(context).cancelAllWorkByTag("unblock_work")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling WorkManager tasks", e)
            }
            
            // Clear shared preferences
            val sharedPreferences = context.getSharedPreferences(
                ScreenTimePlugin.PREF_NAME,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit().apply {
                putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
                putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
                putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
                apply()
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        
        Log.d(TAG, "Received intent: ${intent?.action}")
        
        if (intent?.action == ACTION_STOP_BLOCKING) {
            Log.d(TAG, "Processing stop blocking action")
            
            // Update shared preferences
            val sharedPreferences = context.getSharedPreferences(
                ScreenTimePlugin.PREF_NAME,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit().apply {
                putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
                putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
                putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
                apply()
            }

            // Stop the blocking service if it's running
            if (BlockAppService.isServiceRunning(context)) {
                Log.d(TAG, "Stopping BlockAppService")
                context.stopService(Intent(context, BlockAppService::class.java))
            } else {
                Log.d(TAG, "BlockAppService not running, no need to stop")
            }
            
            // Cancel any remaining alarms or work
            cancelUnblock(context)
        }
    }
}