package com.example.engine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LudoAudio
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LudoViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val sharedPrefs = context.getSharedPreferences("ludo_game_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Map of safe cells on 52-cell track (4 players)
    val safeCells4p = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // Map of safe cells on 60-cell track (6 players)
    val safeCells6p = setOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)

    init {
        loadGame()
    }

    fun initGame(mode: GameMode, customizedPlayers: List<Player>) {
        val finalPlayers = customizedPlayers.map { p ->
            if (p.type == PlayerType.DISABLED) {
                p.copy(tokens = emptyList())
            } else {
                p.copy(tokens = List(4) { Token(id = it, playerOwnerId = p.id) })
            }
        }

        // Find first active player
        val firstActiveIndex = finalPlayers.indexOfFirst { it.type != PlayerType.DISABLED }
        val finalFirstIndex = if (firstActiveIndex != -1) firstActiveIndex else 0

        _state.value = GameState(
            gameMode = mode,
            players = finalPlayers,
            currentTurnPlayerIndex = finalFirstIndex,
            gamePhase = GamePhase.PLAYING,
            diceValue = 1,
            isDiceRolled = false,
            hasMoves = false,
            consecutiveSixes = 0,
            gameLogs = listOf("🎮 بدأت اللعبة الجديدة! دور اللاعب: ${finalPlayers[finalFirstIndex].name}"),
            winnerId = null,
            settings = _state.value.settings,
            statistics = _state.value.statistics
        )
        saveGame()

        try {
            com.example.notification.LudoNotifications.showNotification(
                context,
                "بدء تحدي جديد! 🎲",
                "تم بدء لعبة جديدة! استعد لرمي النرد والمنافسة"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If the first player is AI, trigger its turn
        triggerAiIfNeeded()
    }

    fun rollDice() {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING || currentState.isDiceRolled) return

        LudoAudio.playDiceRoll(context, currentState.settings.soundEnabled)

        val roll = (1..6).random()
        executeDiceRollResult(roll)
    }

    fun selectCheatDice(value: Int) {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING) return

        LudoAudio.playCheat(context, currentState.settings.soundEnabled)
        _state.value = currentState.copy(isDiceRolled = false)
        executeDiceRollResult(value)
        _state.value = _state.value.copy(adminPanelOpen = false)
        saveGame()
    }

    private fun executeDiceRollResult(roll: Int) {
        val currentState = _state.value
        val currentPlayer = currentState.currentTurnPlayer ?: return

        var newConsecutiveSixes = currentState.consecutiveSixes
        if (roll == 6) {
            newConsecutiveSixes++
        } else {
            newConsecutiveSixes = 0
        }

        val logText = "🎲 ${currentPlayer.name} قام برمي النرد وحصل على الرقم $roll"
        val logs = currentState.gameLogs.toMutableList()
        logs.add(0, logText)

        // Check if Bot rolled 6 twice in a row (Trigger automated cheat)
        val isBotCheatTriggered = currentPlayer.type == PlayerType.AI && newConsecutiveSixes >= 2

        val validMoves = getValidMovesForPlayer(currentPlayer, roll, currentState.gameMode)
        val hasValidMoves = validMoves.isNotEmpty()

        val isHumanAdminOpened = (currentPlayer.type == PlayerType.HUMAN && newConsecutiveSixes >= 2)

        _state.value = currentState.copy(
            diceValue = roll,
            isDiceRolled = true,
            hasMoves = hasValidMoves,
            consecutiveSixes = newConsecutiveSixes,
            gameLogs = logs,
            adminPanelOpen = isHumanAdminOpened
        )

        saveGame()

        try {
            if (roll == 6) {
                com.example.notification.LudoNotifications.showNotification(
                    context,
                    "نرد محظوظ! 🎲🔥",
                    "حظ رائع! حصل اللاعب ${currentPlayer.name} على الرقم 6 وسيلعب مجدداً!"
                )
            }
            if (isHumanAdminOpened) {
                com.example.notification.LudoNotifications.showNotification(
                    context,
                    "اللوحة السرية! 🪄🔮",
                    "تم تفعيل لوحة الإدارة السرية للّاعب ${currentPlayer.name}! تم الكشف عن قوى خارقة!"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isBotCheatTriggered) {
            viewModelScope.launch {
                delay(800)
                executeBotCheat()
            }
        } else if (!hasValidMoves) {
            // No moves, pass turn to next player after a delay
            viewModelScope.launch {
                delay(currentState.settings.gameSpeedMs)
                _state.value.gameLogs.toMutableList().apply {
                    add(0, "🚫 لا توجد حركات متاحة لـ ${currentPlayer.name}. تم نقل الدور.")
                    _state.value = _state.value.copy(gameLogs = this)
                }
                nextTurn()
            }
        } else if (currentPlayer.type == PlayerType.AI) {
            // It's AI turn, make the AI move automatically
            viewModelScope.launch {
                delay(currentState.settings.gameSpeedMs)
                executeAiMove(validMoves)
            }
        }
    }

    private fun executeBotCheat() {
        val currentState = _state.value
        val currentPlayer = currentState.currentTurnPlayer ?: return

        // Bot triggers automated admin option: Sabotage the leading opponent!
        // Find leading opponent: an active human/AI player that isn't us, who has a token closest to Home.
        val leadingTokenAndPlayer = currentState.players
            .filter { it.id != currentPlayer.id && it.type != PlayerType.DISABLED }
            .flatMap { p -> p.tokens.map { t -> Pair(p, t) } }
            .filter { (_, t) -> !t.isFinished && t.cellsTraveled >= 0 }
            .maxByOrNull { (_, t) -> t.cellsTraveled }

        val logs = _state.value.gameLogs.toMutableList()

        if (leadingTokenAndPlayer != null) {
            val (victim, tokenToSabotage) = leadingTokenAndPlayer
            
            // Send back to Yard
            val updatedPlayers = currentState.players.map { p ->
                if (p.id == victim.id) {
                    val updatedTokens = p.tokens.map { t ->
                        if (t.id == tokenToSabotage.id) {
                            t.copy(cellsTraveled = -1) // Back to Yard
                        } else t
                    }
                    p.copy(tokens = updatedTokens)
                } else p
            }

            LudoAudio.playCheat(context, currentState.settings.soundEnabled)
            
            val sabotageLog = "🤖 الروبوت [${currentPlayer.name}] حصل على الرقم 6 مرتين متتاليتين! فُتحت له لوحة الإدارة برمجياً واختار الخيار الأكثر تدميراً: أرسل قطعة [${victim.name}] المتقدمة (التي قطعت ${tokenToSabotage.cellsTraveled} خطوة) إلى السجن فوراً! 💥"
            logs.add(0, sabotageLog)

            try {
                com.example.notification.LudoNotifications.showNotification(
                    context,
                    "🤖 غش الذكاء الاصطناعي!",
                    "الروبوت ${currentPlayer.name} استعمل لوحة الإدارة برمجياً ودمر قطعة ${victim.name}! 💥"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _state.value = _state.value.copy(
                players = updatedPlayers,
                consecutiveSixes = 0, // Reset counter
                gameLogs = logs
            )
            saveGame()
        } else {
            logs.add(0, "🤖 الروبوت [${currentPlayer.name}] حاول الغش، لكن لا توجد قطع منافسة على اللوحة لإرسالها للسجن!")
            _state.value = _state.value.copy(consecutiveSixes = 0, gameLogs = logs)
            saveGame()
        }

        // After bot cheat, AI still has a 6 roll to move!
        val validMoves = getValidMovesForPlayer(currentPlayer, _state.value.diceValue, _state.value.gameMode)
        if (validMoves.isNotEmpty()) {
            viewModelScope.launch {
                delay(_state.value.settings.gameSpeedMs)
                executeAiMove(validMoves)
            }
        } else {
            nextTurn()
        }
    }

    private fun executeAiMove(validMoves: List<Token>) {
        val currentState = _state.value
        val currentPlayer = currentState.currentTurnPlayer ?: return

        // Score moves based on selected user priorities:
        // CAPTURE -> prioritize capturing opponent pieces
        // ENTER -> prioritize entering the board with a 6
        // ESCAPE -> prioritize escaping threatened pieces
        // ADVANCE -> prioritize advancing pieces closest to home
        val scoredMoves = validMoves.map { token ->
            var score = 0
            val currentPos = token.cellsTraveled
            val nextPos = if (token.isYard()) 0 else currentPos + currentState.diceValue

            val isCapture = willMoveCapture(currentPlayer.id, nextPos, currentState)
            val isRelease = token.isYard() && currentState.diceValue == 6
            val isEscaping = isThreatened(currentPlayer.id, token, currentState)

            val captureWeight = if (currentPlayer.aiPriority == AiPriority.CAPTURE) 25000 else 8000
            val releaseWeight = if (currentPlayer.aiPriority == AiPriority.ENTER) 25000 else 5000
            val escapeWeight = if (currentPlayer.aiPriority == AiPriority.ESCAPE) 25000 else 3000
            val advanceWeight = if (currentPlayer.aiPriority == AiPriority.ADVANCE) 800 else 10

            if (isCapture) score += captureWeight
            if (isRelease) score += releaseWeight
            if (isEscaping) score += escapeWeight
            score += (currentPos + 1) * advanceWeight

            Pair(token, score)
        }

        // Select move with highest score
        val bestMove = scoredMoves.maxByOrNull { it.second }?.first ?: validMoves.first()
        moveToken(currentPlayer.id, bestMove.id)
    }

    private fun isThreatened(playerId: Int, token: Token, state: GameState): Boolean {
        if (token.isYard() || token.isFinished || token.isHomePath(state.gameMode)) return false
        val tokenGlobalCellIndex = getGlobalCellIndex(playerId, token.cellsTraveled, state.gameMode) ?: return false

        // A cell is safe if it is in the safe cells list
        val isCellSafe = if (state.gameMode == GameMode.FOUR_PLAYERS) {
            safeCells4p.contains(tokenGlobalCellIndex)
        } else {
            safeCells6p.contains(tokenGlobalCellIndex)
        }
        if (isCellSafe) return false

        // Check if there's any opponent token within 6 cells behind us
        val loopSize = if (state.gameMode == GameMode.FOUR_PLAYERS) 52 else 60
        for (dist in 1..6) {
            val checkCellIndex = (tokenGlobalCellIndex - dist + loopSize) % loopSize
            // Find if any active opponent token is on this cell
            val opponentPresent = state.players.filter { it.id != playerId && it.type != PlayerType.DISABLED }.any { opp ->
                opp.tokens.any { oppToken ->
                    if (!oppToken.isYard() && !oppToken.isFinished && !oppToken.isHomePath(state.gameMode)) {
                        val oppGlobal = getGlobalCellIndex(opp.id, oppToken.cellsTraveled, state.gameMode)
                        oppGlobal == checkCellIndex
                    } else false
                }
            }
            if (opponentPresent) return true
        }
        return false
    }

    private fun willMoveCapture(playerId: Int, nextCellsTraveled: Int, state: GameState): Boolean {
        val nextGlobal = getGlobalCellIndex(playerId, nextCellsTraveled, state.gameMode) ?: return false

        // Can't capture on safe cells
        val isCellSafe = if (state.gameMode == GameMode.FOUR_PLAYERS) {
            safeCells4p.contains(nextGlobal)
        } else {
            safeCells6p.contains(nextGlobal)
        }
        if (isCellSafe) return false

        // Check if any opponent is there
        return state.players.filter { it.id != playerId && it.type != PlayerType.DISABLED }.any { opp ->
            opp.tokens.any { oppToken ->
                if (!oppToken.isYard() && !oppToken.isFinished && !oppToken.isHomePath(state.gameMode)) {
                    val oppGlobal = getGlobalCellIndex(opp.id, oppToken.cellsTraveled, state.gameMode)
                    oppGlobal == nextGlobal
                } else false
            }
        }
    }

    fun moveToken(playerId: Int, tokenId: Int) {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING || !currentState.isDiceRolled) return

        val currentPlayer = currentState.currentTurnPlayer ?: return
        if (currentPlayer.id != playerId) return // Ensure correct turn

        val tokenToMove = currentPlayer.tokens.find { it.id == tokenId } ?: return
        val roll = currentState.diceValue

        // Verify if move is valid
        val isValid = isMoveValid(tokenToMove, roll, currentState.gameMode)
        if (!isValid) return

        // Compute new position
        val newCellsTraveled = if (tokenToMove.isYard()) 0 else tokenToMove.cellsTraveled + roll
        val maxFinish = if (currentState.gameMode == GameMode.FOUR_PLAYERS) 57 else 65
        val isNowFinished = newCellsTraveled >= maxFinish

        // Play moving sounds
        LudoAudio.playPieceMove(context, currentState.settings.soundEnabled)

        // Generate log text
        var logText = "🚶 ${currentPlayer.name} حرك القطعة ${tokenId + 1} إلى الخطوة $newCellsTraveled"

        // Handle Capture!
        var capturedOpponentName = ""
        var updatedPlayers = currentState.players.map { p ->
            if (p.id == currentPlayer.id) {
                // Update our token
                val updatedTokens = p.tokens.map { t ->
                    if (t.id == tokenId) {
                        t.copy(cellsTraveled = newCellsTraveled, isFinished = isNowFinished)
                    } else t
                }
                p.copy(tokens = updatedTokens)
            } else p
        }

        if (!isNowFinished && newCellsTraveled >= 0) {
            val globalCell = getGlobalCellIndex(currentPlayer.id, newCellsTraveled, currentState.gameMode)
            val isSafe = if (currentState.gameMode == GameMode.FOUR_PLAYERS) {
                globalCell != null && safeCells4p.contains(globalCell)
            } else {
                globalCell != null && safeCells6p.contains(globalCell)
            }

            if (globalCell != null && !isSafe) {
                // Check if we captured an opponent
                updatedPlayers = updatedPlayers.map { p ->
                    if (p.id != currentPlayer.id && p.type != PlayerType.DISABLED) {
                        val updatedTokens = p.tokens.map { t ->
                            if (!t.isYard() && !t.isFinished && !t.isHomePath(currentState.gameMode)) {
                                val oppGlobal = getGlobalCellIndex(p.id, t.cellsTraveled, currentState.gameMode)
                                if (oppGlobal == globalCell) {
                                    capturedOpponentName = p.name
                                    t.copy(cellsTraveled = -1) // Send back to yard
                                } else t
                            } else t
                        }
                        p.copy(tokens = updatedTokens)
                    } else p
                }
            }
        }

        val logs = currentState.gameLogs.toMutableList()
        if (capturedOpponentName.isNotEmpty()) {
            LudoAudio.playCapture(context, currentState.settings.soundEnabled)
            logText += " 💥 وأكل قطعة $capturedOpponentName!"
            logs.add(0, logText)
            try {
                com.example.notification.LudoNotifications.showNotification(
                    context,
                    "أكل قطعة! 💥🏃‍♂️",
                    "تراجع للوراء! قام ${currentPlayer.name} بأكل قطعة لـ $capturedOpponentName وإعادتها للقاعدة!"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            logs.add(0, logText)
        }

        if (isNowFinished) {
            logs.add(0, "🎉 وصلت قطعة لـ ${currentPlayer.name} إلى خط النهاية!")
        } else {
            val isEnteringHomePath = !tokenToMove.isHomePath(currentState.gameMode) && newCellsTraveled >= (maxFinish - 6)
            if (isEnteringHomePath) {
                try {
                    com.example.notification.LudoNotifications.showNotification(
                        context,
                        "اقترب النصر! 🏁✨",
                        "خطوات أخيرة! قطعة لـ ${currentPlayer.name} اقتربت جداً من خط النهاية!"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        _state.value = currentState.copy(
            players = updatedPlayers,
            isDiceRolled = false,
            hasMoves = false,
            gameLogs = logs,
            adminPanelOpen = false
        )

        // Check if currentPlayer won
        val won = checkWinner(currentPlayer.id, _state.value)
        if (won) {
            LudoAudio.playVictory(context, _state.value.settings.soundEnabled)
            val winMsg = "🏆 فاز ${currentPlayer.name} بالمرتبة الأولى! تهانينا!"
            val updatedLogs = _state.value.gameLogs.toMutableList().apply { add(0, winMsg) }
            
            try {
                com.example.notification.LudoNotifications.showNotification(
                    context,
                    "🏆 انتصار عظيم! 🏆",
                    "تهانينا! اللاعب ${currentPlayer.name} فاز بالجولة وتوّج بطلاً! 🎉"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Add statistics item
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateStr = formatter.format(Date())
            val activeCount = _state.value.players.count { it.type != PlayerType.DISABLED }
            val newStats = _state.value.statistics.toMutableList().apply {
                add(0, GameHistoryItem(
                    date = dateStr,
                    mode = if (currentState.gameMode == GameMode.FOUR_PLAYERS) "4 لاعبين" else "6 لاعبين",
                    winnerName = currentPlayer.name,
                    activePlayersCount = activeCount
                ))
            }

            _state.value = _state.value.copy(
                gamePhase = GamePhase.FINISHED,
                winnerId = currentPlayer.id,
                gameLogs = updatedLogs,
                statistics = newStats
            )
            saveGame()
        } else {
            // Next Turn! (If player rolled 6 and made a valid move, they get another turn in classic rules,
            // but we can pass turn or keep depending on preference. Let's keep dice 6 bonus turn!)
            if (roll == 6 && !isNowFinished) {
                _state.value.gameLogs.toMutableList().apply {
                    add(0, "🎲 حصل ${currentPlayer.name} على 6 ولذلك يحصل على دور إضافي!")
                    _state.value = _state.value.copy(gameLogs = this, consecutiveSixes = currentState.consecutiveSixes)
                }
                saveGame()
                triggerAiIfNeeded()
            } else {
                nextTurn()
            }
        }
    }

    private fun checkWinner(playerId: Int, state: GameState): Boolean {
        val player = state.players.find { it.id == playerId } ?: return false
        val finishedTokens = player.tokens.count { it.isFinished }
        return finishedTokens >= state.settings.targetTokensHome
    }

    fun skipTurn() {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING) return
        val currentPlayer = currentState.currentTurnPlayer ?: return
        
        _state.value.gameLogs.toMutableList().apply {
            add(0, "⏭️ تم تخطي دور ${currentPlayer.name} يدوياً.")
            _state.value = _state.value.copy(
                gameLogs = this,
                isDiceRolled = false,
                hasMoves = false,
                adminPanelOpen = false
            )
        }
        nextTurn()
    }

    fun forceWin() {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING) return
        val currentPlayer = currentState.currentTurnPlayer ?: return

        val updatedPlayers = currentState.players.map { p ->
            if (p.id == currentPlayer.id) {
                p.copy(tokens = p.tokens.map { it.copy(cellsTraveled = if (currentState.gameMode == GameMode.FOUR_PLAYERS) 57 else 65, isFinished = true) })
            } else p
        }

        LudoAudio.playVictory(context, currentState.settings.soundEnabled)
        val logs = currentState.gameLogs.toMutableList().apply {
            add(0, "🪄 غش إداري: تم فوز ${currentPlayer.name} فورياً!")
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = formatter.format(Date())
        val activeCount = _state.value.players.count { it.type != PlayerType.DISABLED }
        val newStats = currentState.statistics.toMutableList().apply {
            add(0, GameHistoryItem(
                date = dateStr,
                mode = if (currentState.gameMode == GameMode.FOUR_PLAYERS) "4 لاعبين" else "6 لاعبين",
                winnerName = currentPlayer.name,
                activePlayersCount = activeCount
            ))
        }

        _state.value = currentState.copy(
            players = updatedPlayers,
            gamePhase = GamePhase.FINISHED,
            winnerId = currentPlayer.id,
            gameLogs = logs,
            isDiceRolled = false,
            adminPanelOpen = false,
            statistics = newStats
        )
        saveGame()
    }

    fun resetStats() {
        _state.value = _state.value.copy(statistics = emptyList())
        saveGame()
    }

    private fun nextTurn() {
        val currentState = _state.value
        val playersCount = currentState.players.size
        if (playersCount == 0) return

        var nextIndex = currentState.currentTurnPlayerIndex
        // Find next active player
        do {
            nextIndex = (nextIndex + 1) % playersCount
        } while (currentState.players[nextIndex].type == PlayerType.DISABLED && nextIndex != currentState.currentTurnPlayerIndex)

        val nextPlayer = currentState.players[nextIndex]
        val logs = currentState.gameLogs.toMutableList().apply {
            add(0, "🔄 دور اللاعب التالي: ${nextPlayer.name}")
        }

        if (nextPlayer.type == PlayerType.HUMAN && currentState.gamePhase == GamePhase.PLAYING) {
            try {
                com.example.notification.LudoNotifications.showNotification(
                    context,
                    "Le Dé Moderne 🎲",
                    "حان دورك الآن يا ${nextPlayer.name}! قم برمي النرد لتحقيق الفوز. 🚀"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _state.value = currentState.copy(
            currentTurnPlayerIndex = nextIndex,
            isDiceRolled = false,
            hasMoves = false,
            consecutiveSixes = if (currentState.players[nextIndex].id == currentState.currentTurnPlayer?.id) currentState.consecutiveSixes else 0,
            gameLogs = logs
        )
        saveGame()

        triggerAiIfNeeded()
    }

    private fun triggerAiIfNeeded() {
        val currentState = _state.value
        val currentPlayer = currentState.currentTurnPlayer ?: return

        if (currentState.gamePhase == GamePhase.PLAYING && currentPlayer.type == PlayerType.AI && !currentState.isDiceRolled) {
            viewModelScope.launch {
                delay(800) // Small delay before rolling dice
                rollDice()
            }
        }
    }

    // Helper to get valid moves
    fun getValidMovesForPlayer(player: Player, roll: Int, gameMode: GameMode): List<Token> {
        return player.tokens.filter { isMoveValid(it, roll, gameMode) }
    }

    private fun isMoveValid(token: Token, roll: Int, gameMode: GameMode): Boolean {
        if (token.isFinished) return false

        val maxFinish = if (gameMode == GameMode.FOUR_PLAYERS) 57 else 65

        if (token.isYard()) {
            return roll == 6
        }

        // Exact match required to finish Ludo
        return token.cellsTraveled + roll <= maxFinish
    }

    // Converts a player's cellsTraveled step index into a global loop index (0..51 or 0..59)
    fun getGlobalCellIndex(playerId: Int, cellsTraveled: Int, gameMode: GameMode): Int? {
        if (cellsTraveled < 0) return null // Yard
        val maxTrack = if (gameMode == GameMode.FOUR_PLAYERS) 50 else 58
        if (cellsTraveled > maxTrack) return null // Home path

        val loopSize = if (gameMode == GameMode.FOUR_PLAYERS) 52 else 60
        val player = _state.value.players.find { it.id == playerId } ?: return null
        return (player.startCellIndex + cellsTraveled) % loopSize
    }

    // UI and Sound settings controls
    fun toggleSound() {
        val settings = _state.value.settings
        _state.value = _state.value.copy(settings = settings.copy(soundEnabled = !settings.soundEnabled))
        saveGame()
    }

    fun toggleVibration() {
        val settings = _state.value.settings
        _state.value = _state.value.copy(settings = settings.copy(vibrationEnabled = !settings.vibrationEnabled))
        saveGame()
    }

    fun setGameSpeed(speedMs: Long) {
        val settings = _state.value.settings
        _state.value = _state.value.copy(settings = settings.copy(gameSpeedMs = speedMs))
        saveGame()
    }

    fun setThemeMode(theme: ThemeMode) {
        val settings = _state.value.settings
        _state.value = _state.value.copy(settings = settings.copy(themeMode = theme))
        saveGame()
    }

    fun setTargetTokens(count: Int) {
        val settings = _state.value.settings
        _state.value = _state.value.copy(
            settings = settings.copy(targetTokensHome = count),
            adminPanelOpen = false
        )
        saveGame()
    }

    fun setGamePhase(phase: GamePhase) {
        _state.value = _state.value.copy(gamePhase = phase)
        saveGame()
    }

    fun toggleAdminPanel() {
        if (_state.value.adminPanelOpen) {
            _state.value = _state.value.copy(adminPanelOpen = false)
        }
    }

    fun adminCapturePiece(targetPlayerId: Int, tokenId: Int) {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING) return

        val targetPlayer = currentState.players.find { it.id == targetPlayerId } ?: return
        val targetToken = targetPlayer.tokens.find { it.id == tokenId } ?: return

        val updatedPlayers = currentState.players.map { p ->
            if (p.id == targetPlayerId) {
                p.copy(tokens = p.tokens.map { t ->
                    if (t.id == tokenId) {
                        t.copy(cellsTraveled = -1, isFinished = false)
                    } else t
                })
            } else p
        }

        val logMsg = "🪄 غش إداري: تم أكل قطعة ${tokenId + 1} للاعب ${targetPlayer.name} وإعادتها للقاعدة!"
        val logs = currentState.gameLogs.toMutableList().apply { add(0, logMsg) }

        LudoAudio.playCapture(context, currentState.settings.soundEnabled)
        _state.value = currentState.copy(
            players = updatedPlayers,
            gameLogs = logs,
            adminPanelOpen = false
        )
        saveGame()

        try {
            com.example.notification.LudoNotifications.showNotification(
                context,
                "غش إداري! 🪄🔮",
                "تراجع للوراء! تم أكل قطعة للاعب ${targetPlayer.name} وإعادتها للقاعدة بواسطة لوحة الإدارة!"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun adminReleasePiece(playerId: Int, tokenId: Int) {
        val currentState = _state.value
        if (currentState.gamePhase != GamePhase.PLAYING) return

        val player = currentState.players.find { it.id == playerId } ?: return
        val token = player.tokens.find { it.id == tokenId } ?: return
        if (!token.isYard()) return

        val updatedPlayers = currentState.players.map { p ->
            if (p.id == playerId) {
                p.copy(tokens = p.tokens.map { t ->
                    if (t.id == tokenId) {
                        t.copy(cellsTraveled = 0, isFinished = false)
                    } else t
                })
            } else p
        }

        val logMsg = "🪄 غش إداري: تم إخراج قطعة ${tokenId + 1} للاعب ${player.name} من القاعدة إلى الملعب!"
        val logs = currentState.gameLogs.toMutableList().apply { add(0, logMsg) }

        LudoAudio.playReleaseYard(context, currentState.settings.soundEnabled)
        _state.value = currentState.copy(
            players = updatedPlayers,
            gameLogs = logs,
            adminPanelOpen = false
        )
        saveGame()
    }

    fun resetToSetup() {
        _state.value = _state.value.copy(
            gamePhase = GamePhase.HOME,
            isDiceRolled = false,
            hasMoves = false,
            consecutiveSixes = 0,
            winnerId = null,
            adminPanelOpen = false
        )
        saveGame()
    }

    // Persistence logic (Moshi + SharedPreferences)
    private fun saveGame() {
        try {
            val jsonAdapter = moshi.adapter(GameState::class.java)
            // Strip out large cyclic fields if any, or just save
            val jsonStr = jsonAdapter.toJson(_state.value)
            sharedPrefs.edit().putString("saved_game_state", jsonStr).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadGame() {
        try {
            val jsonStr = sharedPrefs.getString("saved_game_state", null)
            if (jsonStr != null) {
                val jsonAdapter = moshi.adapter(GameState::class.java)
                val loaded = jsonAdapter.fromJson(jsonStr)
                if (loaded != null) {
                    _state.value = loaded
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
