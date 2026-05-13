// AnswerResponse.kt
package com.example.cac.data

import com.google.gson.annotations.SerializedName
data class AnswerResponse(
    val feedback_speed: String?,
    val answer_id: Int,
    val audio_scores: AudioScores?,
    val stt_text: String,
    val filler_word_count: Int,
    @SerializedName("next_tail_question")
    val nextTailQuestion: String?,
    val feedback: FeedbackData
)

data class AudioScores(
    val tempo: Double,
    val speed_feedback: String?,
    val confidence_score: Double,
    val duration_sec: Double
)

data class Feedback(
    val strength: String?,
    val weakness: String?,
    val suggestion: String?,
    val score: Int
)