package models

import kotlinx.serialization.Serializable

@Serializable
data class GameRoom(
    val id: String,
    val players: MutableList<Player> = mutableListOf(),
    var gameState: GameState = GameState.WAITING,
    var cursorPosition: Float = 0.5f
)