package com.solusibejo.screen_time.manager

import android.content.Context
import android.util.Log
import androidx.work.*
import com.solusibejo.screen_time.model.BlockSchedule
import com.solusibejo.screen_time.worker.BlockAppWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration
import java.time.Instant

class BlockScheduleManager(private val context: Context) {
    companion object {
        private const val TAG = "BlockScheduleManager"
        const val WORK_NAME_PREFIX = "block_schedule_"
    }

    private val activeSchedules = mutableListOf<BlockSchedule>()

    /**
     * Apply a new block schedule from API
     * This should be called when receiving new schedules from your backend
     */
    fun applySchedule(schedule: BlockSchedule) {
        try {
            // Cancel any existing work for this schedule
            WorkManager.getInstance(context)
                .cancelUniqueWork("${WORK_NAME_PREFIX}${schedule.id}")

            if (!schedule.isEnabled) return

            val now = Instant.now()
            if (schedule.startTime.isBefore(now) && !schedule.isRecurring) return

            // Create work request
            val workRequest = BlockAppWorker.createWorkRequest(
                packages = schedule.packages,
                duration = schedule.duration
            )

            // Calculate initial delay
            val initialDelay = Duration.between(now, schedule.startTime)
            
            val workRequestBuilder = OneTimeWorkRequestBuilder<BlockAppWorker>()
                .setInputData(workDataOf(
                    BlockAppWorker.KEY_PACKAGES to schedule.packages.toTypedArray(),
                    BlockAppWorker.KEY_DURATION to schedule.duration.toMillis()
                ))
                .setInitialDelay(initialDelay)

            if (schedule.isRecurring) {
                // Add periodic work constraints based on daysOfWeek
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                workRequestBuilder.setConstraints(constraints)
            }

            // Enqueue the work
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "${WORK_NAME_PREFIX}${schedule.id}",
                    ExistingWorkPolicy.REPLACE,
                    workRequestBuilder.build()
                )

            // Update active schedules
            synchronized(activeSchedules) {
                val existingIndex = activeSchedules.indexOfFirst { it.id == schedule.id }
                if (existingIndex >= 0) {
                    activeSchedules[existingIndex] = schedule
                } else {
                    activeSchedules.add(schedule)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in BlockAppWorker", e)
            throw e
        }
    }

    /**
     * Cancel a specific schedule
     */
    fun cancelSchedule(scheduleId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("${WORK_NAME_PREFIX}$scheduleId")

        synchronized(activeSchedules) {
            activeSchedules.removeAll { it.id == scheduleId }
        }
    }

    /**
     * Cancel all schedules
     */
    fun cancelAllSchedules() {
        WorkManager.getInstance(context).cancelAllWork()
        synchronized(activeSchedules) {
            activeSchedules.clear()
        }
    }

    /**
     * Get all active schedules
     */
    fun getActiveSchedules(): List<BlockSchedule> {
        synchronized(activeSchedules) {
            return activeSchedules.toList()
        }
    }
}
