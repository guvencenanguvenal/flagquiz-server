package models

import kotlinx.serialization.Serializable

// Server tarafında kullanılacak tam soru modeli
@Serializable
data class Question(
    val flagId: String,
    val flagUrl: String,
    val options: List<String>,
    val correctAnswer: String
)

// Client'a gönderilecek güvenli versiyon
@Serializable
data class ClientQuestion(
    val flagUrl: String,
    val options: List<String>
)

// Extension function for easy conversion
fun Question.toClientQuestion() = ClientQuestion(
    flagUrl = flagUrl,
    options = options
) 