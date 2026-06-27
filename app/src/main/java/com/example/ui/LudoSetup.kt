package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.R

@Composable
fun LudoSetupScreen(
    currentSettings: GameSettings,
    onStartGame: (GameMode, List<Player>) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var gameMode by remember { mutableStateOf(GameMode.FOUR_PLAYERS) }

    // Hold player types: Human, AI, or Disabled
    var playerTypes by remember {
        mutableStateOf(
            listOf(
                PlayerType.HUMAN, PlayerType.HUMAN, PlayerType.AI, PlayerType.AI, PlayerType.DISABLED, PlayerType.DISABLED
            )
        )
    }

    // AI customized decision-making priorities for each player slot
    var playerAiPriorities by remember {
        mutableStateOf(
            listOf(
                AiPriority.CAPTURE, AiPriority.CAPTURE, AiPriority.ESCAPE, AiPriority.ADVANCE, AiPriority.CAPTURE, AiPriority.CAPTURE
            )
        )
    }

    var playerEmojis by remember {
        mutableStateOf(
            listOf("👑", "🦊", "🔥", "🚀", "👾", "🦄")
        )
    }

    var playerTokenShapes by remember {
        mutableStateOf(
            listOf(0, 0, 0, 0, 0, 0)
        )
    }

    var customPlayerNames by remember {
        mutableStateOf(
            listOf("", "", "", "", "", "")
        )
    }

    val suggestedNames = remember(playerTypes) {
        var humanCount = 0
        var aiCount = 0
        playerTypes.map { type ->
            when (type) {
                PlayerType.HUMAN -> {
                    humanCount++
                    "لاعب $humanCount"
                }
                PlayerType.AI -> {
                    aiCount++
                    "روبوت $aiCount"
                }
                PlayerType.DISABLED -> {
                    "معطل"
                }
            }
        }
    }

    val maxPlayers = if (gameMode == GameMode.FOUR_PLAYERS) 4 else 6

    // When gameMode changes, automatically disable slots 5 & 6 (index 4 & 5) if FOUR_PLAYERS
    LaunchedEffect(gameMode) {
        val updatedTypes = playerTypes.toMutableList()
        if (gameMode == GameMode.FOUR_PLAYERS) {
            updatedTypes[4] = PlayerType.DISABLED
            updatedTypes[5] = PlayerType.DISABLED
        } else {
            if (updatedTypes[4] == PlayerType.DISABLED) updatedTypes[4] = PlayerType.HUMAN
            if (updatedTypes[5] == PlayerType.DISABLED) updatedTypes[5] = PlayerType.HUMAN
        }
        playerTypes = updatedTypes
    }

    val colors = getThemeColors(currentSettings.themeMode)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Banner & Back Button Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToHome
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "العودة للقائمة الرئيسية",
                    tint = colors.accent
                )
            }
            Text(
                text = "إعداد اللاعبين والأدوار 👥",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accent
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cosmic Hero Banner
        Image(
            painter = painterResource(id = R.drawable.img_ludo_hero),
            contentDescription = "Ludo Board Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Choose Game Mode Segmented Buttons
        Text(
            text = "اختر نمط اللعبة:",
            style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary),
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.trackCell, RoundedCornerShape(16.dp))
                .border(1.dp, colors.trackBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (gameMode == GameMode.FOUR_PLAYERS) colors.accent else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { gameMode = GameMode.FOUR_PLAYERS }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "4 لاعبين (كلاسيكي)",
                    fontWeight = FontWeight.Bold,
                    color = if (gameMode == GameMode.FOUR_PLAYERS) Color.White else colors.trackBorder
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (gameMode == GameMode.SIX_PLAYERS) colors.accent else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { gameMode = GameMode.SIX_PLAYERS }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "6 لاعبين (حديث سداسي)",
                    fontWeight = FontWeight.Bold,
                    color = if (gameMode == GameMode.SIX_PLAYERS) Color.White else colors.trackBorder
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Player Setup Slots
        Text(
            text = "تخصيص اللاعبين والأدوار:",
            style = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary),
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Non-nested column layout of player setup cards with smooth scroll
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            (0 until maxPlayers).forEach { index ->
                val pType = playerTypes[index]
                val pName = if (customPlayerNames[index].isNotBlank()) customPlayerNames[index] else suggestedNames[index]
                val pColor = colors.playerColors[index]
                val isDimmed = pType == PlayerType.DISABLED
                val isBento = currentSettings.themeMode == ThemeMode.BENTO

                val borderStroke = if (isDimmed) {
                    BorderStroke(1.dp, colors.trackBorder.copy(alpha = 0.3f))
                } else if (pType == PlayerType.AI) {
                    BorderStroke(2.dp, colors.accent)
                } else {
                    BorderStroke(1.dp, colors.trackBorder)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isDimmed) 0.6f else 1.0f)
                        .shadow(if (isDimmed) 0.dp else 4.dp, RoundedCornerShape(24.dp))
                        .background(
                            if (pType == PlayerType.AI && isBento) Color(0xFFEADDFF) else colors.trackCell,
                            RoundedCornerShape(24.dp)
                        )
                        .border(borderStroke, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Pill
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(pColor)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Custom Player Name Input or Disabled label
                        if (pType == PlayerType.DISABLED) {
                            Text(
                                text = "معطل",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            OutlinedTextField(
                                value = customPlayerNames[index],
                                onValueChange = { newVal ->
                                    val updated = customPlayerNames.toMutableList()
                                    updated[index] = newVal
                                    customPlayerNames = updated
                                },
                                placeholder = {
                                    Text(
                                        text = suggestedNames[index],
                                        fontSize = 14.sp,
                                        color = colors.textSecondary.copy(alpha = 0.6f)
                                    )
                                },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.trackBorder.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .padding(end = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 3-State Toggle Action Button (👤 بشري -> 🤖 روبوت -> 🚫 معطل)
                        Button(
                            onClick = {
                                val updatedTypes = playerTypes.toMutableList()
                                when (pType) {
                                    PlayerType.HUMAN -> {
                                        updatedTypes[index] = PlayerType.AI
                                    }
                                    PlayerType.AI -> {
                                        updatedTypes[index] = PlayerType.DISABLED
                                    }
                                    PlayerType.DISABLED -> {
                                        updatedTypes[index] = PlayerType.HUMAN
                                    }
                                }
                                playerTypes = updatedTypes
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (pType) {
                                    PlayerType.HUMAN -> colors.accent.copy(alpha = 0.15f)
                                    PlayerType.AI -> colors.accent
                                    PlayerType.DISABLED -> colors.trackBorder.copy(alpha = 0.2f)
                                },
                                contentColor = when (pType) {
                                    PlayerType.HUMAN -> colors.accent
                                    PlayerType.AI -> Color.White
                                    PlayerType.DISABLED -> colors.trackBorder
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = when (pType) {
                                    PlayerType.HUMAN -> "👤 بشري"
                                    PlayerType.AI -> "🤖 ذكاء اصطناعي"
                                    PlayerType.DISABLED -> "🚫 معطل"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Customizable AI Priority settings if slot is AI (Robot)
                    if (pType == PlayerType.AI) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = colors.trackBorder.copy(alpha = 0.4f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "تفضيل ذكاء الروبوت في اتخاذ القرارات:🧠",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier.align(Alignment.End)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Horizontal Flow or Row of choice buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activePriority = playerAiPriorities[index]
                            
                            val priorities = listOf(
                                Pair(AiPriority.CAPTURE, "⚔️ طرد خصوم"),
                                Pair(AiPriority.ENTER, "🚪 خروج بالنرد 6"),
                                Pair(AiPriority.ESCAPE, "🏃 هرب من خطر"),
                                Pair(AiPriority.ADVANCE, "🏁 تقديم الأقرب للبيت")
                            )

                            priorities.forEach { (prio, label) ->
                                val isSelected = activePriority == prio
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) colors.accent else colors.background,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            BorderStroke(1.2.dp, if (isSelected) colors.accent else colors.trackBorder.copy(alpha = 0.5f)),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            val updatedPriorities = playerAiPriorities.toMutableList()
                                            updatedPriorities[index] = prio
                                            playerAiPriorities = updatedPriorities
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else colors.trackBorder
                                    )
                                }
                            }
                        }
                    }

                    if (pType != PlayerType.DISABLED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = colors.trackBorder.copy(alpha = 0.3f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Emoji Picker
                        Text(
                            text = "اختر الرمز التعبيري للّاعب: ${playerEmojis[index]}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val availableEmojis = listOf("👑", "🦊", "🔥", "🚀", "👾", "🦄", "🎯", "🎮", "⚽", "🧿", "⚡", "🧙", "🦁", "🐧")
                            availableEmojis.forEach { emojiOption ->
                                val isSelected = playerEmojis[index] == emojiOption
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (isSelected) colors.accent.copy(alpha = 0.25f) else colors.background,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) colors.accent else colors.trackBorder.copy(alpha = 0.4f)
                                            ),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            val updated = playerEmojis.toMutableList()
                                            updated[index] = emojiOption
                                            playerEmojis = updated
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emojiOption, fontSize = 16.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Token Shape Picker
                        val currentShape = playerTokenShapes[index]
                        Text(
                            text = "اختر شكل قطع اللعب للّاعب:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.align(Alignment.End)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val availableShapes = listOf(
                                Pair(0, "🔵 دائري"),
                                Pair(1, "⬢ سداسي"),
                                Pair(2, "♦ معين"),
                                Pair(3, "⭐ نجمة")
                            )
                            availableShapes.forEach { (shapeVal, shapeLabel) ->
                                val isSelected = currentShape == shapeVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) colors.accent else colors.background,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            BorderStroke(1.2.dp, if (isSelected) colors.accent else colors.trackBorder.copy(alpha = 0.4f)),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            val updated = playerTokenShapes.toMutableList()
                                            updated[index] = shapeVal
                                            playerTokenShapes = updated
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shapeLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Validate count of active players
        val activePlayersCount = playerTypes.take(maxPlayers).count { it != PlayerType.DISABLED }
        val canStart = activePlayersCount >= 2

        if (!canStart) {
            Text(
                text = "⚠️ يجب تفعيل لاعبين على الأقل لبدء المباراة!",
                color = colors.playerColors[0],
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Start Game Button
        Button(
            onClick = {
                if (canStart) {
                    val setupPlayers = (0 until maxPlayers).map { i ->
                        val startCell = if (gameMode == GameMode.FOUR_PLAYERS) i * 13 else i * 10
                        val finalName = if (customPlayerNames[i].isNotBlank()) customPlayerNames[i] else suggestedNames[i]
                        Player(
                            id = i,
                            name = finalName,
                            type = playerTypes[i],
                            colorIndex = i,
                            startCellIndex = startCell,
                            aiPriority = playerAiPriorities[i],
                            emoji = playerEmojis[i],
                            tokenShapeIndex = playerTokenShapes[i]
                        )
                    }
                    onStartGame(gameMode, setupPlayers)
                }
            },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(if (canStart) 6.dp else 0.dp, RoundedCornerShape(29.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                disabledContainerColor = colors.trackBorder.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(29.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "ابدأ", tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ابدأ المباراة الآن 🎲",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "المطور ومصمم النظام: mzn 🚀",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.accent.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
