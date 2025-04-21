package com.solusibejo.screen_time.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.solusibejo.screen_time.R
import com.solusibejo.screen_time.ScreenTimePlugin
import com.solusibejo.screen_time.const.Argument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * A service that shows a persistent notification during the pause period of app blocking.
 * This ensures users are aware that blocking is paused and will resume after a specific duration.
 */
class PauseNotificationService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var pauseDuration: Long = 0
    private var remainingBlockTime: Long = 0
    private var pausedPackagesCount: Int = 0
    private var pauseEndTime: Long = 0
    
    companion object {
        const val CHANNEL_ID = "PauseNotificationService_Channel_ID"
        const val NOTIFICATION_ID = 2
        private const val TAG = "PauseNotificationService"
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PauseNotificationService started")
        
        // Get pause information from intent
        intent?.let { nonNullIntent ->
            pauseEndTime = nonNullIntent.getLongExtra(Argument.pauseEndTime, 0)
            pauseDuration = nonNullIntent.getLongExtra(Argument.pauseDuration, 0)
            remainingBlockTime = nonNullIntent.getLongExtra("remaining_block_time", 0)
            pausedPackagesCount = nonNullIntent.getIntExtra("paused_packages_count", 0)
            
            // Create notification channel
            createNotificationChannel()
            
            // Start as foreground service with notification
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            // Schedule service to stop when pause ends
            scheduleServiceStop()
        }
        
        return START_REDELIVER_INTENT
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pause Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Shows notifications when app blocking is paused"
            channel.setShowBadge(true)
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        val pauseDurationFormatted = Duration.ofMillis(pauseDuration).toString()
            .substring(2)
            .replace("H", "h ")
            .replace("M", "m ")
            .replace("S", "s")
            .lowercase()
        
        val remainingBlockTimeFormatted = Duration.ofMillis(remainingBlockTime).toString()
            .substring(2)
            .replace("H", "h ")
            .replace("M", "m ")
            .replace("S", "s")
            .lowercase()
        
        val title = "App Blocking Paused"
        val text = "Blocking $pausedPackagesCount apps paused for $pauseDurationFormatted. Will resume with $remainingBlockTimeFormatted remaining."
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .build()
    }
    
    private fun scheduleServiceStop() {
        serviceScope.launch {
            try {
                val delayTime = pauseEndTime - System.currentTimeMillis()
                if (delayTime > 0) {
                    delay(delayTime)
                }
                
                // Time's up, stop service
                Log.d(TAG, "Pause period ended, stopping service")
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error in service stop scheduling", e)
                stopSelf()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PauseNotificationService destroyed")
        serviceJob.cancel()
    }
}
