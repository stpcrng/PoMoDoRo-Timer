package com.example.pomodoro

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pomodoro.ui.theme.PoMoDoRoTheme
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val timerSessionDao by lazy { database.timerSessionDao() }
    private val viewModelFactory by lazy { TimerViewModelFactory(timerSessionDao, applicationContext) }

    private val timerViewModel: TimerViewModel by viewModels { viewModelFactory }

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PoMoDoRoTheme {
                AppNavigation(viewModel = timerViewModel, onStartMusic = { startAmbient() }, onStopMusic = { stopAmbient() }, onPlayFinishTone = { playFinishTone() })
            }
        }
    }

    private fun startAmbient() {
        val prefs = getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        val ambientEnabled = prefs.getBoolean("pref_ambient_enabled", false)
        if (!ambientEnabled) return

        // Сначала попробуем получить выбранный ресурс из настроек
        val ambientResName = prefs.getString("pref_ambient_track", null)
        stopAmbient()
        try {
            if (!ambientResName.isNullOrEmpty()) {
                // Получаем идентификатор ресурса по имени
                val resId = resources.getIdentifier(ambientResName, "raw", packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(this, resId)
                    mediaPlayer?.isLooping = true
                    mediaPlayer?.start()
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Фоллбек — системный звук уведомления
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, uri)
            isLooping = true
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAmbient() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private fun playFinishTone() {
        val prefs = getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        val toneEnabled = prefs.getBoolean("pref_finish_tone_enabled", true)
        if (!toneEnabled) return

        val finishResName = prefs.getString("pref_finish_tone", null)
        try {
            if (!finishResName.isNullOrEmpty()) {
                val resId = resources.getIdentifier(finishResName, "raw", packageName)
                if (resId != 0) {
                    val player = MediaPlayer.create(this, resId)
                    player.start()
                    player.setOnCompletionListener { it.release() }
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val r = RingtoneManager.getRingtone(applicationContext, notification)
        r.play()
    }
}

@Composable
fun AnimatedBackground(content: @Composable BoxScope.() -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite bg")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF4A148C),
        targetValue = Color(0xFF6A1B9A),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bg1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFFAD1457),
        targetValue = Color(0xFFEC407A),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bg2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(color1, color2)))
    ) {
        content()
        Text(
            text = "Made by Kulik Y.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun AppNavigation(viewModel: TimerViewModel, onStartMusic: () -> Unit, onStopMusic: () -> Unit, onPlayFinishTone: () -> Unit) {
    val navController = rememberNavController()
    AnimatedBackground {
        NavHost(navController = navController, startDestination = "timer") {
            composable("timer") {
                TimerScreen(viewModel = viewModel, navController = navController, onStartMusic = onStartMusic, onStopMusic = onStopMusic, onPlayFinishTone = onPlayFinishTone)
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel, navController = navController)
            }
            composable("settings") {
                SettingsScreen(navController = navController)
            }
        }
    }
}

@Composable
fun TimerScreen(viewModel: TimerViewModel, navController: NavController, onStartMusic: () -> Unit, onStopMusic: () -> Unit, onPlayFinishTone: () -> Unit) {
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()

    // Наблюдаем за состоянием isRunning и запускаем/останавливаем ambient
    LaunchedEffect(isRunning) {
        if (isRunning) {
            onStartMusic()
        } else {
            onStopMusic()
        }
    }

    // Наблюдаем за таймером и при достижении нуля воспроизводим тон
    LaunchedEffect(timeLeft) {
        if (timeLeft <= 0L) {
            onPlayFinishTone()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ModeSelector(currentMode = currentMode, onModeChange = { viewModel.changeMode(it) })
        Spacer(modifier = Modifier.height(60.dp))
        TimerDisplay(timeLeft = timeLeft)
        Spacer(modifier = Modifier.height(60.dp))
        Controls(
            isRunning = isRunning,
            onStartStopClick = { viewModel.startStopTimer() },
            onResetClick = { viewModel.resetTimer() },
            onHistoryClick = { navController.navigate("history") },
            onSettingsClick = { navController.navigate("settings") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: TimerViewModel, navController: NavController) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История сеансов", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        SessionHistoryList(
            sessions = sessions,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
fun TimerDisplay(timeLeft: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Таймер",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    fontSize = 84.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun Controls(isRunning: Boolean, onStartStopClick: () -> Unit, onResetClick: () -> Unit, onHistoryClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ElevatedButton(
                onClick = onStartStopClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Pause" else "Start", color = Color.White)
            }

            OutlinedButton(
                onClick = onResetClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .width(72.dp)
                    .height(56.dp)
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = "Reset", tint = Color.White)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onHistoryClick,
                shape = RoundedCornerShape(16),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = "History", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("История", color = Color.White)
            }
            Button(
                onClick = onSettingsClick,
                shape = RoundedCornerShape(16),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Настройки", color = Color.White)
            }
        }
    }
}

// Обновлённый SettingsScreen: оборачиваем секции в карточки, используем более аккуратные элементы
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)

    var pomodoroMinutes by remember { mutableStateOf(prefs.getLong("pref_pomodoro_minutes", 25L).toInt()) }
    var shortBreakMinutes by remember { mutableStateOf(prefs.getLong("pref_short_break_minutes", 5L).toInt()) }
    var longBreakMinutes by remember { mutableStateOf(prefs.getLong("pref_long_break_minutes", 15L).toInt()) }
    var ambientEnabled by remember { mutableStateOf(prefs.getBoolean("pref_ambient_enabled", false)) }
    var finishToneEnabled by remember { mutableStateOf(prefs.getBoolean("pref_finish_tone_enabled", true)) }

    val availableTracks = remember {
        val list = mutableListOf<String>()
        for (i in 1..50) {
            val name = String.format("track_%02d", i)
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) list.add(name)
        }
        list
    }

    var selectedAmbient by remember { mutableStateOf(prefs.getString("pref_ambient_track", if (availableTracks.isNotEmpty()) availableTracks.first() else null)) }
    var selectedFinish by remember { mutableStateOf(prefs.getString("pref_finish_tone", if (availableTracks.isNotEmpty()) availableTracks.first() else null)) }

    var ambientMenuExpanded by remember { mutableStateOf(false) }
    var finishMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Временные настройки", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = pomodoroMinutes.toString(), onValueChange = { v -> pomodoroMinutes = v.filter { it.isDigit() }.toIntOrNull() ?: 0 }, label = { Text("Pomodoro (мин)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = shortBreakMinutes.toString(), onValueChange = { v -> shortBreakMinutes = v.filter { it.isDigit() }.toIntOrNull() ?: 0 }, label = { Text("Short Break (мин)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = longBreakMinutes.toString(), onValueChange = { v -> longBreakMinutes = v.filter { it.isDigit() }.toIntOrNull() ?: 0 }, label = { Text("Long Break (мин)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.20f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Ambient music", color = Color.White)
                        Switch(checked = ambientEnabled, onCheckedChange = { ambientEnabled = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Выберите ambient трек", color = Color.White)
                    Box {
                        Button(onClick = { ambientMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.04f))) {
                            Text(selectedAmbient ?: "(не выбран)")
                        }
                        DropdownMenu(expanded = ambientMenuExpanded, onDismissRequest = { ambientMenuExpanded = false }) {
                            if (availableTracks.isEmpty()) {
                                DropdownMenuItem(text = { Text("Нет доступных треков") }, onClick = { ambientMenuExpanded = false })
                            } else {
                                availableTracks.forEach { name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = {
                                        selectedAmbient = name
                                        ambientMenuExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Finish tone", color = Color.White)
                        Switch(checked = finishToneEnabled, onCheckedChange = { finishToneEnabled = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Выберите мелодию окончания", color = Color.White)
                    Box {
                        Button(onClick = { finishMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.04f))) {
                            Text(selectedFinish ?: "(не выбран)")
                        }
                        DropdownMenu(expanded = finishMenuExpanded, onDismissRequest = { finishMenuExpanded = false }) {
                            if (availableTracks.isEmpty()) {
                                DropdownMenuItem(text = { Text("Нет доступных треков") }, onClick = { finishMenuExpanded = false })
                            } else {
                                availableTracks.forEach { name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = {
                                        selectedFinish = name
                                        finishMenuExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        prefs.edit()
                            .putLong("pref_pomodoro_minutes", pomodoroMinutes.toLong())
                            .putLong("pref_short_break_minutes", shortBreakMinutes.toLong())
                            .putLong("pref_long_break_minutes", longBreakMinutes.toLong())
                            .putBoolean("pref_ambient_enabled", ambientEnabled)
                            .putBoolean("pref_finish_tone_enabled", finishToneEnabled)
                            .putString("pref_ambient_track", selectedAmbient)
                            .putString("pref_finish_tone", selectedFinish)
                            .apply()
                        navController.popBackStack()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelector(currentMode: TimerMode, onModeChange: (TimerMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.2f)),
        horizontalArrangement = Arrangement.Center
    ) {
        TimerMode.values().forEach { mode ->
            ModeButton(
                mode = mode,
                isSelected = currentMode == mode,
                onClick = { onModeChange(mode) }
            )
        }
    }
}

@Composable
fun ModeButton(mode: TimerMode, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Black else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
        )
    ) {
        Text(mode.displayName)
    }
}

@Composable
fun SessionHistoryList(sessions: List<TimerSession>, modifier: Modifier = Modifier) {
    if (sessions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пока нет ни одного сеанса", color = Color.White.copy(alpha = 0.7f))
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp)
        ) {
            items(sessions) { session ->
                SessionHistoryItem(session = session)
            }
        }
    }
}

@Composable
fun SessionHistoryItem(session: TimerSession) {
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Режим: ${session.mode.displayName}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Начало: ${dateFormat.format(session.startTime)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Text(
                text = "${session.duration} мин",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
