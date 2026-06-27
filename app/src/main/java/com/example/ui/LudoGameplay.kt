package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.LudoViewModel
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LudoGameplayScreen(
    viewModel: LudoViewModel,
    state: GameState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = getThemeColors(state.settings.themeMode)

    var isRollingLocal by remember { mutableStateOf(false) }
    var displayedRollLocal by remember { mutableStateOf(1) }

    // Synchronize local dice rolling animation
    LaunchedEffect(state.diceValue, state.isDiceRolled) {
        if (state.isDiceRolled && !isRollingLocal) {
            displayedRollLocal = state.diceValue
        }
    }

    // Helper for dice roll trigger
    fun triggerDiceRollAnimation() {
        if (isRollingLocal || state.isDiceRolled || state.gamePhase != GamePhase.PLAYING) return
        
        coroutineScope.launch {
            isRollingLocal = true
            // Play rapid rolling animation
            repeat(10) {
                displayedRollLocal = (1..6).random()
                delay(60)
            }
            viewModel.rollDice()
            isRollingLocal = false
        }
    }

    // Highlight valid moves for the active player
    val currentPlayer = state.currentTurnPlayer
    val highlightedTokens = if (state.isDiceRolled && state.hasMoves && currentPlayer != null) {
        viewModel.getValidMovesForPlayer(currentPlayer, state.diceValue, state.gameMode)
    } else {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Top Bar / Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.resetToSetup() }
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "الخلف", tint = colors.accent)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Le Dé Moderne 🎲",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        brush = Brush.horizontalGradient(
                            colors = listOf(colors.playerColors[0], colors.playerColors[3], colors.accent)
                        )
                    )
                )
                Text(
                    text = "برمجة وتطوير mzn ✨",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.accent.copy(alpha = 0.9f)
                    )
                )
            }

            // Reserved spacer for layout symmetry (Admin panel opens automatically on rolling 6 twice in a row)
            Spacer(modifier = Modifier.size(48.dp))
        }

        // --- 2. Active Turn Indicator Card ---
        currentPlayer?.let { player ->
            TurnIndicatorCard(
                player = player,
                diceValue = displayedRollLocal,
                isRolling = isRollingLocal,
                state = state,
                theme = colors,
                onRollClick = { triggerDiceRollAnimation() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- 3. Main Ludo Board Layout ---
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            LudoBoard(
                state = state,
                highlightedTokens = highlightedTokens,
                onTokenClick = { pId, tId ->
                    viewModel.moveToken(pId, tId)
                },
                isRolling = isRollingLocal,
                diceValue = displayedRollLocal,
                onDiceRoll = { triggerDiceRollAnimation() }
            )

            // Overlays (e.g. Winner screen)
            if (state.gamePhase == GamePhase.FINISHED) {
                val winner = state.players.find { it.id == state.winnerId }
                winner?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(16.dp))
                            .clickable { /* Block taps underneath */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "🏆 البطل الفائز 🏆",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.accent,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تهانينا الحارة بالفوز في هذه الجولة الممتعة!",
                                fontSize = 14.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.resetToSetup() },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Text("إعداد مباراة جديدة", color = colors.background, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var showLogsStats by remember { mutableStateOf(false) }

        // --- 4. Controls & Terminal View (Logs / Admin / Settings) ---
        Box(
            modifier = Modifier
                .weight(if (state.adminPanelOpen || showLogsStats) 1f else 0.25f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (state.adminPanelOpen) {
                AdminControlPanel(
                    viewModel = viewModel,
                    state = state,
                    theme = colors
                )
            } else if (showLogsStats) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showLogsStats = false },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("إخفاء السجل والأحداث ❌", color = colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GameActivityLogsAndStats(
                            state = state,
                            theme = colors,
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                Button(
                    onClick = { showLogsStats = true },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.trackCell),
                    border = BorderStroke(1.dp, colors.trackBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "عرض سجل الأحداث والإعدادات 📜⚙️",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TurnIndicatorCard(
    player: Player,
    diceValue: Int,
    isRolling: Boolean,
    state: GameState,
    theme: LudoThemeColors,
    onRollClick: () -> Unit
) {
    val playerColor = theme.playerColors[player.colorIndex]
    val activeBrush = Brush.horizontalGradient(listOf(playerColor, playerColor.copy(alpha = 0.6f)))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = theme.trackCell),
        border = BorderStroke(1.5.dp, playerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(activeBrush, CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (player.type == PlayerType.AI) "🤖" else "👤",
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = player.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (player.type) {
                            PlayerType.HUMAN -> "دورك للعب الآن يدوياً"
                            PlayerType.AI -> "الذكاء الاصطناعي يفكر..."
                            else -> ""
                        },
                        fontSize = 11.sp,
                        color = theme.trackBorder
                    )
                }
            }

            // Dice element
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isDiceRolled && !isRolling) {
                    Text(
                        text = "الرمية: $diceValue",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.accent,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                DiceView(
                    value = diceValue,
                    isRolling = isRolling,
                    onClick = onRollClick,
                    enabled = !state.isDiceRolled && player.type == PlayerType.HUMAN && state.gamePhase == GamePhase.PLAYING,
                    theme = theme,
                    playerColor = playerColor
                )
            }
        }
    }
}

@Composable
fun DiceView(
    value: Int,
    isRolling: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    theme: LudoThemeColors,
    playerColor: Color
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRolling) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (enabled) 1.05f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )

    Box(
        modifier = Modifier
            .size(54.dp)
            .rotate(if (isRolling) rotation else 0f)
            .shadow(if (enabled) 6.dp else 2.dp, RoundedCornerShape(12.dp))
            .background(
                if (enabled) playerColor else theme.trackBorder.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .border(2.dp, if (enabled) Color.White else theme.trackBorder, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw dice dots
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val dSize = size.width
            val dotRadius = dSize * 0.11f
            val c = dSize / 2f
            val left = dSize * 0.22f
            val right = dSize * 0.78f
            val top = dSize * 0.22f
            val bottom = dSize * 0.78f

            val dotColor = if (enabled) Color.White else theme.background

            fun drawDot(cx: Float, cy: Float) {
                drawCircle(color = dotColor, radius = dotRadius, center = Offset(cx, cy))
            }

            when (value) {
                1 -> {
                    drawDot(c, c)
                }
                2 -> {
                    drawDot(left, top)
                    drawDot(right, bottom)
                }
                3 -> {
                    drawDot(left, top)
                    drawDot(c, c)
                    drawDot(right, bottom)
                }
                4 -> {
                    drawDot(left, top)
                    drawDot(right, top)
                    drawDot(left, bottom)
                    drawDot(right, bottom)
                }
                5 -> {
                    drawDot(left, top)
                    drawDot(right, top)
                    drawDot(c, c)
                    drawDot(left, bottom)
                    drawDot(right, bottom)
                }
                6 -> {
                    drawDot(left, top)
                    drawDot(right, top)
                    drawDot(left, c)
                    drawDot(right, c)
                    drawDot(left, bottom)
                    drawDot(right, bottom)
                }
            }
        }
    }
}

@Composable
fun GameActivityLogsAndStats(
    state: GameState,
    theme: LudoThemeColors,
    viewModel: LudoViewModel
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 -> Logs, 1 -> Settings, 2 -> Stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .background(theme.trackCell, RoundedCornerShape(24.dp))
            .border(1.dp, theme.trackBorder, RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        // Segment tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.background.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("سجل الأحداث 📜", "الإعدادات ⚙️", "الإحصائيات 📊").forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) theme.accent else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = idx }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (active) Color.White else theme.trackBorder
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Logs terminal
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.gameLogs) { log ->
                        Text(
                            text = log,
                            fontSize = 11.sp,
                            color = if (log.contains("💥") || log.contains("غش")) theme.playerColors[0] else if (log.contains("🏆")) theme.accent else theme.trackBorder,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            1 -> {
                // Settings
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المؤثرات الصوتية:", fontSize = 13.sp, color = theme.textPrimary)
                        Switch(
                            checked = state.settings.soundEnabled,
                            onCheckedChange = { viewModel.toggleSound() },
                            colors = SwitchDefaults.colors(checkedThumbColor = theme.accent, checkedTrackColor = theme.accent.copy(alpha = 0.4f))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الاهتزاز والتفاعل اللمسي:", fontSize = 13.sp, color = theme.textPrimary)
                        Switch(
                            checked = state.settings.vibrationEnabled,
                            onCheckedChange = { viewModel.toggleVibration() },
                            colors = SwitchDefaults.colors(checkedThumbColor = theme.accent, checkedTrackColor = theme.accent.copy(alpha = 0.4f))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("سرعة لعب الروبوت:", fontSize = 13.sp, color = theme.textPrimary)
                        Row {
                            listOf(Pair("سريع", 400L), Pair("عادي", 1000L), Pair("هادئ", 1800L)).forEach { (name, ms) ->
                                val active = state.settings.gameSpeedMs == ms
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .background(if (active) theme.accent else theme.background, RoundedCornerShape(4.dp))
                                        .clickable { viewModel.setGameSpeed(ms) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) theme.background else theme.trackBorder)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Statistics
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.statistics.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد جولات مسجلة حالياً.", fontSize = 13.sp, color = theme.trackBorder)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("البطل الفائز", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.accent)
                            Text("النمط", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.accent)
                            Text("التاريخ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.accent)
                        }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.statistics.take(4)) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.winnerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
                                    Text(item.mode, fontSize = 11.sp, color = theme.trackBorder)
                                    Text(item.date, fontSize = 10.sp, color = theme.trackBorder)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.resetStats() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.playerColors[0].copy(alpha = 0.2f)),
                            modifier = Modifier.align(Alignment.CenterHorizontally).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("مسح سجل الإحصائيات", color = theme.playerColors[0], fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminControlPanel(
    viewModel: LudoViewModel,
    state: GameState,
    theme: LudoThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .background(theme.trackCell, RoundedCornerShape(24.dp))
            .border(2.5.dp, theme.accent, RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🪄 لوحة الإدارة الخارقة والغش",
                fontWeight = FontWeight.Black,
                color = theme.accent,
                fontSize = 15.sp
            )
            IconButton(
                onClick = { viewModel.toggleAdminPanel() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = theme.playerColors[0])
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.forceWin() },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.playerColors[1]),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("فوز فوري 🏆", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.skipTurn() },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.playerColors[3]),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تخطي الدور ⏭️", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // 1. Force a specific dice roll
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🎯 اختر الرقم الذي سيأتيك (1-6):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    modifier = Modifier.align(Alignment.End)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..6).forEach { num ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(theme.accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.5.dp, theme.accent, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectCheatDice(num) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = num.toString(),
                                fontWeight = FontWeight.Black,
                                color = theme.accent,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 2. Release Own Pieces from Base
            val currentPlayerId = state.currentTurnPlayerIndex
            val ownYardTokens = state.players.getOrNull(currentPlayerId)?.tokens?.filter { it.isYard() } ?: emptyList()
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🚪 إخراج قطعة من قطعك التي في القاعدة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    modifier = Modifier.align(Alignment.End)
                )

                if (ownYardTokens.isEmpty()) {
                    Text(
                        text = "جميع قطعك خارج القاعدة بالفعل! 👍",
                        fontSize = 11.sp,
                        color = theme.trackBorder,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ownYardTokens.forEach { token ->
                            AssistChip(
                                onClick = { viewModel.adminReleasePiece(currentPlayerId, token.id) },
                                label = { Text("إخراج القطعة ${token.id + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = theme.playerColors[currentPlayerId],
                                    containerColor = theme.playerColors[currentPlayerId].copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.2.dp, theme.playerColors[currentPlayerId])
                            )
                        }
                    }
                }
            }

            // 3. Choose any piece and eat/capture it
            val opponentActiveTokens = state.players
                .filter { it.id != currentPlayerId && it.type != PlayerType.DISABLED }
                .flatMap { p -> p.tokens.map { t -> Pair(p, t) } }
                .filter { (_, t) -> !t.isYard() && !t.isFinished }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "⚔️ اختيار أي قطعة في الملعب وأكلها (إعادتها للقاعدة):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    modifier = Modifier.align(Alignment.End)
                )

                if (opponentActiveTokens.isEmpty()) {
                    Text(
                        text = "لا توجد قطع للخصوم حالياً على لوحة اللعب! 💨",
                        fontSize = 11.sp,
                        color = theme.trackBorder,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        opponentActiveTokens.forEach { (oppPlayer, token) ->
                            val playerColor = theme.playerColors[oppPlayer.id]
                            AssistChip(
                                onClick = { viewModel.adminCapturePiece(oppPlayer.id, token.id) },
                                label = { Text("أكل قطعة ${oppPlayer.name} #${token.id + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = playerColor,
                                    containerColor = playerColor.copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.2.dp, playerColor)
                            )
                        }
                    }
                }
            }

            // 4. Change winning conditions
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "⚙️ تعديل شروط الفوز بالجولة (عدد القطع للوصول):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary,
                    modifier = Modifier.align(Alignment.End)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..4).forEach { count ->
                        val active = state.settings.targetTokensHome == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .background(if (active) theme.accent else theme.background, RoundedCornerShape(8.dp))
                                .border(1.dp, if (active) theme.accent else theme.trackBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.setTargetTokens(count) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count قطع",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) theme.background else theme.trackBorder
                            )
                        }
                    }
                }
            }
        }
    }
}
