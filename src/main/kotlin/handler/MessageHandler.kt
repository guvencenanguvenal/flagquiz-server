package handler

import kotlinx.serialization.json.Json
import models.GameMessage
import service.RoomManagerService
import service.SessionManagerService

/**
 * @author guvencenanguvenal
 */
class MessageHandler private constructor() {
    companion object {

        val INSTANCE: MessageHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { MessageHandler() }
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun handleMessage(playerId: String, message: String) {
        try {
            when (val gameMessage = json.decodeFromString<GameMessage>(message)) {
                is GameMessage.CreateRoom -> {
                    val roomId = RoomManagerService.INSTANCE.createRoom(playerId, gameMessage.playerName)
                    val response = GameMessage.RoomCreated(
                        roomId = roomId
                    )
                    SessionManagerService.INSTANCE.broadcastToPlayers(mutableListOf(playerId), response)
                }

                is GameMessage.JoinRoom -> {
                    val success = RoomManagerService.INSTANCE.joinRoom(playerId, gameMessage.roomId, gameMessage.playerName)
                    val response = GameMessage.JoinRoomResponse(
                        gameMessage.roomId,
                        success = success
                    )
                    SessionManagerService.INSTANCE.broadcastToPlayers(mutableListOf(playerId), response)
                    if (success) {
                        RoomManagerService.INSTANCE.startGame(gameMessage.roomId)
                    }
                }

                is GameMessage.PlayerAnswer -> {
                    val roomId = RoomManagerService.INSTANCE.getRoomIdFromPlayerId(playerId)
                    RoomManagerService.INSTANCE.playerAnswered(roomId, playerId, gameMessage.answer)
                }

                else -> {
                    println("Unexpected message type received: ${gameMessage::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            println("Error processing message: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun handleDisconnect(playerId: String) {
        RoomManagerService.INSTANCE.playerDisconnected(playerId)
        SessionManagerService.INSTANCE.removePlayerSession(playerId)
    }
}