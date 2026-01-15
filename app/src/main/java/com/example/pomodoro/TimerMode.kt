package com.example.pomodoro


// Режимы таймера. Длительности теперь хранятся в настройках, а не в enum.
enum class TimerMode(val displayName: String) {
    POMODORO("Pomodoro"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}


