// QuestionResult.kt
package com.example.cac.data

data class QuestionResult(
    val question_id: Int,
    val question_text: String,
    val question_type: String,
    val order_num: Int,
    val is_saved: Boolean,
    val answer: AnswerSummary?,
    val feedback: FeedbackData?
)

data class AnswerSummary(
    val answer_id: Int?,
    val stt_text: String?
)