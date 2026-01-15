package com.example.pomodoro

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

class TimerViewModel(private val dao: TimerSessionDao) : ViewModel() {

    private val _currentMode = MutableStateFlow(TimerMode.POMODORO)
    val currentMode: StateFlow<TimerMode> = _currentMode.asStateFlow()

    // Время в секундах
    private val _timeLeft = MutableStateFlow(_currentMode.value.duration * 60)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null

    // Flow для получения истории сессий из БД
    val sessions = dao.getAllSessions()

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
                        duration = _currentMode.value.duration, // Сохраняем длительность в минутах
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
        _timeLeft.value = _currentMode.value.duration * 60
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

// Фабрика для создания ViewModel с зависимостью (DAO)
class TimerViewModelFactory(private val dao: TimerSessionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimerViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
