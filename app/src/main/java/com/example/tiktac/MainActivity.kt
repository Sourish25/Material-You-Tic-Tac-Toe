package com.example.tiktac

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktac.TicTacToeTheme

// Fonts handled in Fonts.kt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) } // Default dark
            var fontIndex by remember { mutableStateOf(1) } // Default Pacifico
            
            val currentFont = AppFonts.getFont(fontIndex)

            TicTacToeTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TikTacGame(
                        font = currentFont,
                        fontIndex = fontIndex,
                        isDark = isDarkTheme,
                        onThemeToggle = { isDarkTheme = it },
                        onFontChange = { fontIndex = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikTacGame(
    viewModel: GameViewModel = viewModel(),
    font: FontFamily,
    fontIndex: Int,
    isDark: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onFontChange: (Int) -> Unit
) {
    val state = viewModel.state
    var showSettings by remember { mutableStateOf(false) }
    var showModeInfo by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    LaunchedEffect(state.soundEvent) {
        state.soundEvent?.let { event ->
            if (state.isSoundEnabled) {
                SoundManager.playSound(event)
            }
            
             if (state.isHapticsEnabled && vibrator?.hasVibrator() == true) {
                when (event) {
                    "move" -> vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                    "bomb" -> vibrator.vibrate(VibrationEffect.createOneShot(300, 255))
                    "win" -> {
                         val timings = longArrayOf(0, 100, 100, 100, 100, 300)
                         val amplitudes = intArrayOf(0, 255, 0, 255, 0, 200)
                         if (Build.VERSION.SDK_INT >= 26) {
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                         } else {
                            vibrator.vibrate(500)
                         }
                    }
                    else -> {}
                }
            }
            viewModel.consumeSound()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    // Game Mode Selector Dropdown (Left Aligned)
                    GameModeSelector(
                         currentMode = state.currentMode,
                         onModeSelect = { viewModel.setMode(it) }
                    )
                },
                actions = {
                    // Reset Button Removed (moved to FAB)
                    FilledTonalIconButton(onClick = { showModeInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Rules")
                    }
                    FilledTonalIconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Bottom Start: Reset Button
                Box(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart)) {
                    ResetButton(onReset = { viewModel.resetGame() })
                }
                
                // Bottom End: Donation Button
                Box(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd)) {
                    DonationButton()
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center // Use Center or construct custom overlay
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        state.winner != null -> "${state.winner} Wins!"
                        state.isDraw -> "Draw!"
                        else -> "${state.currentPlayer}'s Turn"
                    },
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = font,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (state.currentMode == GameMode.SPEED) {
                    LinearProgressIndicator(
                        progress = state.timeLeft,
                        modifier = Modifier.fillMaxWidth(0.6f).height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (state.currentMode == GameMode.ULTIMATE) {
                    // Wrap in Box to apply weight/centering like BoardComponent
                    Box(modifier = Modifier.weight(1f, fill = false), contentAlignment = androidx.compose.ui.Alignment.Center) {
                         UltimateBoardComponent(
                             state = state,
                             onCellClick = viewModel::makeMove
                         )
                    }
                } else {
                    BoardComponent(
                        state = state,
                        onCellClick = viewModel::makeMove,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Opponent Selector (Now part of layout to prevent overlap)
                OpponentSelector(
                     currentType = state.opponentType,
                     onSelect = { viewModel.setOpponent(it) },
                     modifier = Modifier.width(300.dp)
                )
                
                // Spacer to clear the floating Reset/Donate buttons (72dp + padding)
                Spacer(modifier = Modifier.height(88.dp))
            }
            
            if (state.showConfetti) {
                ConfettiView()
            }
        }

        if (showSettings) {
            SettingsSheet(
                show = true,
                onDismiss = { showSettings = false },
                currentSize = state.gridSize,
                currentWinCondition = state.winCondition,
                onApply = { size, winCond ->
                    viewModel.resetGame(size, winCond)
                    showSettings = false
                },
                currentFontIndex = fontIndex,
                onFontChange = onFontChange,
                isDarkTheme = isDark,
                onThemeToggle = onThemeToggle,
                // Stats & Settings
                scoreX = state.scoreX,
                scoreO = state.scoreO,
                draws = state.draws,
                onResetStats = { viewModel.resetStats() },
                isSoundEnabled = state.isSoundEnabled,
                onToggleSound = { viewModel.toggleSound(it) },
                isHapticsEnabled = state.isHapticsEnabled,
                onToggleHaptics = { viewModel.toggleHaptics(it) }
            )
        }
        
        if (showModeInfo) {
            AlertDialog(
                onDismissRequest = { showModeInfo = false },
                confirmButton = { TextButton(onClick = { showModeInfo = false }) { Text("Got it") } },
                title = { Text(state.currentMode.title, fontFamily = font) },
                text = { Text(state.currentMode.description, fontFamily = font) },
                icon = { Icon(state.currentMode.icon, null) }
            )
        }
    }
}
