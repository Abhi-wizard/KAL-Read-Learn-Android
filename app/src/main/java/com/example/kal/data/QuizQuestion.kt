package com.example.kal.data

/**
 * A simple data class to hold the structure of a single quiz question.
 * This structure MUST match the JSON object created by your Cloud Function.
 *
 * @param question The text of the question.
 * @param type The type of question (e.g., "mcq").
 * @param options A list of 4 string options for the answer.
 * @param correctAnswer The string that exactly matches the correct option.
 */
data class QuizQuestion(
    val question: String = "",
    val type: String = "mcq",
    val options: List<String> = emptyList(),
    val correctAnswer: String = ""
)