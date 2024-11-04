package models

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val flagId: String,
    val options: List<String>,
    val correctAnswer: String
) 