
package com.example.cac.data

data class ResumeResponse(
    val success: Boolean,
    val message: String,
    val questions: List<String>
) //서버에서 받는거
