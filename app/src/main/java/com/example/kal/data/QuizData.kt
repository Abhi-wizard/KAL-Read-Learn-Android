package com.example.kal.data

// This must match the JSON format from your Cloud Function
data class QuizData(
    val question: String = "",
    val type: String = "", // 'mcq', 'tf', 'fillup', 'descriptive'
    val options: List<String>? = null, // Only for 'mcq'
    val correctAnswer: String = ""
)

// This will hold the user's answer for checking
data class UserAnswer(
    val question: QuizQuestion,
    val answered: String
)

// --- ADD THIS NEW CLASS ---
// This class wraps the response from your cloud function.
// The name "quiz" must match the JSON key you return.
data class QuizResponse(
    val quiz: List<QuizQuestion>
)
