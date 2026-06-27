package com.example.ui

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.engine.LudoViewModel
import com.example.R

@Composable
fun LudoHomeScreen(
    viewModel: LudoViewModel,
    state: GameState,
    modifier: Modifier = Modifier
) {
    val colors = getThemeColors(state.settings.themeMode)
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Pulsing animation for primary buttons
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // --- 1. Modern Geometric Header ---
            Text(
                text = "Le Dé Moderne",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(
                        colors = listOf(colors.accent, colors.playerColors[3], colors.playerColors[1])
                    )
                ),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "لعبة لودو النرد الحديث 🎲",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Developer Signature Badge - mzn
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(colors.accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.5.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "برمجة وتطوير mzn ✨",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful Hero Board Image Frame with Neon Glow Outline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp))
                    .background(colors.background)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ludo_hero),
                    contentDescription = "Ludo Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .border(2.5.dp, colors.accent.copy(alpha = 0.75f), RoundedCornerShape(32.dp))
                )
                // Subtly colored overlay card for depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            ),
                            RoundedCornerShape(32.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // --- 2. Active Session Restore (Conditional) ---
            val hasActiveGame = state.players.any { it.tokens.any { t -> !t.isYard() && !t.isFinished } }
            if (hasActiveGame) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clickable { viewModel.setGamePhase(GamePhase.PLAYING) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.accent.copy(alpha = 0.15f)),
                    border = BorderStroke(1.5.dp, colors.accent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "استمرار",
                            tint = colors.accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            Text(
                                text = "مواصلة الجولة الحالية 🔄",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "اضغط للرجوع للملعب واستئناف تقدمك فورا",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // --- 3. Pristine Minimalist Action Menu ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // START NEW GAME BUTTON
                Button(
                    onClick = { viewModel.setGamePhase(GamePhase.SETUP) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(8.dp, RoundedCornerShape(30.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "لعبة جديدة", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بدء لعبة جديدة 🎲",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // SETTINGS BUTTON
                OutlinedButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.accent
                    ),
                    border = BorderStroke(2.dp, colors.accent.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(29.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات", modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الإعدادات والخيارات ⚙️",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                // EXIT APP BUTTON
                Button(
                    onClick = { (context as? Activity)?.finish() },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.trackCell,
                        contentColor = colors.textSecondary
                    ),
                    shape = RoundedCornerShape(27.dp),
                    border = BorderStroke(1.dp, colors.trackBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "خروج", modifier = Modifier.size(20.dp), tint = colors.textSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الخروج من اللعبة 🚪",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Subdued Credits Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Le Dé Moderne © 2026",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "تطوير وبرمجة mzn 💎",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Elegant Settings Modal Dialog (Contains all stats, themes, rules and triggers to stay perfectly uncluttered) ---
    if (showSettingsDialog) {
        Dialog(
            onDismissRequest = { showSettingsDialog = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, colors.accent.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
                color = colors.background,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.End
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSettingsDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = colors.textSecondary)
                        }
                        Text(
                            text = "لوحة الإعدادات والسمات ⚙️",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accent
                            ),
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section 1: Themes (Completely redesigned with high-contrast text visibility)
                    Text(
                        text = "اختر السمة والظهر (Theme):🎨",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Vertical Themes Scroll
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val themeModes = listOf(
                            ThemeMode.CLASSIC,
                            ThemeMode.COSMIC,
                            ThemeMode.NEON,
                            ThemeMode.PASTEL,
                            ThemeMode.BENTO
                        )
                        themeModes.forEach { t ->
                            val isSelected = state.settings.themeMode == t
                            val tColors = getThemeColors(t)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.trackCell)
                                    .border(
                                        BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.accent else colors.trackBorder),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setThemeMode(t) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Mini color preview indicators
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(tColors.accent))
                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(tColors.playerColors[3]))
                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(tColors.playerColors[1]))
                                }

                                // Right: Text Label
                                Text(
                                    text = when (t) {
                                        ThemeMode.CLASSIC -> "كلاسيكي ريترو 🏛️"
                                        ThemeMode.COSMIC -> "فضاء كوني مظلم 🌌"
                                        ThemeMode.NEON -> "ألوان النيون البارزة ⚡"
                                        ThemeMode.PASTEL -> "هادئ كستنائي ناعم 🌸"
                                        ThemeMode.BENTO -> "مربعات بينتو الحديثة 🍱"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 2: Audio & Vibration Switches
                    Text(
                        text = "خيارات الصوت والاهتزاز:🔊",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Sound Toggle Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.trackCell)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = state.settings.soundEnabled,
                            onCheckedChange = { viewModel.toggleSound() },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("المؤثرات الصوتية والقرع", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text("أصوات واقعية لرمي النرد والطرد وحركات النصر", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vibration Toggle Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.trackCell)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = state.settings.vibrationEnabled,
                            onCheckedChange = { viewModel.toggleVibration() },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent, checkedTrackColor = colors.accent.copy(alpha = 0.4f))
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("اهتزاز تفاعلي هابتيك", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            Text("نبضات اهتزاز مع طرد القطع والوصول لخط النهاية", fontSize = 10.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 3: Detailed statistics
                    Text(
                        text = "سجل إحصائيات المباريات:📊",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.trackCell)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "إجمالي الجولات الملعوبة: ${state.statistics.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        
                        val lastWinner = state.statistics.lastOrNull()?.winnerName ?: "لا يوجد جولات مسبقة"
                        Text(
                            text = "آخر لاعب فاز باللقب: $lastWinner",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )

                        Text(
                            text = "وضع تخزين البيانات: تلقائي وحفظ سحابي محلي 💾",
                            fontSize = 10.sp,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 4: Rules of Play (Subtle Collapsible Item)
                    var rulesExpanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, colors.trackBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .background(colors.trackCell)
                            .clickable { rulesExpanded = !rulesExpanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (rulesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = "قوانين",
                                tint = colors.accent
                            )
                            Text(
                                text = "قوانين وطريقة اللعب الكونية 📜",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        if (rulesExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = colors.trackBorder.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "• الخروج للعب: رمي الرقم 6 يحرر قطعة من القاعدة.\n" +
                                        "• رمية إضافية: يعطيك الرقم 6 رمية أخرى للنرد.\n" +
                                        "• الطرد (Capture): الهبوط على قطعة خصم يعيدها للساحة فورا!\n" +
                                        "• مربعات الأمان: النجوم هي خلايا آمنة ضد طرد القطع.\n" +
                                        "• لوحة الإدارة السحرية: تظهر تلقائياً عندما تحصل على الرقم 6 مرتين متتاليتين في رمياتك! يمكنك الاختيار من خيارات الخوارق ثم تختفي.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth(),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Close Settings Button
                    Button(
                        onClick = { showSettingsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "تأكيد وحفظ التغييرات ✔️",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
