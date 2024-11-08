package models

import kotlinx.serialization.Serializable

@Serializable
enum class GameState {
    WAITING,
    COUNTDOWN,
    PLAYING,
    PAUSED,    // Yeni durum
    ROUND_END,
    FINISHED
}