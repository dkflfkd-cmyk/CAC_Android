
package com.example.cac.network

data class AnswerRequest(val answer: String)

data class ChatResponse(
    val next_question: String?,
    val is_last: Boolean = false
)

data class FeedbackResponse(
    val total_score: Int,
    val competence_scores: Map<String, Int>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val key_points: List<String>
)