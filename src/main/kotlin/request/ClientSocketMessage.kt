package request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * @author guvencenanguvenal
 */
@Serializable
@JsonClassDiscriminator("type")
sealed class ClientSocketMessage {

    @Serializable
    @SerialName("CreateRoom")
    data object CreateRoom : ClientSocketMessage()

    @Serializable
    @SerialName("JoinRoom")
    data class JoinRoom(
        val roomId: String,
    ) : ClientSocketMessage()

    @Serializable
    @SerialName("PlayerAnswer")
    data class PlayerAnswer(
        val answer: String
    ) : ClientSocketMessage()
}