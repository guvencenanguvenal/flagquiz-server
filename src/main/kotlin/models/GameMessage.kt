package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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

