package models

import kotlinx.serialization.Serializable

/**
 * @author guvencenanguvenal
 */
@Serializable
abstract class Game (
    val id: String,
    val players: MutableList<Player> = mutableListOf(),
    var currentQuestion: Question? = null
) {
    abstract fun nextQuestion() : Question
    abstract fun processAnswer(answeredPlayerId: String?, answer: String?)

    abstract fun getRoundTime() : Long

    abstract fun maxPlayerCount() : Int
}