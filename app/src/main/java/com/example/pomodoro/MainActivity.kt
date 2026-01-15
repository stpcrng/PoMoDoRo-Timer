package com.example.pomodoro

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    private val viewModelFactory by lazy { TimerViewModelFactory(timerSessionDao) }

    private val timerViewModel: TimerViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PoMoDoRoTheme {
                AppNavigation(viewModel = timerViewModel)
            }
        }
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
fun AppNavigation(viewModel: TimerViewModel) {
    val navController = rememberNavController()
    AnimatedBackground {
        NavHost(navController = navController, startDestination = "timer") {
            composable("timer") {
                TimerScreen(viewModel = viewModel, navController = navController)
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel, navController = navController)
            }
        }
    }
}

@Composable
fun TimerScreen(viewModel: TimerViewModel, navController: NavController) {
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()

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
            onHistoryClick = { navController.navigate("history") }
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
fun TimerDisplay(timeLeft: Long) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    Text(
        text = "%02d:%02d".format(minutes, seconds),
        fontSize = 96.sp,
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun Controls(isRunning: Boolean, onStartStopClick: () -> Unit, onResetClick: () -> Unit, onHistoryClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onStartStopClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                modifier = Modifier.width(120.dp)
            ) {
                Text(if (isRunning) "Stop" else "Start", fontSize = 18.sp, color = Color.White)
            }
            IconButton(
                onClick = onResetClick,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Text("⟳", color = Color.White, fontSize = 20.sp)
            }
        }
        Button(
            onClick = onHistoryClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Text("История сеансов", color = Color.White)
        }
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
