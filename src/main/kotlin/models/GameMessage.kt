package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")  // Bunu ekledik
sealed class GameMessage {
    @Serializable
    @SerialName("CreateRoom")
    data class CreateRoom(
        val type: String = "CreateRoom",
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("JoinRoom")
    data class JoinRoom(
        val type: String = "JoinRoom",
        val roomId: String,
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("PlayerAnswer")
    data class PlayerAnswer(
        val type: String = "PlayerAnswer",
        val answer: String
    ) : GameMessage()

    @Serializable
    @SerialName("GameUpdate")
    data class GameUpdate(
        val type: String = "GameUpdate",
        val gameState: GameState,
        val cursorPosition: Float,
        val timeRemaining: Long? = null,
        val currentQuestion: ClientQuestion? = null
    ) : GameMessage()

    @Serializable
    @SerialName("TimeUpdate")
    data class TimeUpdate(
        val type: String = "TimeUpdate",
        val timeRemaining: Long
    ) : GameMessage()

    @Serializable
    @SerialName("TimeUp")
    data class TimeUp(
        val type: String = "TimeUp",
        val correctAnswer: String
    ) : GameMessage()

    @Serializable
    @SerialName("GameOver")
    data class GameOver(
        val type: String = "GameOver",
        val winner: String
    ) : GameMessage()

    @Serializable
    @SerialName("AnswerResult")
    data class AnswerResult(
        val type: String = "AnswerResult",
        val playerName: String,
        val answer: String,
        val correct: Boolean
    ) : GameMessage()

    @Serializable
    @SerialName("PlayerDisconnected")
    data class PlayerDisconnected(
        val type: String = "PlayerDisconnected",
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("PlayerReconnected")
    data class PlayerReconnected(
        val type: String = "PlayerReconnected",
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("RoomCreated")
    data class RoomCreated(
        val type: String = "RoomCreated",
        val roomId: String
    ) : GameMessage()

    @Serializable
    @SerialName("JoinRoomResponse")
    data class JoinRoomResponse(
        val type: String = "JoinRoomResponse",
        val success: Boolean
    ) : GameMessage()

    @Serializable
    @SerialName("RoomClosed")
    data class RoomClosed(
        val type: String = "RoomClosed",
        val reason: String
    ) : GameMessage()
}

