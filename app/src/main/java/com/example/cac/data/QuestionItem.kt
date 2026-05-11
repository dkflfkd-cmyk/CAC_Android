package com.example.cac.data

import com.google.gson.annotations.SerializedName
data class QuestionItem(

    @SerializedName("id")
    val id: Int,
    @SerializedName("question_text")
    val question: String,


    val number: Int

)