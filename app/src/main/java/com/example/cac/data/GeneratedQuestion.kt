package com.example.cac.data

data class GeneratedQuestion(
    val question_id: Int,
    val question_text: String,
    val question_type: String,
    val order_num: Int,
    val is_saved: Boolean = false
)