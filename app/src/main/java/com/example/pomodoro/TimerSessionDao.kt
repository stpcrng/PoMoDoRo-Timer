package com.example.pomodoro

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionDao {
    @Insert
    suspend fun insertSession(session: TimerSession)

    @Query("SELECT * FROM timer_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TimerSession>>
}
