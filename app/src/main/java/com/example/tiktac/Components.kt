package com.example.tiktac


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.SmartToy
import kotlinx.coroutines.launch

@Composable
fun DonationButton() {
    val uriHandler = LocalUriHandler.current
    
    // Flower / Squircle Shape
    FloatingActionButton(
        onClick = { uriHandler.openUri("https://ko-fi.com/sourish25") },
        shape = RoundedCornerShape(20.dp), // Soft Squircle / Flower-ish
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.padding(16.dp).size(72.dp)
    ) {
        Icon(androidx.compose.material.icons.Icons.Default.Favorite, "Donate", modifier = Modifier.size(32.dp))
    }
}

@Composable
fun ResetButton(onReset: () -> Unit) {
    FloatingActionButton(
        onClick = onReset,
        shape = CircleShape, // Circular as requested
        containerColor = MaterialTheme.colorScheme.tertiaryContainer, // Same theme as donation
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.padding(16.dp).size(72.dp) // Same size as donation button
    ) {
        Icon(androidx.compose.material.icons.Icons.Default.Refresh, "Reset Game", modifier = Modifier.size(32.dp))
    }
}

@Composable
fun BoardComponent(
    state: GameState,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(state.gridSize),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.board.size) { index ->
                val isVisible = if (state.currentMode == GameMode.FOG) state.fogCells.contains(index) || state.board[index] != null else true
                
                if (isVisible) {
                    val isBlocked = state.blockedCells.contains(index)
                    CellComponent(
                        player = state.board[index],
                        onClick = { onCellClick(index) },
                        isWinningCell = state.winningLine?.contains(index) == true,
                        isBlocked = isBlocked
                    )
                } else {
                     Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray.copy(alpha=0.3f))
                            .clickable(onClick = { onCellClick(index) })
                     )
                }
            }
        }
        
        // Win Strikethrough
        state.winningLine?.let { winIndices ->
            if (winIndices.isNotEmpty()) {
                val startIdx = winIndices.first()
                val endIdx = winIndices.last()
                val gridSize = state.gridSize // Renamed from size to avoid shadow
                val color = MaterialTheme.colorScheme.primary
                
                Canvas(modifier = Modifier.matchParentSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val cellWidth = canvasWidth / gridSize
                    val cellHeight = canvasHeight / gridSize 
                    
                    val startRow = startIdx / gridSize
                    val startCol = startIdx % gridSize
                    val endRow = endIdx / gridSize
                    val endCol = endIdx % gridSize
                    
                    val x1 = (startCol + 0.5f) * cellWidth
                    val y1 = (startRow + 0.5f) * cellHeight
                    val x2 = (endCol + 0.5f) * cellWidth
                    val y2 = (endRow + 0.5f) * cellHeight

                    drawLine(
                        color = color,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun CellComponent(
    player: Player?,
    onClick: () -> Unit,
    isWinningCell: Boolean,
    isBlocked: Boolean
) {
    val shape: Shape = if (isWinningCell) RoundedCornerShape(24.dp) else RoundedCornerShape(16.dp)
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                color = when {
                    isBlocked -> MaterialTheme.colorScheme.errorContainer
                    isWinningCell -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(enabled = player == null && !isBlocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBlocked) {
             Icon(androidx.compose.material.icons.Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error)
        } else if (player != null) {
            AnimatedSymbol(player = player)
        }
    }
}

@Composable
fun AnimatedSymbol(player: Player) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(player) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 300, easing = LinearEasing))
    }

    val color = MaterialTheme.colorScheme.onSurfaceVariant
    
    Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
        val strokeWidth = size.width * 0.15f
        val cap = StrokeCap.Round

        if (player == Player.X) {
            val p = progress.value
            if (p > 0) {
                 drawLine(color, Offset(0f, 0f), Offset(size.width * p, size.height * p), strokeWidth, cap)
            }
             drawLine(color, Offset(size.width, 0f), Offset(size.width * (1 - p), size.height * p), strokeWidth, cap)
        } else {
            drawArc(color, 0f, 360f * progress.value, false, style = Stroke(strokeWidth, cap = cap))
        }
    }
}

@Composable
fun GameModeSelector(
    currentMode: GameMode,
    onModeSelect: (GameMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp), // More rounded for larger size
            color = MaterialTheme.colorScheme.secondaryContainer, // "Catchy" nice color, distinct from Tertiary
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp) // Bigger padding
            ) {
                Icon(currentMode.icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) // Larger icon
                Spacer(modifier = Modifier.width(12.dp))
                // Larger Text
                Text(currentMode.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(12.dp))
                Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(IntrinsicSize.Max) // Attempt to match, but Width logic in Compose Menus can be tricky. Fixed size in Modifier is safer if parent is known.
        ) {
             GameMode.values().forEach { mode ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(mode.icon, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(mode.title)
                        } 
                    },
                    onClick = { 
                        onModeSelect(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    currentSize: Int,
    currentWinCondition: Int,
    onApply: (Int, Int) -> Unit,
    currentFontIndex: Int,
    currentFont: FontFamily? = null,
    onFontChange: (Int) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    // New Params
    scoreX: Int,
    scoreO: Int,
    draws: Int,
    onResetStats: () -> Unit,
    isSoundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    isHapticsEnabled: Boolean,
    onToggleHaptics: (Boolean) -> Unit
) {
    if (show) {
        val sheetState = rememberModalBottomSheetState()
        var size by remember { mutableStateOf(currentSize) }
        var winCondition by remember { mutableStateOf(currentWinCondition) }
        
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Dark Mode", modifier = Modifier.weight(1f))
                    Switch(checked = isDarkTheme, onCheckedChange = onThemeToggle)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Font Style", style = MaterialTheme.typography.bodyMedium)
                
                var expanded by remember { mutableStateOf(false) }
                val fonts = AppFonts.AllFonts.map { it.first }
                
                Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart)) {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(fonts.getOrElse(currentFontIndex) { "Select Font" })
                        Spacer(Modifier.width(8.dp))
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        fonts.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { 
                                    onFontChange(index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Stats Section
                Text("Scoreboard", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ScoreItem("X Wins", scoreX)
                    ScoreItem("O Wins", scoreO)
                    ScoreItem("Draws", draws)
                }
                TextButton(onClick = onResetStats, modifier = Modifier.align(Alignment.End)) {
                    Text("Reset Stats", color = MaterialTheme.colorScheme.error)
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Sound & Haptics", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Sound Effects", modifier = Modifier.weight(1f))
                    Switch(checked = isSoundEnabled, onCheckedChange = onToggleSound)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Vibration", modifier = Modifier.weight(1f))
                    Switch(checked = isHapticsEnabled, onCheckedChange = onToggleHaptics)
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Grid Size: ${size}x${size}", style = MaterialTheme.typography.titleMedium)
                Slider(value = size.toFloat(), onValueChange = { size = it.toInt() }, valueRange = 3f..10f, steps = 6)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Win Sequence: $winCondition", style = MaterialTheme.typography.titleMedium)
                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (3..5).filter { it <= size }.forEach { cond ->
                         SuggestionChip(
                            onClick = { winCondition = cond },
                            label = { Text("$cond") },
                            border = if (winCondition == cond) null else SuggestionChipDefaults.suggestionChipBorder(borderColor = Color.Transparent)
                        )
                    }
                }
                 LaunchedEffect(size) { if (winCondition > size) winCondition = size }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { onApply(size, winCondition) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Game")
                }

                Divider(modifier = Modifier.padding(vertical = 24.dp))

                Text("Developer", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                val uriHandler = LocalUriHandler.current
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalButton(
                        onClick = { uriHandler.openUri("https://github.com/Sourish25") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("GitHub")
                    }
                    FilledTonalButton(
                        onClick = { uriHandler.openUri("https://www.linkedin.com/in/sourish-maity-91722a34a/") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("LinkedIn")
                    }
                }
            }
        }
    }
}

@Composable
fun OpponentSelector(
    currentType: OpponentType,
    onSelect: (OpponentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = OpponentType.values()
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    BoxWithConstraints(
        modifier = modifier
            .height(64.dp) // Taller for better touch target
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val segmentWidthPx = maxWidthPx / options.size
        
        val indicatorOffset by androidx.compose.animation.core.animateFloatAsState(
            targetValue = currentType.ordinal * segmentWidthPx,
            animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
        )
        
        // Indicator
        Box(
            modifier = Modifier
                .width(maxWidth / options.size)
                .fillMaxHeight()
                .offset { androidx.compose.ui.unit.IntOffset(indicatorOffset.toInt(), 0) }
                .padding(4.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
        )
        
        // Labels
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { type ->
                val isSelected = type == currentType
                val label = when(type) {
                    OpponentType.PVP -> "PvP"
                    OpponentType.AI_EASY -> "Easy"
                    OpponentType.AI_HARD -> "Hard"
                }
                val icon = when(type) {
                    OpponentType.PVP -> androidx.compose.material.icons.Icons.Default.Groups
                    OpponentType.AI_EASY -> androidx.compose.material.icons.Icons.Default.Face
                    OpponentType.AI_HARD -> androidx.compose.material.icons.Icons.Default.SmartToy
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null // Ripple handled by indicator movement visually
                        ) { onSelect(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreItem(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = score.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun UltimateBoardComponent(
    state: GameState,
    onCellClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3 Rows of SubGrids -> Iterating Meta Rows 0..2
        for (metaRow in 0..2) {
            Row {
                for (metaCol in 0..2) {
                    val subGridIndex = metaRow * 3 + metaCol
                    val isActive = state.winner == null && (state.activeGridIndex == null || state.activeGridIndex == subGridIndex)
                    val winner = state.subGridWinners[subGridIndex]
                    
                    SubGrid(
                        state = state,
                        subGridIndex = subGridIndex,
                        isActive = isActive,
                        winner = winner,
                        onCellClick = onCellClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .aspectRatio(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (state.winner == null) {
            Text(
                text = if (state.activeGridIndex != null) "Play in the highlighted grid!" else "Play anywhere!",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SubGrid(
    state: GameState,
    subGridIndex: Int,
    isActive: Boolean,
    winner: Player?,
    onCellClick: (Int) -> Unit,
    modifier: Modifier
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=0.2f)
    val borderWidth = if (isActive) 3.dp else 1.dp
    
    Box(
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp)) // Border around SubGrid
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
    ) {
        if (winner != null) {
             // Show Big Winner Symbol
             Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                 // Render Background alpha
                 Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha=0.6f)))
                 // Render Symbol
                 AnimatedSymbol(player = winner)
             }
        } else {
            // Render 3x3 Cells
             Column {
                for (r in 0..2) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0..2) {
                            // Calculate Global Index
                            val globalRow = (subGridIndex / 3) * 3 + r
                            val globalCol = (subGridIndex % 3) * 3 + c
                            val globalIndex = globalRow * 9 + globalCol
                            
                            val cellPlayer = state.board.getOrNull(globalIndex)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = isActive && cellPlayer == null, onClick = { onCellClick(globalIndex) }),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cellPlayer != null) {
                                    PlayerSymbolSmall(cellPlayer)
                                }
                            }
                        }
                    }
                }
             }
        }
    }
}

@Composable
fun PlayerSymbolSmall(player: Player) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
        val strokeWidth = 3.dp.toPx()
        val cap = StrokeCap.Round
        if (player == Player.X) {
            drawLine(color, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth, cap)
            drawLine(color, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth, cap)
        } else {
             drawCircle(color, style = Stroke(strokeWidth))
        }
    }
}
