package com.example.pomodoro

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "timer_sessions")
data class TimerSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Date,
    val endTime: Date,
    val duration: Long, // Duration in minutes
    val mode: TimerMode
)
