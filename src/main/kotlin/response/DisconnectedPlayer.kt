package response

import kotlinx.serialization.Serializable

@Serializable
data class DisconnectedPlayer(
    val playerId: String,
    val playerName: String,
    val roomId: String,
    val disconnectTime: Long = System.currentTimeMillis()
)