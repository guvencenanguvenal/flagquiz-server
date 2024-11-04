import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import service.ActiveRoomsResponse
import service.GameService
import java.time.Duration
import java.util.*

private val gameService = GameService()

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        get("/") {
            call.respondText("models.Flag Quiz Game Server Running!")
        }

        get("/rooms") {
            val rooms = gameService.getActiveRooms()
            call.respond(ActiveRoomsResponse(rooms))
        }

        webSocket("/game") {
            val playerId = UUID.randomUUID().toString()
            println("New WebSocket connection: $playerId")

            try {
                gameService.registerPlayerSession(playerId, this)

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            // Reconnect mesajını kontrol et
                            val jsonElement = Json.parseToJsonElement(text)
                            if (jsonElement.jsonObject["type"]?.jsonPrimitive?.content == "Reconnect") {
                                val oldPlayerId = jsonElement.jsonObject["playerId"]?.jsonPrimitive?.content
                                if (oldPlayerId != null) {
                                    gameService.handleReconnect(oldPlayerId, this)
                                }
                            } else {
                                gameService.handleMessage(playerId, text)
                            }
                        }

                        is Frame.Close -> {
                            println("WebSocket closed for player $playerId")
                            gameService.handleDisconnect(playerId)
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                println("Error in WebSocket connection: ${e.message}")
                e.printStackTrace()
            } finally {
                println("WebSocket connection terminated for player $playerId")
                gameService.handleDisconnect(playerId)
            }
        }
    }
}