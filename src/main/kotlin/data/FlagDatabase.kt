package data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import models.Flag
import models.Question

object FlagDatabase {
    private val flags: List<Flag> = loadFlags()

    private fun loadFlags(): List<Flag> {
        val inputStream = javaClass.getResourceAsStream("/flags.json")
        return inputStream?.bufferedReader()?.use { reader ->
            Json.decodeFromString<FlagResponse>(reader.readText()).flags
        } ?: emptyList()
    }

    fun getRandomFlags(count: Int): List<Flag> {
        return flags.shuffled().take(count)
    }

    fun getRandomQuestion(): Question {
        val allFlags = flags.shuffled()
        val correctFlag = allFlags.first()
        val options = allFlags.take(4).map { it.name.tr }.shuffled()

        return Question(
            flagId = correctFlag.id,
            options = options,
            correctAnswer = correctFlag.name.tr
        )
    }

    @Serializable
    private data class FlagResponse(
        val flags: List<Flag>
    )
}