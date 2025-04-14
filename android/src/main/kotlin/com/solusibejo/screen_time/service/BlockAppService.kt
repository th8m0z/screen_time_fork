package com.solusibejo.screen_time.service

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.solusibejo.screen_time.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockAppService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isOverlayDisplayed = false
    private val blockedPackages = mutableSetOf<String>()
    private var blockEndTime: Long = 0
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    companion object {
        const val CHANNEL_ID = "BlockAppService_Channel_ID"
        const val NOTIFICATION_ID = 1
        private const val CHECK_INTERVAL = 1000L // 1 second
        const val PREF_NAME = "screen_time"
        const val KEY_BLOCK_END_TIME = "block_end_time"
        const val KEY_BLOCKED_PACKAGES = "blocked_packages"
        const val KEY_IS_BLOCKING = "isBlocking"
        private const val TAG = "BlockAppService"
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )


    private fun isDeviceLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        return keyguardManager.isKeyguardLocked
    }


    private fun startBlockingApps() {
        serviceScope.launch {
            try {
                while (isActive && System.currentTimeMillis() < blockEndTime) {
                    checkAndBlockApp()
                    delay(CHECK_INTERVAL)
                }
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Error in blocking loop", e)
                stopSelf()
            }
        }
    }

    private suspend fun checkAndBlockApp() = withContext(Dispatchers.Default) {
        if (!Settings.canDrawOverlays(this@BlockAppService) || 
            !hasUsageStatsPermission(this@BlockAppService)) {
            stopBlocking()
            return@withContext
        }

        val foregroundApp = getForegroundApp()
        withContext(Dispatchers.Main) {
            if (foregroundApp != null && 
                blockedPackages.contains(foregroundApp) && 
                !isDeviceLocked(this@BlockAppService)) {
                showOverlay()
            } else {
                hideOverlay()
            }
        }
    }

    private suspend fun getForegroundApp(): String? = withContext(Dispatchers.IO) {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000 * 10 // Last 10 seconds

            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            var lastForegroundEvent: UsageEvents.Event? = null

            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForegroundEvent = event
                }
            }

            lastForegroundEvent?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            null
        }
    }

    private fun showOverlay() {
        if (!isOverlayDisplayed && overlayView?.windowToken == null) {
            try {
                windowManager?.addView(overlayView, params)
                isOverlayDisplayed = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideOverlay() {
        if (isOverlayDisplayed && overlayView?.windowToken != null) {
            try {
                windowManager?.removeView(overlayView)
                isOverlayDisplayed = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopBlocking() {
        try {
            // Remove overlay if it exists
            if (overlayView != null && windowManager != null && isOverlayDisplayed) {
                windowManager?.removeView(overlayView)
                isOverlayDisplayed = false
            }
            
            // Clear blocked packages
            blockedPackages.clear()
            
            // Cancel any ongoing coroutines
            serviceJob.cancel()
            
            // Clear shared preferences
            val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putBoolean(KEY_IS_BLOCKING, false)
                putStringSet(KEY_BLOCKED_PACKAGES, setOf())
                putLong(KEY_BLOCK_END_TIME, 0)
                apply()
            }
            
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            // Remove overlay if it exists
            if (overlayView != null && windowManager != null && isOverlayDisplayed) {
                windowManager?.removeView(overlayView)
                isOverlayDisplayed = false
            }
            
            // Cancel any ongoing coroutines
            serviceJob.cancel()
            
            // Clear resources
            windowManager = null
            overlayView = null
            blockedPackages.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get packages and duration from intent
        intent?.let { nonNullIntent ->
            val packages = nonNullIntent.getStringArrayListExtra("packages")
            val duration = nonNullIntent.getLongExtra("duration", 0)
            
            packages?.let { packageList ->
                blockedPackages.addAll(packageList)
                blockEndTime = System.currentTimeMillis() + duration
            }
        }
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.block_overlay, null)

        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        blockEndTime = sharedPreferences.getLong(KEY_BLOCK_END_TIME, 0)
        blockedPackages.clear()
        blockedPackages.addAll(
            sharedPreferences.getStringSet(KEY_BLOCKED_PACKAGES, setOf()) ?: setOf()
        )

        if (System.currentTimeMillis() >= blockEndTime) {
            stopBlocking()
            return START_NOT_STICKY
        }

        val channel = NotificationChannel(CHANNEL_ID, "BlockAppService Channel", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocker Active")
            .setContentText("Blocking ${blockedPackages.size} apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        startBlockingApps()

        return START_STICKY
    }
}