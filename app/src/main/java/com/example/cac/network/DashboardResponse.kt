package com.example.cac.network

data class DashboardResponse(
    val avg_document_score: Double?,
    val avg_interview_score: Double?,
    val total_count: Int?,
    val score_improvement: Double?,
    val growth_rate: Double?,
    val history: List<DashboardHistory>?,
    val competency_avg: CompetencyAvg?
)

data class DashboardHistory(
    val round: Int?,
    val total_score: Double?,
    val resume_score: Double?,
    val cover_letter_score: Double?,
    val date: String?
)

data class CompetencyAvg(
    val technical: Double?,
    val passion: Double?,
    val communication: Double?,
    val collaboration: Double?,
    val problem_solving: Double?
)