package com.example.cac.data

import com.google.gson.annotations.SerializedName

data class SessionResponse(
    @SerializedName("session_id")
    val sessionId: Int
)