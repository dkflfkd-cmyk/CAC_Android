// AnswerResponse.kt
package com.example.cac.data

data class AnswerResponse(
    val answer_id: Int,
    val stt_text: String,
    val filler_word_count: Int,
    val feedback: FeedbackData
)