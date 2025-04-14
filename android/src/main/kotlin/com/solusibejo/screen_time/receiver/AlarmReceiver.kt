package com.solusibejo.screen_time.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.solusibejo.screen_time.service.BlockAppService

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP_BLOCKING = "com.solusibejo.screen_time.STOP_BLOCKING"

        fun scheduleUnblock(context: Context, duration: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + duration
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

            // Save block end time
            val sharedPreferences = context.getSharedPreferences(
                BlockAppService.PREF_NAME,
                Context.MODE_PRIVATE
            )
            sharedPreferences.edit().apply {
                putLong(BlockAppService.KEY_BLOCK_END_TIME, triggerTime)
                apply()
            }
        }

        fun cancelUnblock(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_STOP_BLOCKING
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != ACTION_STOP_BLOCKING) return

        val sharedPreferences = context.getSharedPreferences(
            BlockAppService.PREF_NAME,
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().apply {
            putBoolean(BlockAppService.KEY_IS_BLOCKING, false)
            putStringSet(BlockAppService.KEY_BLOCKED_PACKAGES, setOf())
            putLong(BlockAppService.KEY_BLOCK_END_TIME, 0)
            apply()
        }

        // Stop the blocking service
        context.stopService(Intent(context, BlockAppService::class.java))
    }
}