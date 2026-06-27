package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.*
import kotlin.math.*

data class VisualTokenDrawingData(
    val playerOwnerId: Int,
    val tokenId: Int,
    val center: Offset,
    val radius: Float,
    val isHighlighted: Boolean,
    val emoji: String = "👑",
    val tokenShapeIndex: Int = 0
)

private fun getHexagonRadius(angleRad: Float, baseR: Float): Float {
    val pi = 3.14159265f
    // Normalize angle to [0, 2pi]
    var normalizedAngle = angleRad % (2 * pi)
    if (normalizedAngle < 0f) normalizedAngle += 2 * pi
    val angleMod = normalizedAngle % (pi / 3f)
    val diff = angleMod - (pi / 6f)
    return (baseR * cos(pi / 6f)) / cos(diff)
}

@Composable
fun LudoBoard(
    state: GameState,
    highlightedTokens: List<Token>,
    onTokenClick: (playerId: Int, tokenId: Int) -> Unit,
    modifier: Modifier = Modifier,
    isRolling: Boolean = false,
    diceValue: Int = 1,
    onDiceRoll: (() -> Unit)? = null
) {
    // Theme colors mapping
    val colors = getThemeColors(state.settings.themeMode)

    // Pulsating animations for active turn indicator
    val infiniteTransition = rememberInfiniteTransition(label = "turn_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Optimize: Calculate and group tokens outside of DrawScope to avoid GC thrashing and lag!
    val visualTokens = remember(state.players, state.gameMode, highlightedTokens) {
        val list = mutableListOf<VisualTokenDrawingData>()
        val activePlayers = state.players.filter { it.type != PlayerType.DISABLED }
        
        if (state.gameMode == GameMode.FOUR_PLAYERS) {
            val cellGroups = mutableMapOf<String, MutableList<Pair<Int, Token>>>()
            for (player in activePlayers) {
                for (token in player.tokens) {
                    val key = if (token.isYard()) {
                        "yard_${player.id}_${token.id}"
                    } else if (token.isHome(GameMode.FOUR_PLAYERS)) {
                        "home_${player.id}"
                    } else {
                        val coords = get4pTokenCoords(1000f / 15f, player.id, token, state)
                        "cell_${coords.x.toInt()}_${coords.y.toInt()}"
                    }
                    cellGroups.getOrPut(key) { mutableListOf() }.add(Pair(player.id, token))
                }
            }

            cellGroups.forEach { (key, groupList) ->
                val count = groupList.size
                val cellSize = 1000f / 15f
                groupList.forEachIndexed { idx, (pId, token) ->
                    val isHighlighted = highlightedTokens.any { it.playerOwnerId == pId && it.id == token.id }
                    val baseCoords = get4pTokenCoords(cellSize, pId, token, state)
                    val offset = if (count > 1 && !key.startsWith("yard_") && !key.startsWith("home_")) {
                        val angle = (idx * (360f / count)) * (PI / 180f)
                        val spreadRadius = cellSize * 0.22f
                        Offset((spreadRadius * cos(angle)).toFloat(), (spreadRadius * sin(angle)).toFloat())
                    } else if (key.startsWith("home_")) {
                        val angle = (idx * (360f / count) + pId * 45f) * (PI / 180f)
                        val spreadRadius = cellSize * 0.35f
                        Offset((spreadRadius * cos(angle)).toFloat(), (spreadRadius * sin(angle)).toFloat())
                    } else {
                        Offset.Zero
                    }
                    val pObj = activePlayers.find { it.id == pId }
                    list.add(
                        VisualTokenDrawingData(
                            playerOwnerId = pId,
                            tokenId = token.id,
                            center = baseCoords + offset,
                            radius = cellSize * 0.38f,
                            isHighlighted = isHighlighted,
                            emoji = pObj?.emoji ?: "👑",
                            tokenShapeIndex = pObj?.tokenShapeIndex ?: 0
                        )
                    )
                }
            }
        } else {
            val cellGroups = mutableMapOf<String, MutableList<Pair<Int, Token>>>()
            for (player in activePlayers) {
                for (token in player.tokens) {
                    val key = if (token.isYard()) {
                        "yard_${player.id}_${token.id}"
                    } else if (token.isHome(GameMode.SIX_PLAYERS)) {
                        "home"
                    } else {
                        val coords = get6pTokenCoords(1000f, player.id, token, state)
                        "cell_${coords.x.toInt()}_${coords.y.toInt()}"
                    }
                    cellGroups.getOrPut(key) { mutableListOf() }.add(Pair(player.id, token))
                }
            }

            cellGroups.forEach { (key, groupList) ->
                val count = groupList.size
                groupList.forEachIndexed { idx, (pId, token) ->
                    val isHighlighted = highlightedTokens.any { it.playerOwnerId == pId && it.id == token.id }
                    val baseCoords = get6pTokenCoords(1000f, pId, token, state)
                    val radiusScale = when (count) {
                        1 -> 1f
                        2 -> 0.76f
                        3 -> 0.66f
                        else -> 0.54f
                    }
                    val spreadRadius = if (count > 1 && !key.startsWith("yard_")) {
                        1000f * 0.017f
                    } else {
                        0f
                    }
                    val offset = if (count > 1 && !key.startsWith("yard_")) {
                        val angle = (idx * (360f / count)) * (PI / 180f)
                        Offset((spreadRadius * cos(angle)).toFloat(), (spreadRadius * sin(angle)).toFloat())
                    } else {
                        Offset.Zero
                    }
                    val pObj = activePlayers.find { it.id == pId }
                    list.add(
                        VisualTokenDrawingData(
                            playerOwnerId = pId,
                            tokenId = token.id,
                            center = baseCoords + offset,
                            radius = (1000f * 0.024f) * radiusScale,
                            isHighlighted = isHighlighted,
                            emoji = pObj?.emoji ?: "👑",
                            tokenShapeIndex = pObj?.tokenShapeIndex ?: 0
                        )
                    )
                }
            }
        }
        list
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
    ) {
        val animatedVisualTokens = visualTokens.map { vt ->
            val animatedCenter by animateOffsetAsState(
                targetValue = vt.center,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "token_${vt.playerOwnerId}_${vt.tokenId}"
            )
            vt.copy(center = animatedCenter)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state.gamePhase, state.players, animatedVisualTokens) {
                    detectTapGestures { offset ->
                        handleBoardTap(
                            offset,
                            size.width.toFloat(),
                            animatedVisualTokens,
                            onTokenClick,
                            onDiceRoll
                        )
                    }
                }
        ) {
            val boardSize = size.width

            // 1. Draw Background
            drawRect(
                color = colors.background,
                size = size
            )

            if (state.gameMode == GameMode.FOUR_PLAYERS) {
                draw4PlayerBoard(boardSize, state, animatedVisualTokens, colors)
            } else {
                draw6PlayerBoard(
                    boardSize = boardSize,
                    state = state,
                    visualTokens = animatedVisualTokens,
                    theme = colors,
                    isRolling = isRolling,
                    diceValue = diceValue,
                    pulseAlpha = pulseAlpha,
                    pulseScale = pulseScale
                )
            }
        }
    }
}

// 4-Player Board Drawing
private fun DrawScope.draw4PlayerBoard(
    boardSize: Float,
    state: GameState,
    visualTokens: List<VisualTokenDrawingData>,
    theme: LudoThemeColors
) {
    val cellSize = boardSize / 15f

    // Draw grid track borders and safe cells
    for (row in 0..14) {
        for (col in 0..14) {
            // Determine cell type and color
            val isYard = (col < 6 && row < 6) || (col > 8 && row < 6) || (col > 8 && row > 8) || (col < 6 && row > 8)
            val isHome = col in 6..8 && row in 6..8

            if (isYard || isHome) continue

            val x = col * cellSize
            val y = row * cellSize

            // Default cell fill
            var fillCol = theme.trackCell
            var borderCol = theme.trackBorder

            // Color safe zones & start zones
            val isSafe = isSafeCell4p(col, row)
            val pathOwner = getHomePathOwner4p(col, row)

            if (pathOwner != null) {
                fillCol = theme.playerColors[pathOwner].copy(alpha = 0.85f)
            } else if (isSafe) {
                val startOwner = getStartCellOwner4p(col, row)
                fillCol = if (startOwner != null) {
                    theme.playerColors[startOwner].copy(alpha = 0.4f)
                } else {
                    theme.starCell
                }
            }

            drawRect(
                color = fillCol,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize)
            )

            drawRect(
                color = borderCol,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw a cute star on safe cells
            if (isSafe) {
                drawStarIcon(x + cellSize / 2f, y + cellSize / 2f, cellSize * 0.35f, theme.accent)
            }
        }
    }

    // Draw 4 Yards
    draw4Yards(cellSize, theme, state)

    // Draw Central Home Triangle divisions
    val centerStart = 6 * cellSize
    val centerEnd = 9 * cellSize
    val centerMid = 7.5f * cellSize

    // Top triangle (Green, Player 1)
    val pathGreen = Path().apply {
        moveTo(centerStart, centerStart)
        lineTo(centerEnd, centerStart)
        lineTo(centerMid, centerMid)
        close()
    }
    drawPath(pathGreen, color = theme.playerColors[1].copy(alpha = 0.9f))
    drawPath(pathGreen, color = theme.trackBorder, style = Stroke(width = 2.dp.toPx()))

    // Right triangle (Yellow, Player 2)
    val pathYellow = Path().apply {
        moveTo(centerEnd, centerStart)
        lineTo(centerEnd, centerEnd)
        lineTo(centerMid, centerMid)
        close()
    }
    drawPath(pathYellow, color = theme.playerColors[2].copy(alpha = 0.9f))
    drawPath(pathYellow, color = theme.trackBorder, style = Stroke(width = 2.dp.toPx()))

    // Bottom triangle (Blue, Player 3)
    val pathBlue = Path().apply {
        moveTo(centerEnd, centerEnd)
        lineTo(centerStart, centerEnd)
        lineTo(centerMid, centerMid)
        close()
    }
    drawPath(pathBlue, color = theme.playerColors[3].copy(alpha = 0.9f))
    drawPath(pathBlue, color = theme.trackBorder, style = Stroke(width = 2.dp.toPx()))

    // Left triangle (Red, Player 0)
    val pathRed = Path().apply {
        moveTo(centerStart, centerStart)
        lineTo(centerStart, centerEnd)
        lineTo(centerMid, centerMid)
        close()
    }
    drawPath(pathRed, color = theme.playerColors[0].copy(alpha = 0.9f))
    drawPath(pathRed, color = theme.trackBorder, style = Stroke(width = 2.dp.toPx()))

    // Draw Tokens for 4-Player
    val scale = boardSize / 1000f
    visualTokens.forEach { vt ->
        drawSingleToken(
            center = vt.center * scale,
            radius = vt.radius * scale,
            playerColor = theme.playerColors[vt.playerOwnerId],
            isHighlighted = vt.isHighlighted,
            theme = theme,
            emoji = vt.emoji,
            tokenShapeIndex = vt.tokenShapeIndex
        )
    }
}

private fun DrawScope.draw4Yards(
    cellSize: Float,
    theme: LudoThemeColors,
    state: GameState
) {
    val yardSize = 6 * cellSize

    val yardPositions = listOf(
        Offset(0f, 0f),                         // Red, Player 0
        Offset(9 * cellSize, 0f),                // Green, Player 1
        Offset(9 * cellSize, 9 * cellSize),      // Yellow, Player 2
        Offset(0f, 9 * cellSize)                 // Blue, Player 3
    )

    for (pId in 0..3) {
        val pos = yardPositions[pId]
        val player = state.players.getOrNull(pId)
        val isDisabled = player?.type == PlayerType.DISABLED
        val alpha = if (isDisabled) 0.25f else 1f

        // Yard Card Background
        drawRoundRectCard(
            color = theme.playerColors[pId].copy(alpha = 0.15f * alpha),
            topLeft = pos,
            size = Size(yardSize, yardSize),
            borderColor = theme.playerColors[pId].copy(alpha = 0.8f * alpha),
            borderWidth = 3.dp.toPx()
        )

        // Title text / icon or inner circle
        drawCircle(
            color = theme.background.copy(alpha = alpha),
            radius = cellSize * 2f,
            center = Offset(pos.x + 3 * cellSize, pos.y + 3 * cellSize)
        )
        drawCircle(
            color = theme.playerColors[pId].copy(alpha = 0.4f * alpha),
            radius = cellSize * 1.8f,
            center = Offset(pos.x + 3 * cellSize, pos.y + 3 * cellSize),
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw 4 token base slots inside yard
        val offsets = listOf(
            Offset(1.8f * cellSize, 1.8f * cellSize),
            Offset(4.2f * cellSize, 1.8f * cellSize),
            Offset(1.8f * cellSize, 4.2f * cellSize),
            Offset(4.2f * cellSize, 4.2f * cellSize)
        )

        offsets.forEachIndexed { idx, off ->
            drawCircle(
                color = theme.background.copy(alpha = alpha),
                radius = cellSize * 0.6f,
                center = pos + off
            )
            drawCircle(
                color = theme.playerColors[pId].copy(alpha = 0.7f * alpha),
                radius = cellSize * 0.5f,
                center = pos + off
            )
        }
    }
}

// 6-Player Board Drawing
private fun DrawScope.draw6PlayerBoard(
    boardSize: Float,
    state: GameState,
    visualTokens: List<VisualTokenDrawingData>,
    theme: LudoThemeColors,
    isRolling: Boolean,
    diceValue: Int,
    pulseAlpha: Float,
    pulseScale: Float
) {
    val centerX = boardSize / 2f
    val centerY = boardSize / 2f
    val center = Offset(centerX, centerY)

    // Main track radius
    val trackRadius = boardSize * 0.26f
    val cellRadius = boardSize * 0.021f

    // Draw regular outer hexagon backplate with gradient dark neon styling
    val hexRadius = boardSize * 0.485f
    drawRegularHexagon(
        center = center,
        radius = hexRadius,
        color = Color(0xFF0C0D16), // Premium modern charcoal/dark background
        borderColor = theme.accent.copy(alpha = 0.35f),
        borderWidth = 3.dp.toPx()
    )
    drawRegularHexagon(
        center = center,
        radius = hexRadius - 6.dp.toPx(),
        color = Color.Transparent,
        borderColor = theme.accent.copy(alpha = 0.12f),
        borderWidth = 8.dp.toPx()
    )

    // 1. Draw 6 Yard Cards radially
    val yardRadius = boardSize * 0.38f
    val yardWidth = boardSize * 0.18f

    for (pId in 0..5) {
        val player = state.players.getOrNull(pId)
        val isDisabled = player?.type == PlayerType.DISABLED
        val alpha = if (isDisabled) 0.2f else 1f
        val isActiveTurn = pId == state.currentTurnPlayerIndex && !isDisabled && state.gamePhase == GamePhase.PLAYING

        val angleRad = Math.toRadians((pId * 60.0)).toFloat()
        val xY = centerX + yardRadius * cos(angleRad)
        val yY = centerY + yardRadius * sin(angleRad)
        val yardCenter = Offset(xY, yY)

        val playerColor = theme.playerColors[pId]

        // If active turn, draw beautiful glowing aura pulse
        if (isActiveTurn) {
            for (g in 1..5) {
                drawCircle(
                    color = playerColor.copy(alpha = 0.05f * (6 - g) * pulseAlpha),
                    radius = yardWidth * pulseScale + g * 4.dp.toPx() * pulseScale,
                    center = yardCenter
                )
            }
        }

        // Draw Yard Background with Neon Glow Ring
        for (g in 1..3) {
            drawCircle(
                color = playerColor.copy(alpha = 0.06f * (4 - g) * alpha),
                radius = yardWidth + g * 2.5f.dp.toPx(),
                center = yardCenter
            )
        }

        drawCircle(
            color = Color(0xFF131524).copy(alpha = alpha), // Charcoal metallic center
            radius = yardWidth,
            center = yardCenter
        )

        drawCircle(
            color = playerColor.copy(alpha = 0.85f * alpha),
            radius = yardWidth,
            center = yardCenter,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw 4 slots inside Yard
        val dist = yardWidth * 0.5f
        val slots = listOf(
            Offset(dist * 0.7f, dist * 0.7f),
            Offset(-dist * 0.7f, dist * 0.7f),
            Offset(-dist * 0.7f, -dist * 0.7f),
            Offset(dist * 0.7f, -dist * 0.7f)
        )

        slots.forEach { slot ->
            val slotPos = Offset(xY + slot.x, yY + slot.y)
            drawCircle(
                color = theme.background.copy(alpha = alpha),
                radius = cellRadius * 1.15f,
                center = slotPos
            )
            // inner ring neon
            drawCircle(
                color = playerColor.copy(alpha = 0.8f * alpha),
                radius = cellRadius * 0.95f,
                center = slotPos,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }

    // 2. Draw 60 Main Track Cells as a beautiful connected hexagonal chain
    val loopSize = 60
    val trackPath = androidx.compose.ui.graphics.Path()
    for (i in 0 until loopSize) {
        val angleRad = Math.toRadians((i * (360.0 / loopSize))).toFloat()
        val rHex = getHexagonRadius(angleRad, trackRadius)
        val xC = centerX + rHex * cos(angleRad)
        val yC = centerY + rHex * sin(angleRad)
        if (i == 0) {
            trackPath.moveTo(xC, yC)
        } else {
            trackPath.lineTo(xC, yC)
        }
    }
    trackPath.close()
    
    // Draw the hexagonal track connector ribbon
    drawPath(
        path = trackPath,
        color = theme.trackBorder.copy(alpha = 0.35f),
        style = Stroke(width = cellRadius * 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = trackPath,
        color = Color(0xFF10111A), // Sleek obsidian/dark charcoal lane
        style = Stroke(width = cellRadius * 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    for (i in 0 until loopSize) {
        val angleRad = Math.toRadians((i * (360.0 / loopSize))).toFloat()
        val rHex = getHexagonRadius(angleRad, trackRadius)
        val xC = centerX + rHex * cos(angleRad)
        val yC = centerY + rHex * sin(angleRad)
        val cellCenter = Offset(xC, yC)

        // Determine if safe or starting
        val safeCells6p = setOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        val isSafe = safeCells6p.contains(i)
        val startOwner = getStartCellOwner6p(i)

        var cellColor = Color(0xFF181A26) // Modern dark gray for general cells
        var borderColor = Color(0xFF2C2E3E) // Dark metal rim
        var cellRadiusScale = 0.95f

        if (startOwner != null) {
            cellColor = theme.playerColors[startOwner].copy(alpha = 0.85f)
            borderColor = theme.playerColors[startOwner]
            cellRadiusScale = 1.05f
        } else if (isSafe) {
            cellColor = Color(0xFF1B233A)
            borderColor = theme.accent.copy(alpha = 0.8f)
        }

        // Draw cell backplate as a sleek regular hexagon
        drawRegularHexagon(
            center = cellCenter,
            radius = cellRadius * cellRadiusScale,
            color = cellColor,
            borderColor = borderColor,
            borderWidth = 1.5.dp.toPx()
        )

        // If safe zone but not start, draw a beautiful Neon Shield
        if (isSafe && startOwner == null) {
            drawShieldIcon(xC, yC, cellRadius * 0.72f, theme.accent)
        }
    }

    // 3. Draw 6 Safe Home Paths leading radially to the center (representing the middle safe lanes)
    val homePathRadiusMax = trackRadius * 0.85f
    val homePathRadiusMin = boardSize * 0.082f

    for (pId in 0..5) {
        val player = state.players.getOrNull(pId)
        if (player?.type == PlayerType.DISABLED) continue

        val angleRad = Math.toRadians((pId * 60.0)).toFloat()
        val playerColor = theme.playerColors[pId]

        // Draw 6 steps radially
        for (step in 0..5) {
            val ratio = step / 6f
            val r = homePathRadiusMax - ratio * (homePathRadiusMax - homePathRadiusMin)
            val xC = centerX + r * cos(angleRad)
            val yC = centerY + r * sin(angleRad)
            val stepCenter = Offset(xC, yC)

            // Draw home steps as perfect hexagons for interlocking Hex grid feel
            drawRegularHexagon(
                center = stepCenter,
                radius = cellRadius * 0.9f,
                color = playerColor.copy(alpha = 0.9f),
                borderColor = Color.White.copy(alpha = 0.4f),
                borderWidth = 1.2.dp.toPx()
            )
        }
    }

    // 4. Central Home Triangle / Hexagon split into 6 colored slices
    val centerHexRadius = boardSize * 0.082f
    for (pId in 0..5) {
        val angleStart = Math.toRadians((pId * 60.0 - 30.0)).toFloat()
        val angleEnd = Math.toRadians((pId * 60.0 + 30.0)).toFloat()
        
        val pA = center
        val pB = Offset(centerX + centerHexRadius * cos(angleStart), centerY + centerHexRadius * sin(angleStart))
        val pC = Offset(centerX + centerHexRadius * cos(angleEnd), centerY + centerHexRadius * sin(angleEnd))
        
        val triPath = Path().apply {
            moveTo(pA.x, pA.y)
            lineTo(pB.x, pB.y)
            lineTo(pC.x, pC.y)
            close()
        }
        
        val playerColor = theme.playerColors[pId]
        drawPath(path = triPath, color = playerColor.copy(alpha = 0.85f))
        drawPath(path = triPath, color = playerColor, style = Stroke(width = 1.5.dp.toPx()))
    }

    // Draw outer rim around the central home hexagon
    val outerCenterPath = Path()
    for (i in 0..5) {
        val angle = Math.toRadians((i * 60.0 - 30.0)).toFloat()
        val x = centerX + centerHexRadius * cos(angle)
        val y = centerY + centerHexRadius * sin(angle)
        if (i == 0) outerCenterPath.moveTo(x, y) else outerCenterPath.lineTo(x, y)
    }
    outerCenterPath.close()
    drawPath(path = outerCenterPath, color = theme.accent, style = Stroke(width = 2.dp.toPx()))

    // 5. Draw Spinning 3D Neon Dice inside the Central Home
    val diceSize = boardSize * 0.065f
    val diceCenter = center
    val activePlayerColor = theme.playerColors[state.currentTurnPlayerIndex]

    // If human turn and dice not rolled, draw a glowing pulsating invite ring around center
    val isHumanTurn = state.currentTurnPlayer?.type == PlayerType.HUMAN
    val canRoll = !state.isDiceRolled && isHumanTurn && state.gamePhase == GamePhase.PLAYING
    if (canRoll) {
        drawCircle(
            color = activePlayerColor.copy(alpha = 0.25f * pulseAlpha),
            radius = (diceSize * 0.85f) * pulseScale,
            center = diceCenter,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }

    val rotationDegrees = if (isRolling) (pulseAlpha * 720f) % 360f else 0f
    rotate(degrees = rotationDegrees, pivot = diceCenter) {
        // Shadow offset for 3D look
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = diceCenter - Offset(diceSize / 2f - 2.dp.toPx(), diceSize / 2f - 4.dp.toPx()),
            size = Size(diceSize, diceSize),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Neon Glow aura under dice
        for (g in 1..3) {
            drawRoundRect(
                color = activePlayerColor.copy(alpha = 0.07f * (4 - g)),
                topLeft = diceCenter - Offset(diceSize / 2f + g * 1.5f.dp.toPx(), diceSize / 2f + g * 1.5f.dp.toPx()),
                size = Size(diceSize + g * 3.dp.toPx(), diceSize + g * 3.dp.toPx()),
                cornerRadius = CornerRadius((8 + g * 2).dp.toPx(), (8 + g * 2).dp.toPx())
            )
        }

        // Dice Body
        drawRoundRect(
            color = Color.White,
            topLeft = diceCenter - Offset(diceSize / 2f, diceSize / 2f),
            size = Size(diceSize, diceSize),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(activePlayerColor.copy(alpha = 0.2f), activePlayerColor),
                center = diceCenter,
                radius = diceSize * 0.7f
            ),
            topLeft = diceCenter - Offset(diceSize / 2f, diceSize / 2f),
            size = Size(diceSize, diceSize),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )

        // Dots on dice face
        val dotRadius = diceSize * 0.09f
        val c = 0f
        val offsetDist = diceSize * 0.25f

        fun drawDiceDot(dx: Float, dy: Float) {
            drawCircle(
                color = activePlayerColor,
                radius = dotRadius,
                center = diceCenter + Offset(dx, dy)
            )
            // Shine highlight inside dot for 3D effect
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = dotRadius * 0.4f,
                center = diceCenter + Offset(dx - dotRadius * 0.2f, dy - dotRadius * 0.2f)
            )
        }

        when (diceValue) {
            1 -> {
                drawDiceDot(c, c)
            }
            2 -> {
                drawDiceDot(-offsetDist, -offsetDist)
                drawDiceDot(offsetDist, offsetDist)
            }
            3 -> {
                drawDiceDot(-offsetDist, -offsetDist)
                drawDiceDot(c, c)
                drawDiceDot(offsetDist, offsetDist)
            }
            4 -> {
                drawDiceDot(-offsetDist, -offsetDist)
                drawDiceDot(offsetDist, -offsetDist)
                drawDiceDot(-offsetDist, offsetDist)
                drawDiceDot(offsetDist, offsetDist)
            }
            5 -> {
                drawDiceDot(-offsetDist, -offsetDist)
                drawDiceDot(offsetDist, -offsetDist)
                drawDiceDot(c, c)
                drawDiceDot(-offsetDist, offsetDist)
                drawDiceDot(offsetDist, offsetDist)
            }
            6 -> {
                drawDiceDot(-offsetDist, -offsetDist)
                drawDiceDot(offsetDist, -offsetDist)
                drawDiceDot(-offsetDist, c)
                drawDiceDot(offsetDist, c)
                drawDiceDot(-offsetDist, offsetDist)
                drawDiceDot(offsetDist, offsetDist)
            }
        }
    }

    // Draw Tokens for 6-Player
    val scale = boardSize / 1000f
    visualTokens.forEach { vt ->
        drawSingleToken(
            center = vt.center * scale,
            radius = vt.radius * scale,
            playerColor = theme.playerColors[vt.playerOwnerId],
            isHighlighted = vt.isHighlighted,
            theme = theme,
            emoji = vt.emoji,
            tokenShapeIndex = vt.tokenShapeIndex
        )
    }
}

private fun DrawScope.drawRegularHexagon(
    center: Offset,
    radius: Float,
    color: Color,
    borderColor: Color? = null,
    borderWidth: Float = 0f
) {
    val path = Path()
    for (i in 0..5) {
        val angleRad = Math.toRadians((i * 60.0 - 30.0)).toFloat()
        val x = center.x + radius * cos(angleRad)
        val y = center.y + radius * sin(angleRad)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    drawPath(path = path, color = color)
    if (borderColor != null && borderWidth > 0f) {
        drawPath(path = path, color = borderColor, style = Stroke(width = borderWidth))
    }
}

private fun DrawScope.drawShieldIcon(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path().apply {
        val w = radius * 0.85f
        val h = radius
        moveTo(cx, cy - h / 2f)
        lineTo(cx + w / 2f, cy - h / 3f)
        lineTo(cx + w / 2f, cy + h / 6f)
        quadraticTo(cx + w / 2f, cy + h / 2f, cx, cy + h * 0.65f)
        quadraticTo(cx - w / 2f, cy + h / 2f, cx - w / 2f, cy + h / 6f)
        lineTo(cx - w / 2f, cy - h / 3f)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.2f))
    drawPath(path = path, color = color, style = Stroke(width = 1.5.dp.toPx()))
}

private fun DrawScope.drawSingleToken(
    center: Offset,
    radius: Float,
    playerColor: Color,
    isHighlighted: Boolean,
    theme: LudoThemeColors,
    emoji: String = "",
    tokenShapeIndex: Int = 0
) {
    // 1. Shadow / Underglow
    drawCircle(
        color = playerColor.copy(alpha = 0.28f),
        radius = radius * 1.35f,
        center = center + Offset(0f, radius * 0.15f)
    )

    // 2. Active Selection Pulsing Neon Glow
    if (isHighlighted) {
        drawCircle(
            color = theme.accent.copy(alpha = 0.45f),
            radius = radius * 1.6f,
            center = center
        )
        drawCircle(
            color = theme.accent,
            radius = radius * 1.6f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // 3. Define the base shape path
    val shapePath = Path().apply {
        when (tokenShapeIndex) {
            1 -> { // Regular Hexagon
                val points = (0 until 6).map { i ->
                    val angle = i * PI / 3f
                    Offset(
                        (center.x + radius * cos(angle)).toFloat(),
                        (center.y + radius * sin(angle)).toFloat()
                    )
                }
                moveTo(points[0].x, points[0].y)
                for (i in 1 until 6) {
                    lineTo(points[i].x, points[i].y)
                }
                close()
            }
            2 -> { // Diamond
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            3 -> { // Star
                val spikes = 5
                val rot = PI / 2 * 3
                val step = PI / spikes
                val innerRadius = radius * 0.45f
                val outerRadius = radius
                moveTo(center.x, center.y - outerRadius)
                for (i in 0 until spikes) {
                    val xOuter = (center.x + cos(rot + i * 2 * step) * outerRadius).toFloat()
                    val yOuter = (center.y + sin(rot + i * 2 * step) * outerRadius).toFloat()
                    lineTo(xOuter, yOuter)

                    val xInner = (center.x + cos(rot + (i * 2 + 1) * step) * innerRadius).toFloat()
                    val yInner = (center.y + sin(rot + (i * 2 + 1) * step) * innerRadius).toFloat()
                    lineTo(xInner, yInner)
                }
                close()
            }
            else -> { // Circle
                addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }
        }
    }

    // 4. Draw outer border shape
    drawPath(
        path = shapePath,
        color = Color.White
    )

    // 5. Glossy gradient body inside the shape
    drawPath(
        path = shapePath,
        brush = Brush.radialGradient(
            colors = listOf(playerColor.copy(alpha = 0.45f), playerColor),
            center = center,
            radius = radius
        )
    )

    // 6. Polished highlight ring around the shape
    drawPath(
        path = shapePath,
        color = Color.White.copy(alpha = 0.5f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // 7. Render Custom Emoji Centered perfectly in the piece
    if (emoji.isNotEmpty()) {
        val paint = android.graphics.Paint().apply {
            textSize = radius * 1.05f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val fontMetrics = paint.fontMetrics
        val yOffset = (fontMetrics.descent + fontMetrics.ascent) / 2f
        drawContext.canvas.nativeCanvas.drawText(
            emoji,
            center.x,
            center.y - yOffset,
            paint
        )
    } else {
        // Fallback default inner core dots for a professional standard look
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = radius * 0.42f,
            center = center
        )
        drawCircle(
            color = playerColor,
            radius = radius * 0.28f,
            center = center
        )
    }

    // 8. 3D Specular Highlight (Shiny glass look)
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = radius * 0.18f,
        center = center - Offset(radius * 0.26f, radius * 0.26f)
    )
}

// 4p Coordinates Resolver
private fun get4pTokenCoords(cellSize: Float, playerId: Int, token: Token, state: GameState): Offset {
    if (token.isYard()) {
        val yardPos = get4pYardBase(playerId, cellSize)
        val offsets = listOf(
            Offset(1.8f * cellSize, 1.8f * cellSize),
            Offset(4.2f * cellSize, 1.8f * cellSize),
            Offset(1.8f * cellSize, 4.2f * cellSize),
            Offset(4.2f * cellSize, 4.2f * cellSize)
        )
        return yardPos + offsets[token.id]
    }

    if (token.isHome(GameMode.FOUR_PLAYERS)) {
        // Center is at 7.5 cells
        return Offset(7.5f * cellSize, 7.5f * cellSize)
    }

    if (token.isHomePath(GameMode.FOUR_PLAYERS)) {
        val step = token.cellsTraveled - 51
        val (col, row) = when (playerId) {
            0 -> Pair(1 + step, 7)
            1 -> Pair(7, 1 + step)
            2 -> Pair(13 - step, 7)
            3 -> Pair(7, 13 - step)
            else -> Pair(7, 7)
        }
        return Offset((col + 0.5f) * cellSize, (row + 0.5f) * cellSize)
    }

    // Normal path
    val trackIndex = (state.players[playerId].startCellIndex + token.cellsTraveled) % 52
    val (col, row) = getGridCoords4p(trackIndex)
    return Offset((col + 0.5f) * cellSize, (row + 0.5f) * cellSize)
}

private fun get4pYardBase(playerId: Int, cellSize: Float): Offset {
    return when (playerId) {
        0 -> Offset(0f, 0f)
        1 -> Offset(9 * cellSize, 0f)
        2 -> Offset(9 * cellSize, 9 * cellSize)
        3 -> Offset(0f, 9 * cellSize)
        else -> Offset.Zero
    }
}

// 6p Coordinates Resolver
private fun get6pTokenCoords(boardSize: Float, playerId: Int, token: Token, state: GameState): Offset {
    val centerX = boardSize / 2f
    val centerY = boardSize / 2f

    if (token.isYard()) {
        val yardRadius = boardSize * 0.38f
        val angleRad = Math.toRadians((playerId * 60.0)).toFloat()
        val xY = centerX + yardRadius * cos(angleRad)
        val yY = centerY + yardRadius * sin(angleRad)

        val dist = boardSize * 0.18f * 0.5f
        val offsets = listOf(
            Offset(dist * 0.7f, dist * 0.7f),
            Offset(-dist * 0.7f, dist * 0.7f),
            Offset(-dist * 0.7f, -dist * 0.7f),
            Offset(dist * 0.7f, -dist * 0.7f)
        )
        return Offset(xY + offsets[token.id].x, yY + offsets[token.id].y)
    }

    if (token.isHome(GameMode.SIX_PLAYERS)) {
        return Offset(centerX, centerY)
    }

    if (token.isHomePath(GameMode.SIX_PLAYERS)) {
        val step = token.cellsTraveled - 59
        val homePathRadiusMax = boardSize * 0.26f * 0.85f
        val homePathRadiusMin = boardSize * 0.08f
        val ratio = step / 6f
        val r = homePathRadiusMax - ratio * (homePathRadiusMax - homePathRadiusMin)
        val angleRad = Math.toRadians((playerId * 60.0)).toFloat()
        return Offset(centerX + r * cos(angleRad), centerY + r * sin(angleRad))
    }

    // Track
    val trackIndex = (state.players[playerId].startCellIndex + token.cellsTraveled) % 60
    val angleRad = Math.toRadians((trackIndex * 6.0)).toFloat()
    val r = getHexagonRadius(angleRad, boardSize * 0.26f)
    return Offset(centerX + r * cos(angleRad), centerY + r * sin(angleRad))
}

// Map screen gestures to Ludo token selection
private fun handleBoardTap(
    offset: Offset,
    actualBoardSize: Float,
    visualTokens: List<VisualTokenDrawingData>,
    onTokenClick: (playerId: Int, tokenId: Int) -> Unit,
    onDiceRoll: (() -> Unit)? = null
) {
    // Check if clicking center dice
    val center = Offset(actualBoardSize / 2f, actualBoardSize / 2f)
    if (onDiceRoll != null && (offset - center).getDistance() < actualBoardSize * 0.08f) {
        onDiceRoll()
        return
    }

    if (visualTokens.isEmpty()) return

    val scale = actualBoardSize / 1000f
    var closestToken: VisualTokenDrawingData? = null
    var minDistance = Float.MAX_VALUE

    for (vt in visualTokens) {
        if (!vt.isHighlighted) continue
        val tokenCoords = vt.center * scale
        val dist = (offset - tokenCoords).getDistance()
        // Within visual range of tap (at least 48dp target equivalent)
        if (dist < minDistance && dist < actualBoardSize * 0.13f) {
            minDistance = dist
            closestToken = vt
        }
    }

    closestToken?.let { vt ->
        onTokenClick(vt.playerOwnerId, vt.tokenId)
    }
}

// Utility Canvas Draw Star Function
private fun DrawScope.drawStarIcon(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val spikes = 5
    val rot = PI / 2 * 3
    var x = cx
    var y = cy
    val step = PI / spikes

    val innerRadius = radius * 0.4f
    val outerRadius = radius

    path.moveTo(cx, cy - outerRadius)
    for (i in 0 until spikes) {
        x = (cx + cos(rot + i * 2 * step) * outerRadius).toFloat()
        y = (cy + sin(rot + i * 2 * step) * outerRadius).toFloat()
        path.lineTo(x, y)

        x = (cx + cos(rot + (i * 2 + 1) * step) * innerRadius).toFloat()
        y = (cy + sin(rot + (i * 2 + 1) * step) * innerRadius).toFloat()
        path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

// Standard 15x15 Ludo Grid Track Cell Mappings
fun getGridCoords4p(trackIndex: Int): Pair<Int, Int> {
    return when (trackIndex) {
        0 -> Pair(1, 6)
        1 -> Pair(2, 6)
        2 -> Pair(3, 6)
        3 -> Pair(4, 6)
        4 -> Pair(5, 6)
        5 -> Pair(6, 5)
        6 -> Pair(6, 4)
        7 -> Pair(6, 3)
        8 -> Pair(6, 2)
        9 -> Pair(6, 1)
        10 -> Pair(6, 0)
        11 -> Pair(7, 0)
        12 -> Pair(8, 0)
        13 -> Pair(8, 1)
        14 -> Pair(8, 2)
        15 -> Pair(8, 3)
        16 -> Pair(8, 4)
        17 -> Pair(8, 5)
        18 -> Pair(9, 6)
        19 -> Pair(10, 6)
        20 -> Pair(11, 6)
        21 -> Pair(12, 6)
        22 -> Pair(13, 6)
        23 -> Pair(14, 6)
        24 -> Pair(14, 7)
        25 -> Pair(14, 8)
        26 -> Pair(13, 8)
        27 -> Pair(12, 8)
        28 -> Pair(11, 8)
        29 -> Pair(10, 8)
        30 -> Pair(9, 8)
        31 -> Pair(8, 9)
        32 -> Pair(8, 10)
        33 -> Pair(8, 11)
        34 -> Pair(8, 12)
        35 -> Pair(8, 13)
        36 -> Pair(8, 14)
        37 -> Pair(7, 14)
        38 -> Pair(6, 14)
        39 -> Pair(6, 13)
        40 -> Pair(6, 12)
        41 -> Pair(6, 11)
        42 -> Pair(6, 10)
        43 -> Pair(6, 9)
        44 -> Pair(5, 8)
        45 -> Pair(4, 8)
        46 -> Pair(3, 8)
        47 -> Pair(2, 8)
        48 -> Pair(1, 8)
        49 -> Pair(0, 8)
        50 -> Pair(0, 7)
        51 -> Pair(0, 6)
        else -> Pair(0, 0)
    }
}

// 4p cell queries
private fun isSafeCell4p(col: Int, row: Int): Boolean {
    // Standard safe spots or start spots on 15x15 board
    return (col == 1 && row == 6) || (col == 8 && row == 1) ||
           (col == 13 && row == 8) || (col == 6 && row == 13) ||
           (col == 6 && row == 2) || (col == 12 && row == 6) ||
           (col == 8 && row == 12) || (col == 2 && row == 8)
}

private fun getStartCellOwner4p(col: Int, row: Int): Int? {
    return when {
        col == 1 && row == 6 -> 0  // Red start
        col == 8 && row == 1 -> 1  // Green start
        col == 13 && row == 8 -> 2 // Yellow start
        col == 6 && row == 13 -> 3 // Blue start
        else -> null
    }
}

private fun getHomePathOwner4p(col: Int, row: Int): Int? {
    return when {
        row == 7 && col in 1..5 -> 0  // Red path
        col == 7 && row in 1..5 -> 1  // Green path
        row == 7 && col in 9..13 -> 2 // Yellow path
        col == 7 && row in 9..13 -> 3 // Blue path
        else -> null
    }
}

// 6p cell queries
private fun getStartCellOwner6p(cellIndex: Int): Int? {
    return when (cellIndex) {
        0 -> 0   // Red
        10 -> 1  // Green
        20 -> 2  // Yellow
        30 -> 3  // Blue
        40 -> 4  // Purple
        50 -> 5  // Teal
        else -> null
    }
}

// Helper to draw clean rounded cards with borders in Canvas
private fun DrawScope.drawRoundRectCard(
    color: Color,
    topLeft: Offset,
    size: Size,
    borderColor: Color,
    borderWidth: Float
) {
    val r = size.width * 0.08f
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        style = Stroke(width = borderWidth)
    )
}

// Theme color definitions for LudoBoard
data class LudoThemeColors(
    val background: Color,
    val trackCell: Color,
    val trackBorder: Color,
    val starCell: Color,
    val accent: Color,
    val playerColors: List<Color>,
    val textPrimary: Color,
    val textSecondary: Color
)

fun getThemeColors(mode: ThemeMode): LudoThemeColors {
    return when (mode) {
        ThemeMode.CLASSIC -> LudoThemeColors(
            background = Color(0xFFF8F9FA),
            trackCell = Color(0xFFEDF2F7),
            trackBorder = Color(0xFFCBD5E0),
            starCell = Color(0xFFFEFCBF),
            accent = Color(0xFFE53E3E),
            playerColors = listOf(
                Color(0xFFE53E3E), // Royal Red
                Color(0xFF319795), // Deep Teal / Green
                Color(0xFFDD6B20), // Dark Amber / Yellow
                Color(0xFF3182CE), // Cobalt Blue
                Color(0xFF805AD5), // Amethyst Purple
                Color(0xFF38B2AC)  // Turquoise
            ),
            textPrimary = Color(0xFF1A202C),
            textSecondary = Color(0xFF718096)
        )
        ThemeMode.COSMIC -> LudoThemeColors(
            background = Color(0xFF03050F),
            trackCell = Color(0xFF0B0F28),
            trackBorder = Color(0xFF1F2C61),
            starCell = Color(0xFF131B3D),
            accent = Color(0xFF00FFD1),
            playerColors = listOf(
                Color(0xFFFF2A6D), // Nebula Rose
                Color(0xFF05D9E8), // Cyan Glow
                Color(0xFFF3E500), // Laser Gold
                Color(0xFF01012B), // Deep Space Blue
                Color(0xFF9E00FF), // Cyber Purple
                Color(0xFF00FF87)  // Emerald Aurora
            ),
            textPrimary = Color(0xFFF7FAFC),
            textSecondary = Color(0xFFA0AEC0)
        )
        ThemeMode.NEON -> LudoThemeColors(
            background = Color(0xFF000000),
            trackCell = Color(0xFF0D0D11),
            trackBorder = Color(0xFF22222D),
            starCell = Color(0xFF1B003A),
            accent = Color(0xFFFF007F),
            playerColors = listOf(
                Color(0xFFFF0055), // Radioactive Magenta
                Color(0xFF39FF14), // Poison Green
                Color(0xFFEFFF14), // Toxic Yellow
                Color(0xFF00F0FF), // Electric Cyan
                Color(0xFFCC00FF), // Neon Purple
                Color(0xFF00FF99)  // Laser Mint
            ),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF94A3B8)
        )
        ThemeMode.PASTEL -> LudoThemeColors(
            background = Color(0xFFFAF6EE),
            trackCell = Color(0xFFF0E5D1),
            trackBorder = Color(0xFFDCC8AA),
            starCell = Color(0xFFEFE8DA),
            accent = Color(0xFF8D7B68),
            playerColors = listOf(
                Color(0xFFFFB3BA), // Soft Rose
                Color(0xFFBAFFC9), // Muted Sage
                Color(0xFFFFDFBA), // Warm Apricot
                Color(0xFFBAE1FF), // Baby Blue
                Color(0xFFE8AEFF), // Soft Lilac
                Color(0xFFBFFCC6)  // Muted Emerald
            ),
            textPrimary = Color(0xFF4A3E3D),
            textSecondary = Color(0xFF8A7A78)
        )
        ThemeMode.BENTO -> LudoThemeColors(
            background = Color(0xFFF4F4F6),
            trackCell = Color(0xFFFFFFFF),
            trackBorder = Color(0xFFE2E8F0),
            starCell = Color(0xFFEEF2F6),
            accent = Color(0xFF4F46E5),
            playerColors = listOf(
                Color(0xFFEF4444), // Apple Red
                Color(0xFF10B981), // Emerald Green
                Color(0xFFF59E0B), // Honey Yellow
                Color(0xFF3B82F6), // Ocean Blue
                Color(0xFF8B5CF6), // Indigo Purple
                Color(0xFF14B8A6)  // Teal Green
            ),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF64748B)
        )
    }
}
