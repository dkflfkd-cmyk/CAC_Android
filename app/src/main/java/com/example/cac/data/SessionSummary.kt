// SessionSummary.kt
package com.example.cac.data

data class SessionSummary(
    val total_score: Float,
    val summary: String,
    val strength_summary: String,
    val weakness_summary: String,
    val recommendation: String
)