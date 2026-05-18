package com.example.cac.data

import com.google.gson.annotations.SerializedName



data class SessionRequest(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("target_job")
    val targetJob: String,

    @SerializedName("question_count")
    val questionCount: Int,

    @SerializedName("question_types")
    val questionTypes: List<String>,

    @SerializedName("resume_id")
    val resumeId: Int? = null,

    @SerializedName("pdf_s3_key")
    val pdfS3Key: String? = null
)