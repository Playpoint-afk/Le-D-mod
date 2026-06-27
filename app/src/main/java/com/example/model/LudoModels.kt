package com.example.model

enum class GameMode {
    FOUR_PLAYERS, SIX_PLAYERS
}

enum class PlayerType {
    HUMAN, AI, DISABLED
}

enum class GamePhase {
    HOME, SETUP, PLAYING, FINISHED
}

enum class ThemeMode {
    CLASSIC, COSMIC, NEON, PASTEL, BENTO
}

data class Token(
    val id: Int,
    val playerOwnerId: Int,
    // Monotonically increasing steps traveled.
    // -1 = In Yard (القاعدة)
    // 0..50 (for 4p) or 0..58 (for 6p) = On Main Track
    // 51..56 (for 4p) or 59..64 (for 6p) = In Home Path
    // 57 (for 4p) or 65 (for 6p) = Reached Home (الوصول)
    val cellsTraveled: Int = -1,
    val isFinished: Boolean = false
) {
    fun isYard() = cellsTraveled == -1
    fun isHomePath(gameMode: GameMode): Boolean {
        val maxTrack = if (gameMode == GameMode.FOUR_PLAYERS) 50 else 58
        val maxFinish = if (gameMode == GameMode.FOUR_PLAYERS) 56 else 64
        return cellsTraveled in (maxTrack + 1)..maxFinish
    }
    fun isHome(gameMode: GameMode): Boolean {
        val homeIndex = if (gameMode == GameMode.FOUR_PLAYERS) 57 else 65
        return cellsTraveled >= homeIndex
    }
}

enum class AiPriority {
    CAPTURE, // prioritize capturing opponent pieces
    ENTER,   // prioritize entering the board with a 6
    ESCAPE,  // prioritize escaping threatened pieces
    ADVANCE  // prioritize advancing pieces closest to home
}

data class Player(
    val id: Int,
    val name: String,
    val type: PlayerType,
    val colorIndex: Int, // 0: Red, 1: Green, 2: Yellow, 3: Blue, 4: Purple, 5: Teal
    val startCellIndex: Int,
    val tokens: List<Token> = List(4) { Token(id = it, playerOwnerId = id) },
    val aiPriority: AiPriority = AiPriority.CAPTURE,
    val emoji: String = "👑",
    val tokenShapeIndex: Int = 0 // 0 = Circle, 1 = Hexagon, 2 = Diamond, 3 = Star
)

data class GameSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val gameSpeedMs: Long = 1000L, // Speed of AI play (delay)
    val themeMode: ThemeMode = ThemeMode.BENTO,
    val targetTokensHome: Int = 1 // Standard is 1 or 2 for faster mobile sessions, customizable!
)

data class GameHistoryItem(
    val date: String,
    val mode: String,
    val winnerName: String,
    val activePlayersCount: Int
)

data class GameState(
    val gameMode: GameMode = GameMode.FOUR_PLAYERS,
    val players: List<Player> = emptyList(),
    val currentTurnPlayerIndex: Int = 0,
    val diceValue: Int = 1,
    val isDiceRolled: Boolean = false,
    val hasMoves: Boolean = false,
    val consecutiveSixes: Int = 0,
    val gamePhase: GamePhase = GamePhase.HOME,
    val winnerId: Int? = null,
    val gameLogs: List<String> = emptyList(),
    val adminPanelOpen: Boolean = false,
    val selectedCheatDice: Int? = null,
    val settings: GameSettings = GameSettings(),
    val statistics: List<GameHistoryItem> = emptyList()
) {
    val activePlayers: List<Player>
        get() = players.filter { it.type != PlayerType.DISABLED }
    
    val currentTurnPlayer: Player?
        get() = if (players.isNotEmpty()) players[currentTurnPlayerIndex] else null
}
