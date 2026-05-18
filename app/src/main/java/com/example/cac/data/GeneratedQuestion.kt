package com.example.cac.data

import com.google.gson.annotations.SerializedName

//data class GeneratedQuestion(
//    @SerializedName("question_id")
//    val id: Int,
//
//    @SerializedName("question_text")
//    val question_text: String,
//
//    val order_num: Int,
//    val session_id: Int?,
//    val question_type: String?,
//    @SerializedName("is_answered") val isAnswered: Boolean
//)



data class GeneratedQuestion(
    @SerializedName("question_id") val id: Int,
    @SerializedName("question_text") val question_text: String,
    @SerializedName("question_type") val question_type: String,
    @SerializedName("order_num") val order_num: Int,
    @SerializedName("is_saved") val is_saved: Boolean,
    @SerializedName("is_answered") val isAnswered: Boolean
)