package service

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import response.ActiveRoom
import response.DisconnectedPlayer
import response.ServerSocketMessage
import java.util.*

/**
 * @author guvencenanguvenal
 */
class RoomManagerService private constructor() {
    companion object {

        val INSTANCE: RoomManagerService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { RoomManagerService() }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val gameScope = CoroutineScope(Dispatchers.Default + Job())

    private val rooms = Collections.synchronizedMap(mutableMapOf<String, GameRoom>())

    private val playerToRoom = Collections.synchronizedMap(mutableMapOf<String, String>())

    private val disconnectedPlayers = Collections.synchronizedMap(mutableMapOf<String, DisconnectedPlayer>())

    fun getRoomIdFromPlayerId(playerId: String): String {
        return playerToRoom[playerId]!!
    }

    fun createRoom(playerId: String): String {
        val roomId = UUID.randomUUID().toString()
        val room = GameRoom(roomId)
        val player = PlayerManagerService.INSTANCE.getPlayer(playerId) ?: return roomId //TODO: error message atilmali
        room.players.add(player)
        rooms[roomId] = room
        playerToRoom[playerId] = roomId
        println("Room $roomId created by player $playerId")
        return roomId
    }

    suspend fun joinRoom(playerId: String, roomId: String): Boolean {
        val player = PlayerManagerService.INSTANCE.getPlayer(playerId) ?: return false
        val room = rooms[roomId] ?: return false
        //TODO odaya istedigi kadar kisi katilabilecek
        if (room.players.size >= 2) return false

        room.players.add(player)
        playerToRoom[playerId] = roomId
        println("Player $playerId joined room $roomId")
        broadcastRoomState(roomId)
        return true
    }

    private fun cleanupRoom(room: GameRoom) {
        // Odadaki oyunculara bildir
        room.players.forEach { player ->
            SessionManagerService.INSTANCE.getPlayerSession(player.id)?.let { session ->
                CoroutineScope(Dispatchers.IO).launch {
                    val message = ServerSocketMessage.RoomClosed(reason = "Player disconnected for too long")
                    session.send(Frame.Text(json.encodeToString(message)))
                }
            }
            // Oyuncu verilerini temizle
            SessionManagerService.INSTANCE.removePlayerSession(player.id)
            disconnectedPlayers.remove(player.id)
        }
        // Oda verilerini temizle
        room.rounds.last().timer?.cancel()
        rooms.remove(room.id)
    }

    suspend fun startGame(roomId: String) {
        val room = rooms[roomId] ?: return
        //default resistanceGame start
        val game = ResistanceGame(roomId, room.players)
        room.game = game

        println("Starting game for room $roomId with ${room.players.size} players")
        if (room.players.size != room.game!!.maxPlayerCount()) return

        gameScope.launch {
            println("Starting countdown for room $roomId")
            // Geri sayım başlat
            room.roomState = RoomState.COUNTDOWN
            broadcastRoomState(roomId)

            println("Waiting 3 seconds...")
            // 3 saniye bekle
            delay(3000)

            println("Starting actual game for room $roomId")
            // Oyunu başlat
            room.roomState = RoomState.PLAYING
            nextQuestion(room)
        }
    }

    private suspend fun broadcastRoomState(roomId: String) {
        println("Broadcasting game state for room $roomId")
        val room = rooms[roomId] ?: return

        val gameUpdate = ServerSocketMessage.RoomUpdate(
            players = room.players,
            state = room.roomState,
            cursorPosition = room.cursorPosition,
            currentQuestion = room.game?.currentQuestion?.toClientQuestion()
        )
        broadcastToRoom(roomId, gameUpdate)
    }

    private suspend fun nextQuestion(room: GameRoom) {
        val question = room.game!!.nextQuestion()
        //TODO: gameleri yoneten bir yapi kurulmali
        val resistanceGame = room.game as ResistanceGame?

        val gameUpdate = ServerSocketMessage.RoomUpdate(
            players = room.players,
            state = RoomState.PLAYING,
            cursorPosition = resistanceGame?.cursorPosition ?: 0.5f,
            timeRemaining = room.game!!.getRoundTime(),
            currentQuestion = question.toClientQuestion()
        )

        broadcastToRoom(room.id, gameUpdate)
        startRound(room.id)
    }

    private fun startRound(roomId: String) {
        val room = rooms[roomId]!!
        val roundNumber = room.rounds.size + 1
        room.rounds.add(Round(roundNumber))
        room.rounds.last().timer = CoroutineScope(Dispatchers.Default).launch {
            try {
                for (timeLeft in room.game!!.getRoundTime() - 1 downTo 1) {
                    delay(1000)
                    val timeUpdate = ServerSocketMessage.TimeUpdate(remaining = timeLeft)
                    broadcastToRoom(roomId, timeUpdate)
                }
                delay(1000)
                // Süre doldu
                room.rounds.last().answer = null
                endRound(roomId)
                nextRound(room)
            } catch (e: CancellationException) {
                // Timer iptal edildi
            }
        }
    }

    private fun nextRound(room: GameRoom) {
        val roundNumber = room.rounds.size + 1
        room.rounds.add(Round(roundNumber))
    }

    private suspend fun endRound(roomId: String) {
        val room = rooms[roomId] ?: return
        val question = room.game!!.currentQuestion ?: return
        val answer = room.rounds.last().answer
        val answeredPlayerId = room.rounds.last().answeredPlayer?.id

        room.rounds.last().timer?.cancel()

        // Süre doldu mesajı
        val timeUpMessage = ServerSocketMessage.TimeUp(correctAnswer = question.correctAnswer)
        broadcastToRoom(roomId, timeUpMessage)

        room.game?.processAnswer(answeredPlayerId, answer)

        if (room.cursorPosition <= 0f || room.cursorPosition >= 1f) {
            room.roomState = RoomState.FINISHED
            val gameOverMessage = ServerSocketMessage.GameOver(winnerPlayerId = room.rounds.last().answeredPlayer?.id!!)
            broadcastToRoom(roomId, gameOverMessage)

            // Odayı temizle
            delay(5000)
            cleanupRoom(room)
        } else {
            // Yeni soruya geç
            delay(3000)
            nextQuestion(room)
        }
    }

    private suspend fun broadcastToRoom(roomId: String, message: ServerSocketMessage) {
        println("Broadcasting message to room $roomId: $message")
        val room = rooms[roomId] ?: return
        val playerIds = room.players.map(Player::id).toMutableList()
        SessionManagerService.INSTANCE.broadcastToPlayers(playerIds, message)
    }

    fun getActiveRooms(): List<ActiveRoom> {
        return rooms.map { (id, room) ->
            ActiveRoom(
                id = id,
                playerCount = room.players.size,
                roomState = room.roomState,
                players = room.players.map { it.name }
            )
        }
    }

    suspend fun playerAnswered(roomId: String, playerId: String, answer: String) {
        val room = rooms[roomId] ?: return
        val question = room.game!!.currentQuestion ?: return
        val player = room.players.find { it.id == playerId } ?: return
        room.rounds.last().answer = answer
        room.rounds.last().answeredPlayer = player

        // Cevap sonucunu bildir
        val answerResult = ServerSocketMessage.AnswerResult(
            playerId = player.id,
            answer = answer,
            correct = answer == question.correctAnswer
        )

        broadcastToRoom(roomId, answerResult)

        // Doğru cevap verildiyse eli hemen sonlandır
        if (answer == question.correctAnswer) {
            room.rounds.last().timer?.cancel()
            endRound(roomId)
        }
    }

    suspend fun handleReconnect(playerId: String, session: DefaultWebSocketSession): Boolean {
        /*val disconnectedPlayer = disconnectedPlayers[playerId] ?: return false
        val room = rooms[disconnectedPlayer.roomId] ?: return false

        // Oyuncuyu yeniden bağla
        SessionManagerService.INSTANCE.addPlayerToSession(playerId, session)
        playerToRoom[playerId] = disconnectedPlayer.roomId
        disconnectedPlayers.remove(playerId)
        // Diğer oyuncuya bildir
        val reconnectMessage = ServerMessage.ConnectionState(
            type = GameMessage.ConnectionStateType.RECONNECT_SUCCESS,
            playerId = playerId,
            playerName = disconnectedPlayer.playerName
        )
        SessionManagerService.INSTANCE.broadcastToPlayers(room.players.filter { it.id != playerId }.map(Player::id).toMutableList(), reconnectMessage)
        broadcastRoomState(room.id)

        // Oyunu devam ettir
        if (room.roomState == RoomState.PAUSED) {
            room.roomState = RoomState.PLAYING
            nextQuestion(room)
        }*/

        return true
    }

    suspend fun playerDisconnected(playerId: String) {
        val roomId = rooms.entries.find { entry ->
            entry.value.players.any { it.id == playerId }
        }?.key

        if (roomId != null) {
            val room = rooms[roomId]
            if (room != null) {
                val player = room.players.find { it.id == playerId }
                if (player != null) {
                    // Disconnected players listesine ekle
                    disconnectedPlayers[playerId] = DisconnectedPlayer(
                        playerId = playerId,
                        playerName = player.name,
                        roomId = roomId
                    )

                    // Diğer oyuncuya bildir
                    val disconnectMessage = ServerSocketMessage.PlayerDisconnected(playerId = player.id, playerName = player.name)
                    SessionManagerService.INSTANCE.broadcastToPlayers(
                        room.players.filter { it.id != playerId }.map(Player::id).toMutableList(),
                        disconnectMessage)

                    // Oyunu duraklatmak için GameState'i gncelle
                    room.roomState = RoomState.PAUSED
                    room.rounds.last().timer?.cancel() // Timer'ı durdur

                    // 30 saniye bekle ve oyuncu geri bağlanmazsa odayı temizle
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(30000)
                        if (disconnectedPlayers.containsKey(playerId)) {
                            println("Player $playerId did not reconnect within 30 seconds, cleaning up room $roomId")
                            cleanupRoom(room)
                        }
                    }
                }
            }
        }
    }


}