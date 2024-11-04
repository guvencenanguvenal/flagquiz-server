package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class Player(
    val id: String,
    val name: String
)

@Serializable
data class GameRoom(
    val id: String,
    val players: MutableList<Player> = mutableListOf(),
    var gameState: GameState = GameState.WAITING,
    var cursorPosition: Float = 0.5f
)

@Serializable
@JsonClassDiscriminator("type")  // Bunu ekledik
sealed class GameMessage {
    @Serializable
    @SerialName("CreateRoom")
    data class CreateRoom(val playerName: String) : GameMessage()

    @Serializable
    @SerialName("JoinRoom")
    data class JoinRoom(val roomId: String, val playerName: String) : GameMessage()

    @Serializable
    @SerialName("PlayerAnswer")
    data class PlayerAnswer(val answer: String) : GameMessage()

    @Serializable
    @SerialName("GameUpdate")
    data class GameUpdate(
        val gameState: GameState,
        val cursorPosition: Float,
        val currentQuestion: Question? = null
    ) : GameMessage()
}

@Serializable
data class Question(
    val flagId: String,
    val options: List<String>,
    val correctAnswer: String
)

@Serializable
enum class GameState {
    WAITING,
    COUNTDOWN,
    PLAYING,
    PAUSED,    // Yeni durum
    ROUND_END,
    FINISHED
}