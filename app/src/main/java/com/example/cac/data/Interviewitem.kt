package com.example.cac.data
import java.io.Serializable

data class InterviewItem(
    val session_id: Int,
    val date: String,
    val meta: String,
    val score: Int,
    val feedback: String? = null,
    val pdf_url: String? = null
) : Serializable