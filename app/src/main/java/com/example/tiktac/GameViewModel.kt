package com.example.tiktac

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.filled.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class GameState(
    val board: List<Player?>,
    val blockedCells: Set<Int> = emptySet(),
    val fogCells: Set<Int> = emptySet(),
    val gridSize: Int,
    val currentPlayer: Player,
    val winner: Player? = null,
    val isDraw: Boolean = false,
    val winCondition: Int,
    val winningLine: List<Int>? = null,
    val moveHistory: List<Move> = emptyList(),
    val currentMode: GameMode = GameMode.CLASSIC,
    val timeLeft: Float = 1.0f,
    val soundEvent: String? = null,
    val showConfetti: Boolean = false,
    // Ultimate Mode
    val activeGridIndex: Int? = null, // Index of the 3x3 subgrid (0-8) valid for next move. Null = Any.
    val subGridWinners: List<Player?> = List(9) { null }, // Winners of local 3x3 grids
    // Persistence & Settings
    val scoreX: Int = 0,
    val scoreO: Int = 0,
    val draws: Int = 0,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val opponentType: OpponentType = OpponentType.PVP,
    val isAiThinking: Boolean = false
)

enum class Player { X, O }
enum class OpponentType { PVP, AI_EASY, AI_HARD }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("tiktac_prefs", Context.MODE_PRIVATE)

    var state by mutableStateOf(GameState(
        board = List(9) { null },
        gridSize = 3,
        currentPlayer = Player.X,
        winCondition = 3,
        // Load persisted values
        scoreX = prefs.getInt("scoreX", 0),
        scoreO = prefs.getInt("scoreO", 0),
        draws = prefs.getInt("draws", 0),
        isSoundEnabled = prefs.getBoolean("sound", true),
        isHapticsEnabled = prefs.getBoolean("haptics", true),
        opponentType = OpponentType.values()[prefs.getInt("opponentType", 0)]
    ))
        private set

    private var timberJob: Job? = null
    private val TURN_DURATION = 3000L

    fun setMode(mode: GameMode) {
        state = state.copy(currentMode = mode)
        resetGame()
    }

    fun toggleSound(enabled: Boolean) {
        state = state.copy(isSoundEnabled = enabled)
        prefs.edit().putBoolean("sound", enabled).apply()
    }

    fun toggleHaptics(enabled: Boolean) {
        state = state.copy(isHapticsEnabled = enabled)
        prefs.edit().putBoolean("haptics", enabled).apply()
    }

    fun setOpponent(type: OpponentType) {
        state = state.copy(opponentType = type)
        prefs.edit().putInt("opponentType", type.ordinal).apply()
        resetGame()
    }

    fun resetStats() {
        state = state.copy(scoreX = 0, scoreO = 0, draws = 0)
        prefs.edit()
            .putInt("scoreX", 0)
            .putInt("scoreO", 0)
            .putInt("draws", 0)
            .apply()
    }

    // Pass false to keep board but reset other things? No, this is game reset.
    fun resetGame(newGridSize: Int = state.gridSize, newWinCondition: Int = state.winCondition) {
        timberJob?.cancel()
        
        val finalGridSize = if (state.currentMode == GameMode.ULTIMATE) 9 else newGridSize
        val totalCells = finalGridSize * finalGridSize
        var blocked = emptySet<Int>()
        
        if (state.currentMode == GameMode.BLOCKED) {
            val numBlocked = (totalCells * 0.15).toInt().coerceAtLeast(1)
            val allIndices = (0 until totalCells).toMutableList()
            allIndices.shuffle()
            blocked = allIndices.take(numBlocked).toSet()
        }

        state = state.copy(
            board = List(totalCells) { null },
            blockedCells = blocked,
            fogCells = if (state.currentMode == GameMode.FOG) emptySet() else (0 until totalCells).toSet(),
            gridSize = finalGridSize,
            currentPlayer = Player.X,
            winCondition = newWinCondition,
            winningLine = null,
            winner = null,
            isDraw = false,
            showConfetti = false,
            moveHistory = emptyList(),
            activeGridIndex = null,
            subGridWinners = List(9) { null }
        )

        if (state.currentMode == GameMode.SPEED) {
            startTimer()
        }
    }

    private fun startTimer() {
        timberJob?.cancel()
        timberJob = viewModelScope.launch {
            val steps = 100
            for (i in 0..steps) {
                state = state.copy(timeLeft = 1.0f - (i / steps.toFloat()))
                delay(TURN_DURATION / steps)
            }
            switchTurn()
            playSound("pop") 
            startTimer()
        }
    }

    fun consumeSound() {
        state = state.copy(soundEvent = null)
    }
    
    fun makeMove(index: Int, fromAi: Boolean = false) {
        // Prevent user input if AI is thinking or if it's AI's turn (in single player)
        if (!fromAi) {
            if (state.isAiThinking) return
            if (state.opponentType != OpponentType.PVP && state.currentPlayer == Player.O) return
        }

        if (state.winner != null || state.board[index] != null || state.blockedCells.contains(index)) return

        // Speed Mode: Cancel timer immediately if move is valid preventing race conditions
        if (state.currentMode == GameMode.SPEED) {
            timberJob?.cancel()
        }

        // Ultimate Mode Logic
        if (state.currentMode == GameMode.ULTIMATE) {
            // 1. Check Constraint
            val row = index / 9
            val col = index % 9
            val subGridRow = row / 3
            val subGridCol = col / 3
            val subGridIndex = subGridRow * 3 + subGridCol
            
            if (state.activeGridIndex != null && state.activeGridIndex != subGridIndex) {
                 // Invalid move (wrong subgrid)
                 playSound("pop") // Or error sound?
                 return
            }
            
            // 2. Play Move
            val newBoard = state.board.toMutableList()
            newBoard[index] = state.currentPlayer
            playSound("move")
            
            // 3. Check Local Winner (SubGrid)
            // Subgrid cells:
            // R: subGridRow*3 -> +3
            // C: subGridCol*3 -> +3
            // We need to extract this 3x3 into a list or check indices directly.
            // Helper to check 3x3 at offset
            var newSubWinners = state.subGridWinners.toMutableList()
            var localWin = false
            
            if (newSubWinners[subGridIndex] == null) {
                // Only check if not already won (or full?)
                // Check rows/cols/diagonals within the 3x3 subgrid region in the 9x9 board
                if (checkUltimateSubWin(newBoard, subGridRow, subGridCol, state.currentPlayer)) {
                    newSubWinners[subGridIndex] = state.currentPlayer
                    localWin = true
                    // Visual effect for local win?
                }
            }
            
            // 4. Update Active Grid for Next Turn
            // The move was at (row, col) absolute.
            // Relative in subgrid: (row % 3, col % 3)
            // This determines the NEXT subgrid index.
            val nextRowInMeta = row % 3 // 0, 1, 2
            val nextColInMeta = col % 3 // 0, 1, 2
            val nextGridIndex = nextRowInMeta * 3 + nextColInMeta
            
            // If next target grid is already won or full, Active = null (Any)
            // Need to check if nextGrid is full.
            val isNextGridFull = isSubGridFull(newBoard, nextRowInMeta, nextColInMeta)
            val isNextGridWon = newSubWinners[nextGridIndex] != null
            
            val nextActive = if (isNextGridWon || isNextGridFull) null else nextGridIndex
            
            state = state.copy(
                board = newBoard,
                subGridWinners = newSubWinners,
                activeGridIndex = nextActive,
                moveHistory = state.moveHistory + Move(index, state.currentPlayer)
            )
            
            // 5. Check Global Winner (Meta Board)
            // Check newSubWinners (list of 9) as a 3x3 board
            val globalWin = checkWinner(newSubWinners, 3, state.currentPlayer, 3)
            
            if (globalWin != null) {
                updateScore(state.currentPlayer)
                state = state.copy(
                    winner = state.currentPlayer,
                    winningLine = globalWin, // Indices 0-8 of subgrids. UI needs to handle this scale.
                    soundEvent = "win",
                    showConfetti = true
                )
                timberJob?.cancel()
                return
            } else if (newSubWinners.all { it != null } || newBoard.none { it == null }) { // Draw conditions
                 // If all subgrids won/drawn...
                 // Simplification: If board full.
                 updateScore(null)
                 state = state.copy(isDraw = true, soundEvent = "draw")
                 return
            }
            
            switchTurn()
            return
        }

        // Standard Modes Logic
        var effectiveIndex = index
        if (state.currentMode == GameMode.GRAVITY) {
            val col = index % state.gridSize
            for (r in state.gridSize - 1 downTo 0) {
                val idx = r * state.gridSize + col
                if (state.board[idx] == null && !state.blockedCells.contains(idx)) {
                    effectiveIndex = idx
                    break
                }
            }
            if (state.board[effectiveIndex] != null) return
        }

        // Bomb Logic
        var isBomb = false
        if (state.currentMode == GameMode.BOMB && Random.nextFloat() < 0.15f) isBomb = true

        val newBoard = state.board.toMutableList()
        var newMoves = state.moveHistory.toMutableList()

        if (isBomb) {
            val row = effectiveIndex / state.gridSize
            val col = effectiveIndex % state.gridSize
            for (r in row - 1..row + 1) {
                for (c in col - 1..col + 1) {
                    if (r in 0 until state.gridSize && c in 0 until state.gridSize) {
                        newBoard[r * state.gridSize + c] = null 
                    }
                }
            }
            playSound("bomb")
        } else {
            var placedPlayer = state.currentPlayer
            if (state.currentMode == GameMode.CHAOS && Random.nextFloat() < 0.10f) {
                placedPlayer = if (placedPlayer == Player.X) Player.O else Player.X
            }

            newBoard[effectiveIndex] = placedPlayer
            newMoves.add(Move(effectiveIndex, placedPlayer))
            playSound("move") 
        }

        // Fading Logic
        if (state.currentMode == GameMode.FADING && newMoves.size > state.gridSize + 2) { 
            val oldest = newMoves.removeAt(0)
            if (!isBomb) newBoard[oldest.index] = null
        }
        
        // Fog Logic
        val newFog = state.fogCells.toMutableSet()
        if (state.currentMode == GameMode.FOG) {
             val row = effectiveIndex / state.gridSize
             val col = effectiveIndex % state.gridSize
             for (r in row - 1..row + 1) {
                for (c in col - 1..col + 1) {
                    if (r in 0 until state.gridSize && c in 0 until state.gridSize) {
                        newFog.add(r * state.gridSize + c)
                    }
                }
            }
        }

        state = state.copy(
            board = newBoard,
            moveHistory = newMoves,
            fogCells = if (state.currentMode == GameMode.FOG) newFog else state.fogCells,
            isAiThinking = false // AI move completed
        )

        val winResult = checkWinner(newBoard, state.gridSize, state.currentPlayer, state.winCondition)
        val isFull = newBoard.none { it == null }

        if (winResult != null) {
            if (state.currentMode == GameMode.MISERE) {
                // Lose
                val winner = if (state.currentPlayer == Player.X) Player.O else Player.X
                updateScore(winner)
                 state = state.copy(
                    winner = winner,
                    winningLine = winResult,
                     soundEvent = "lose",
                     showConfetti = false
                )
            } else {
                 // Win
                 updateScore(state.currentPlayer)
                 state = state.copy(
                    winner = state.currentPlayer,
                    winningLine = winResult,
                    soundEvent = "win",
                    showConfetti = true
                )
            }
            timberJob?.cancel()
        } else if (isFull) {
            updateScore(null) // Draw
            state = state.copy(isDraw = true, currentPlayer = if (state.currentPlayer == Player.X) Player.O else Player.X, soundEvent = "draw")
            timberJob?.cancel()
        } else {
            switchTurn()
        }
    }
    
    private fun updateScore(winner: Player?) {
        var sX = state.scoreX
        var sO = state.scoreO
        var d = state.draws
        
        if (winner == Player.X) sX++
        else if (winner == Player.O) sO++
        else d++
        
        state = state.copy(scoreX = sX, scoreO = sO, draws = d)
        
        prefs.edit()
            .putInt("scoreX", sX)
            .putInt("scoreO", sO)
            .putInt("draws", d)
            .apply()
    }

    private fun switchTurn() {
        if (!state.isDraw && state.winner == null) {
            val nextPlayer = if (state.currentPlayer == Player.X) Player.O else Player.X
            state = state.copy(currentPlayer = nextPlayer)
            
            if (state.opponentType != OpponentType.PVP && nextPlayer == Player.O) {
                // AI Turn
                state = state.copy(isAiThinking = true)
                viewModelScope.launch {
                    delay(500) // Thinking time
                    makeAiMove()
                }
            } else if (state.currentMode == GameMode.SPEED) {
                startTimer()
            }
        }
    }
    
    private fun makeAiMove() {
        if (state.winner != null || state.isDraw) return
        
        // ULTIMATE MODE AI
        if (state.currentMode == GameMode.ULTIMATE) {
            // 1. Determine Valid Moves
            val allEmpty = state.board.indices.filter { state.board[it] == null }
            val validMoves = if (state.activeGridIndex != null) {
                // Must play in active subgrid
                allEmpty.filter { 
                    val row = it / 9
                    val col = it % 9
                    val subRow = row / 3
                    val subCol = col / 3
                    val subIdx = subRow * 3 + subCol
                    subIdx == state.activeGridIndex
                }
            } else {
                // Can play in any subgrid (that isn't full/won ideally, but let's stick to simple legal moves)
                // Better heuristic: Filter out moves in full/won subgrids if possible?
                // For now, allow any empty cell, makeMove handles legality if we add checks there, 
                // but standard rules say "anywhere".
                allEmpty
            }
            
            if (validMoves.isEmpty()) return 
            
            // 2. Simple Heuristic: Win Local Subgrid -> Block Local Subgrid -> Random
            
            fun findLocalWin(player: Player): Int? {
                for (index in validMoves) {
                    val tempBoard = state.board.toMutableList()
                    tempBoard[index] = player
                    val r = index / 9
                    val c = index % 9
                    val subR = r / 3
                    val subC = c / 3
                    if (checkUltimateSubWin(tempBoard, subR, subC, player)) return index
                }
                return null
            }

            // A. Try to win local grid
            findLocalWin(Player.O)?.let { makeMove(it, true); return }
            
            // B. Block opponent winning local grid
            findLocalWin(Player.X)?.let { makeMove(it, true); return }
            
            // C. Random
            makeMove(validMoves.random(), true)
            return
        }

        // STANDARD MODES AI
        // Helper
        fun findWinningMove(player: Player): Int? {
            val available = state.board.indices.filter { state.board[it] == null && !state.blockedCells.contains(it) }
            for (index in available) {
                val tempBoard = state.board.toMutableList()
                tempBoard[index] = player
                if (checkWinner(tempBoard, state.gridSize, player, state.winCondition) != null) {
                    return index
                }
            }
            return null
        }
        
        if (state.opponentType == OpponentType.AI_HARD) {
            // HARD MODE (Minimax approximation or full check)
            // 1. Instant Win
            findWinningMove(Player.O)?.let { makeMove(it, true); return }
            
            // 2. Block Instant Loss
            findWinningMove(Player.X)?.let { makeMove(it, true); return }
            
            // 3. Center (if 3x3 or available)
            val center = (state.gridSize * state.gridSize) / 2
            if (state.board[center] == null && !state.blockedCells.contains(center)) {
                makeMove(center, true)
                return
            }
            
            // 4. Random but smart (corners preferred)
            val available = state.board.indices.filter { state.board[it] == null && !state.blockedCells.contains(it) }
            // Filter corners if 3x3
            if (state.gridSize == 3) {
                val corners = listOf(0, 2, 6, 8).filter { it in available }
                if (corners.isNotEmpty()) {
                    makeMove(corners.random(), true)
                    return
                }
            }
            if (available.isNotEmpty()) makeMove(available.random(), true)
             
        } else {
             // EASY MODE
            var moveIndex = findWinningMove(Player.O) // 1. Try to Win
            if (moveIndex == null) {
                moveIndex = findWinningMove(Player.X) // 2. Block Opponent
            }
            if (moveIndex == null) {
                // 3. Random Valid Move
                val available = state.board.indices.filter { state.board[it] == null && !state.blockedCells.contains(it) }
                if (available.isNotEmpty()) {
                    moveIndex = available.random()
                }
            }
            moveIndex?.let { makeMove(it, true) }
        }
    }

    private fun playSound(event: String) {
        state = state.copy(soundEvent = event)
    }

    private fun checkWinner(board: List<Player?>, size: Int, player: Player, winCondition: Int): List<Int>? {
        fun checkSequence(indices: List<Int>): Boolean {
            return indices.all { board[it] == player }
        }
        for (row in 0 until size) {
            for (col in 0..size - winCondition) {
                val indices = (0 until winCondition).map { (row * size) + (col + it) }
                if (checkSequence(indices)) return indices
            }
        }
        for (col in 0 until size) {
             for (row in 0..size - winCondition) {
                val indices = (0 until winCondition).map { ((row + it) * size) + col }
                if (checkSequence(indices)) return indices
            }
        }
        for (row in 0..size - winCondition) {
            for (col in 0..size - winCondition) {
                val indices = (0 until winCondition).map { ((row + it) * size) + (col + it) }
                if (checkSequence(indices)) return indices
            }
        }
         for (row in 0..size - winCondition) {
            for (col in winCondition - 1 until size) {
                val indices = (0 until winCondition).map { ((row + it) * size) + (col - it) }
                if (checkSequence(indices)) return indices
            }
        }
        return null
    }
    
    // Ultimate Helpers
    private fun checkUltimateSubWin(board: List<Player?>, gridRow: Int, gridCol: Int, player: Player): Boolean {
        // gridRow/Col are 0..2 (Meta coordinates)
        // Global offsets: startRow = gridRow*3, startCol = gridCol*3
        val startRow = gridRow * 3
        val startCol = gridCol * 3
        
        fun getCell(r: Int, c: Int) = board[(startRow + r) * 9 + (startCol + c)]
        
        // Rows
        for (i in 0..2) if ((0..2).all { getCell(i, it) == player }) return true
        // Cols
        for (i in 0..2) if ((0..2).all { getCell(it, i) == player }) return true
        // Diagonals
        if (getCell(0,0) == player && getCell(1,1) == player && getCell(2,2) == player) return true
        if (getCell(0,2) == player && getCell(1,1) == player && getCell(2,0) == player) return true
        
        return false
    }
    
    private fun isSubGridFull(board: List<Player?>, gridRow: Int, gridCol: Int): Boolean {
         val startRow = gridRow * 3
         val startCol = gridCol * 3
         for (r in 0..2) {
             for (c in 0..2) {
                 if (board[(startRow + r) * 9 + (startCol + c)] == null) return false
             }
         }
         return true
    }
}
