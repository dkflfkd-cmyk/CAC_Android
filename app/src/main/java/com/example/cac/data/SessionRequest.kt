package com.example.cac.data

import com.google.gson.annotations.SerializedName

data class SessionRequest(
    @SerializedName("user_id")
    val user_id: String,

    @SerializedName("target_job")
    val target_job: String,

    @SerializedName("question_count")
    val question_count: Int,

    @SerializedName("question_types")
    val question_types: List<String>,

    @SerializedName("analysis_id")
    val analysis_id: Int? = null,

    @SerializedName("pdf_s3_key")
    val pdf_s3_key: String? = null
)