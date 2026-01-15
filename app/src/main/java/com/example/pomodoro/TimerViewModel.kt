package com.example.pomodoro

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class TimerViewModel(private val dao: TimerSessionDao, private val ctx: Context) : ViewModel() {

    private val _currentMode = MutableStateFlow(TimerMode.POMODORO)
    val currentMode: StateFlow<TimerMode> = _currentMode.asStateFlow()

    // Время в секундах
    private val _timeLeft = MutableStateFlow(getDurationForMode(_currentMode.value) * 60)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null

    // Flow для получения истории сессий из БД
    val sessions = dao.getAllSessions()

    private fun getDurationForMode(mode: TimerMode): Long {
        val prefs = ctx.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        return when (mode) {
            TimerMode.POMODORO -> prefs.getLong("pref_pomodoro_minutes", 25L)
            TimerMode.SHORT_BREAK -> prefs.getLong("pref_short_break_minutes", 5L)
            TimerMode.LONG_BREAK -> prefs.getLong("pref_long_break_minutes", 15L)
        }
    }

    fun startStopTimer() {
        if (_isRunning.value) {
            timerJob?.cancel()
            _isRunning.value = false
        } else {
            if (_timeLeft.value <= 0) return // Не запускать, если время вышло

            val sessionStartTime = Date()

            timerJob = viewModelScope.launch {
                while (_timeLeft.value > 0) {
                    delay(1000)
                    _timeLeft.value--
                }

                // Когда таймер дошел до нуля, сохраняем сессию
                if (_timeLeft.value <= 0) {
                    val sessionEndTime = Date()
                    val session = TimerSession(
                        startTime = sessionStartTime,
                        endTime = sessionEndTime,
                        duration = getDurationForMode(_currentMode.value), // Сохраняем длительность в минутах
                        mode = _currentMode.value
                    )
                    saveSession(session)
                    _isRunning.value = false
                    // Можно добавить авто-переключение на следующий режим
                }
            }
            _isRunning.value = true
        }
    }

    fun changeMode(newMode: TimerMode) {
        _currentMode.value = newMode
        resetTimer()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timeLeft.value = getDurationForMode(_currentMode.value) * 60
        _isRunning.value = false
    }

    private fun saveSession(session: TimerSession) {
        viewModelScope.launch {
            dao.insertSession(session)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

// Фабрика для создания ViewModel с зависимостью (DAO + Context)
class TimerViewModelFactory(private val dao: TimerSessionDao, private val ctx: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(dao, ctx) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
