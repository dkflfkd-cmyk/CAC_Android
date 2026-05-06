package com.example.cac.data

data class AnalysisSummaryResponse(
    val analysis_id: Int,
    val created_at: String?,
    val total_score: Int?,
    val resume_score: Int?,
    val cover_letter_score: Int?
)