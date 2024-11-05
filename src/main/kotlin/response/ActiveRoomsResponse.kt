package response

import kotlinx.serialization.Serializable
import service.ActiveRoom

@Serializable
data class ActiveRoomsResponse(
    val rooms: List<ActiveRoom>
)