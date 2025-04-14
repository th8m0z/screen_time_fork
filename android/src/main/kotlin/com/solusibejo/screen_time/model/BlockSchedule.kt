package com.solusibejo.screen_time.model

import java.time.Duration
import java.time.Instant

data class BlockSchedule(
    val id: String,
    val packages: List<String>,
    val startTime: Instant,
    val duration: Duration,
    val isRecurring: Boolean = false,
    val daysOfWeek: List<Int> = emptyList(),
    val isEnabled: Boolean = true
)
