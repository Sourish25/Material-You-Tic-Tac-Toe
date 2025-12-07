package com.example.tiktac

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.BlurOff
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.graphics.vector.ImageVector

enum class GameMode(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    CLASSIC("Classic", "Standard Tic Tac Toe rules.", Icons.Default.Grid3x3),
    MISERE("Misere", "Don't win! If you get N in a row, you LOSE.", Icons.Default.Dangerous),
    FADING("Fading", "Old moves fade away. Only N moves allowed per player.", Icons.Default.DeleteSweep),
    GRAVITY("Gravity", "Pieces fall to the bottom of the column.", Icons.Default.ArrowDownward),
    BLOCKED("Blocked", "Random cells are blocked at start.", Icons.Default.Block),
    SPEED("Speed", "3 seconds to move or you skip turn!", Icons.Default.AvTimer),
    FOG("Fog", "Adjacent cells hidden until you play near them.", Icons.Default.BlurOff),
    BOMB("Bomb", "Special move: Clears 3x3 area instantly.", Icons.Default.Bolt),
    CHAOS("Chaos", "10% chance your piece swaps with opponent.", Icons.Default.Shuffle),
    ULTIMATE("Ultimate", "9x9 Grid. Win 3 subgrids in a row to win!", Icons.Default.Grid3x3)
}

data class Move(val index: Int, val player: Player, val timestamp: Long = System.currentTimeMillis())
