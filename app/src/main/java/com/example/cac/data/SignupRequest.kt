package com.example.cac.data

data class SignupRequest(
    val user_id: String,
    val email: String,
    val password: String,
    val username: String,
    val major: String,
    val grade: String,
    val target_job: String,
    val experience_level: String
)