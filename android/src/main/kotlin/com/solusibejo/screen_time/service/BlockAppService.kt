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
import com.solusibejo.screen_time.worker.ServiceMonitorWorker
import com.solusibejo.screen_time.worker.ServiceRestartWorker
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
        
        // WorkManager tags
        private const val SERVICE_MONITOR_TAG = "block_app_service_monitor"
        private const val SERVICE_RESTART_TAG = "block_app_service_restart"
        
        // Intent actions
        const val ACTION_START_BLOCKING = "com.solusibejo.screen_time.START_BLOCKING"
        const val ACTION_STOP_BLOCKING = "com.solusibejo.screen_time.STOP_BLOCKING"
        
        // Check if service is running
        fun isServiceRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE).any { 
                it.service.className == BlockAppService::class.java.name 
            }
        }
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
        // Log the packages we're blocking and until when
        Log.d(TAG, "Starting blocking apps: ${blockedPackages.joinToString(", ")} until ${blockEndTime}")
        Log.d(TAG, "Current time: ${System.currentTimeMillis()}, remaining: ${blockEndTime - System.currentTimeMillis()} ms")
        
        // Schedule a periodic work to ensure service keeps running
        scheduleServiceMonitor()
        
        serviceScope.launch {
            try {
                while (isActive && System.currentTimeMillis() < blockEndTime) {
                    checkAndBlockApp()
                    delay(CHECK_INTERVAL)
                }
                Log.d(TAG, "Block time ended, stopping service")
                stopBlocking()
            } catch (e: Exception) {
                Log.e(TAG, "Error in blocking loop", e)
                // Don't stop the service on error, try to recover
                Log.d(TAG, "Attempting to recover from error")
                delay(1000) // Wait a bit before retrying
                if (System.currentTimeMillis() < blockEndTime) {
                    startBlockingApps() // Restart the blocking loop
                } else {
                    stopBlocking()
                }
            }
        }
    }

    private suspend fun checkAndBlockApp() = withContext(Dispatchers.Default) {
        if (!Settings.canDrawOverlays(this@BlockAppService) || 
            !hasUsageStatsPermission(this@BlockAppService)) {
            Log.e(TAG, "Missing required permissions, stopping service")
            stopBlocking()
            return@withContext
        }

        val foregroundApp = getForegroundApp()
        Log.d(TAG, "Current foreground app: $foregroundApp")
        
        withContext(Dispatchers.Main) {
            if (foregroundApp != null && 
                blockedPackages.contains(foregroundApp) && 
                !isDeviceLocked(this@BlockAppService)) {
                Log.d(TAG, "Showing overlay for blocked app: $foregroundApp")
                showOverlay()
            } else {
                // Only hide if we're not blocking the current app
                if (foregroundApp != null && !blockedPackages.contains(foregroundApp)) {
                    Log.d(TAG, "Hiding overlay, current app not blocked: $foregroundApp")
                    hideOverlay()
                } else if (isDeviceLocked(this@BlockAppService)) {
                    Log.d(TAG, "Hiding overlay, device is locked")
                    hideOverlay()
                } else if (foregroundApp == null) {
                    // If we can't detect the foreground app, don't change the overlay state
                    // This prevents the overlay from disappearing when app detection fails temporarily
                    Log.d(TAG, "Could not detect foreground app, maintaining current overlay state")
                }
            }
        }
    }

    private suspend fun getForegroundApp(): String? = withContext(Dispatchers.IO) {
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            // Increase the time window to improve detection reliability
            val beginTime = endTime - 1000 * 30 // Last 30 seconds

            // First try to get events
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            var lastForegroundEvent: UsageEvents.Event? = null

            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForegroundEvent = event
                }
            }

            // If we found a foreground event, return its package name
            if (lastForegroundEvent != null) {
                return@withContext lastForegroundEvent.packageName
            }
            
            // Fallback: If no events found, try to get usage stats
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
            if (stats.isNotEmpty()) {
                // Sort by last time used
                val mostRecentApp = stats.maxByOrNull { it.lastTimeUsed }
                if (mostRecentApp != null) {
                    Log.d(TAG, "Using fallback detection method, found: ${mostRecentApp.packageName}")
                    return@withContext mostRecentApp.packageName
                }
            }
            
            // If we couldn't find anything, return null
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            null
        }
    }

    private fun showOverlay() {
        if (!isOverlayDisplayed || overlayView?.windowToken == null) {
            try {
                // If the view was already added but the token is null, remove it first
                if (isOverlayDisplayed) {
                    try {
                        windowManager?.removeView(overlayView)
                    } catch (e: Exception) {
                        // Ignore, view might not be attached
                    }
                }
                
                windowManager?.addView(overlayView, params)
                isOverlayDisplayed = true
                Log.d(TAG, "Overlay displayed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing overlay", e)
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

    /**
     * Loads an overlay view from a specified package or creates a default one programmatically
     * 
     * @param packageName The package name containing the layout resource (e.g., "com.example.app")
     * @param layoutName The name of the layout resource without the extension (e.g., "block_overlay")
     * @return The inflated or created View
     */
    private fun loadOverlayView(packageName: String?, layoutName: String): View {
        try {
            // First try to load from our own package (the plugin)
            try {
                val layoutId = resources.getIdentifier(layoutName, "layout", this.packageName)
                if (layoutId != 0) {
                    Log.d(TAG, "Loading layout from plugin package: ${this.packageName}, layout: $layoutName")
                    return LayoutInflater.from(this).inflate(layoutId, null)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Could not load layout from plugin package: ${e.message}")
            }
            
            // Then try to load from the specified package
            if (packageName != null && packageName != this.packageName) {
                try {
                    // Create a context for the package with more permissive flags
                    val flags = Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE
                    val packageContext = createPackageContext(packageName, flags)
                    val layoutId = packageContext.resources.getIdentifier(layoutName, "layout", packageName)
                    
                    if (layoutId != 0) {
                        Log.d(TAG, "Loading layout from host package: $packageName, layout: $layoutName")
                        
                        // Use the package context's layout inflater to ensure proper resource resolution
                        val inflater = LayoutInflater.from(packageContext)
                        return inflater.inflate(layoutId, null)
                    } else {
                        Log.d(TAG, "Layout resource not found in package: $packageName, layout: $layoutName")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing package context: $packageName", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadOverlayView", e)
        }
        
        // Fallback to creating a view programmatically
        Log.d(TAG, "Using fallback programmatic layout")
        return createFallbackOverlayView()
    }
    
    /**
     * Creates a simple programmatic overlay view as a fallback
     */
    private fun createFallbackOverlayView(): View {
        val frameLayout = android.widget.FrameLayout(this)
        frameLayout.setBackgroundColor(android.graphics.Color.BLACK)
        
        val textView = android.widget.TextView(this)
        textView.text = "This app is currently blocked"
        textView.setTextColor(android.graphics.Color.WHITE)
        textView.textSize = 24f
        
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = android.view.Gravity.CENTER
        frameLayout.addView(textView, params)
        
        // Add a button to close the overlay (for debugging purposes)
        val closeButton = android.widget.Button(this)
        closeButton.text = "Close"
        closeButton.setOnClickListener {
            try {
                if (isOverlayDisplayed && overlayView?.windowToken != null) {
                    windowManager?.removeView(overlayView)
                    isOverlayDisplayed = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error closing overlay", e)
            }
        }
        
        val buttonParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        buttonParams.bottomMargin = 50
        frameLayout.addView(closeButton, buttonParams)
        
        return frameLayout
    }
    
    private fun stopBlocking() {
        try {
            Log.d(TAG, "Stopping blocking service")
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
            
            // Cancel the service monitor work
            cancelServiceMonitor()
            
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping service", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            Log.d(TAG, "onDestroy called")
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
            
            // Check if we're still in blocking period
            val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val isBlocking = sharedPreferences.getBoolean(KEY_IS_BLOCKING, false)
            val blockEndTime = sharedPreferences.getLong(KEY_BLOCK_END_TIME, 0)
            
            if (isBlocking && System.currentTimeMillis() < blockEndTime) {
                Log.d(TAG, "Service destroyed while still in blocking period, scheduling restart")
                // Schedule a restart of the service
                scheduleServiceRestart()
            } else {
                blockedPackages.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }
    
    /**
     * Schedules a periodic work to monitor the service and restart it if it's killed
     */
    private fun scheduleServiceMonitor() {
        try {
            val workManager = androidx.work.WorkManager.getInstance(applicationContext)
            
            // Cancel any existing monitoring work
            workManager.cancelAllWorkByTag(SERVICE_MONITOR_TAG)
            
            // Create a periodic work request to check if service is running
            val monitorRequest = androidx.work.PeriodicWorkRequestBuilder<ServiceMonitorWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .addTag(SERVICE_MONITOR_TAG)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            // Enqueue the work
            workManager.enqueueUniquePeriodicWork(
                SERVICE_MONITOR_TAG,
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                monitorRequest
            )
            
            Log.d(TAG, "Scheduled service monitor work")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service monitor", e)
        }
    }
    
    /**
     * Cancels the service monitor work
     */
    private fun cancelServiceMonitor() {
        try {
            val workManager = androidx.work.WorkManager.getInstance(applicationContext)
            workManager.cancelAllWorkByTag(SERVICE_MONITOR_TAG)
            Log.d(TAG, "Cancelled service monitor work")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling service monitor", e)
        }
    }
    
    /**
     * Schedules a one-time work to restart the service
     */
    private fun scheduleServiceRestart() {
        try {
            val workManager = androidx.work.WorkManager.getInstance(applicationContext)
            
            // Create a one-time work request to restart the service
            val restartRequest = androidx.work.OneTimeWorkRequestBuilder<ServiceRestartWorker>()
                .addTag(SERVICE_RESTART_TAG)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            // Enqueue the work
            workManager.enqueueUniqueWork(
                SERVICE_RESTART_TAG,
                androidx.work.ExistingWorkPolicy.REPLACE,
                restartRequest
            )
            
            Log.d(TAG, "Scheduled service restart work")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling service restart", e)
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
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Load shared preferences first to ensure we have the latest state
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Get packages and duration from intent
        intent?.let { nonNullIntent ->
            val packages = nonNullIntent.getStringArrayListExtra("packages")
            val duration = nonNullIntent.getLongExtra("duration", 0)
            val customLayoutPackage = nonNullIntent.getStringExtra("layoutPackage")
            val customLayoutName = nonNullIntent.getStringExtra("layoutName") ?: "block_overlay"
            
            // Only update if we have new packages
            if (packages != null && packages.isNotEmpty()) {
                Log.d(TAG, "Updating blocked packages from intent: ${packages.joinToString(", ")}")
                blockedPackages.clear()
                blockedPackages.addAll(packages)
                blockEndTime = System.currentTimeMillis() + duration
                
                // Save to preferences
                sharedPreferences.edit().apply {
                    putBoolean(KEY_IS_BLOCKING, true)
                    putStringSet(KEY_BLOCKED_PACKAGES, blockedPackages)
                    putLong(KEY_BLOCK_END_TIME, blockEndTime)
                    apply()
                }
            }
        }
        
        // If no packages were provided in the intent, load from preferences
        if (blockedPackages.isEmpty()) {
            blockEndTime = sharedPreferences.getLong(KEY_BLOCK_END_TIME, 0)
            blockedPackages.addAll(
                sharedPreferences.getStringSet(KEY_BLOCKED_PACKAGES, setOf()) ?: setOf()
            )
            Log.d(TAG, "Loaded blocked packages from preferences: ${blockedPackages.joinToString(", ")}")
        }
        
        // Try to load the layout from the specified package or fall back to a simple programmatic layout
        overlayView = loadOverlayView(intent?.getStringExtra("layoutPackage"), intent?.getStringExtra("layoutName") ?: "block_overlay")

        // Check if blocking period has ended
        if (System.currentTimeMillis() >= blockEndTime || blockedPackages.isEmpty()) {
            Log.d(TAG, "Blocking period has ended or no packages to block")
            stopBlocking()
            return START_NOT_STICKY
        }

        // Create notification channel
        val channel = NotificationChannel(CHANNEL_ID, "BlockAppService Channel", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        // Create a more informative notification
        val timeRemaining = (blockEndTime - System.currentTimeMillis()) / 60000 // in minutes
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocker Active")
            .setContentText("Blocking ${blockedPackages.size} apps for $timeRemaining more minutes")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        startBlockingApps()

        // Return START_REDELIVER_INTENT to ensure the service is restarted with the same intent if killed
        return START_REDELIVER_INTENT
    }
}