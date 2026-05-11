// AnswerResponse.kt
package com.example.cac.data

import com.google.gson.annotations.SerializedName
data class AnswerResponse(
    val feedback_speed: String?,
    val answer_id: Int,
    val stt_text: String,
    val filler_word_count: Int,
    @SerializedName("next_tail_question")
    val nextTailQuestion: String?,
    val feedback: FeedbackData
)