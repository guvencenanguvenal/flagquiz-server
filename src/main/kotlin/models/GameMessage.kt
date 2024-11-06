package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("type")
sealed class GameMessage {
    @Serializable
    @SerialName("CreateRoom")
    data class CreateRoom(
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("JoinRoom")
    data class JoinRoom(
        val roomId: String,
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("PlayerAnswer")
    data class PlayerAnswer(
        val answer: String
    ) : GameMessage()

    @Serializable
    @SerialName("GameUpdate")
    data class GameUpdate(
        val roomState: RoomState,
        val cursorPosition: Float,
        val timeRemaining: Long? = null,
        val currentQuestion: ClientQuestion? = null
    ) : GameMessage()

    @Serializable
    @SerialName("TimeUpdate")
    data class TimeUpdate(
        val timeRemaining: Long
    ) : GameMessage()

    @Serializable
    @SerialName("TimeUp")
    data class TimeUp(
        val correctAnswer: String
    ) : GameMessage()

    @Serializable
    @SerialName("GameOver")
    data class GameOver(
        val winner: String
    ) : GameMessage()

    @Serializable
    @SerialName("AnswerResult")
    data class AnswerResult(
        val playerName: String,
        val answer: String,
        val correct: Boolean
    ) : GameMessage()

    @Serializable
    @SerialName("PlayerDisconnected")
    data class PlayerDisconnected(
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("PlayerReconnected")
    data class PlayerReconnected(
        val playerName: String
    ) : GameMessage()

    @Serializable
    @SerialName("RoomCreated")
    data class RoomCreated(
        val roomId: String
    ) : GameMessage()

    @Serializable
    @SerialName("JoinRoomResponse")
    data class JoinRoomResponse(
        val roomId: String,
        val success: Boolean
    ) : GameMessage()

    @Serializable
    @SerialName("RoomClosed")
    data class RoomClosed(
        val reason: String
    ) : GameMessage()
}

