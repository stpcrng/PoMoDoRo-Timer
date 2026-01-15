package com.example.pomodoro

enum class TimerMode(val duration: Long, val displayName: String) {
    POMODORO(25, "Pomodoro"),
    SHORT_BREAK(5, "Short Break"),
    LONG_BREAK(15, "Long Break")
}
