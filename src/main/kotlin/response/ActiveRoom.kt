package response

import kotlinx.serialization.Serializable
import models.RoomState

@Serializable
data class ActiveRoom(
    val id: String,
    val playerCount: Int,
    val roomState: RoomState,
    val players: List<String> // oyuncu isimleri
)