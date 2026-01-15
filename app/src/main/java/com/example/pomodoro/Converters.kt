package com.example.pomodoro

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromTimerMode(value: String?): TimerMode? {
        return value?.let { TimerMode.valueOf(it) }
    }

    @TypeConverter
    fun timerModeToString(timerMode: TimerMode?): String? {
        return timerMode?.name
    }
}
