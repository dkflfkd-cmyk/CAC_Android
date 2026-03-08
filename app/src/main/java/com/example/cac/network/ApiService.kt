package com.example.cac.network

import android.R
import com.example.cac.data.TokenResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*
import com.example.cac.data.ResumeResponse
import com.example.cac.data.ResumeRequest
import okhttp3.ResponseBody

interface ApiService {

    // 회원가입

    @POST("/auth/signup")
    fun signup(@Body body: com.example.cac.data.SignupRequest): Call<Map<String, Any>>

    // 로그인 (토큰)
    @FormUrlEncoded
    @POST("/auth/token")
    fun tokenByUserId(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): Call<TokenResponse>

    // 내 정보
    @GET("/auth/me")
    fun me(
        @Header("Authorization") bearerToken: String
    ): Call<Map<String, Any>>

    // 1️ 이력서 업로드
    @Multipart
    @POST("/resumes/upload")
    fun uploadResume(
        @Header("Authorization") bearerToken: String,
        @Part file: MultipartBody.Part
    ): Call<Map<String, Any>>

    // 2️ 이력서 분석
    @POST("/resumes/{resume_id}/analyze")
    fun analyzeResumeById(
        @Header("Authorization") bearerToken: String,
        @Path("resume_id") resumeId: String
    ): Call<Map<String, Any>>

    @GET("/resumes/{resume_id}/analysis/public")
    fun getAnalysisPublic(
        @Path("resume_id") resumeId: Int
    ): Call<okhttp3.ResponseBody>

    // (선택) 면접 – 지금은 안 써도 됨

    @POST("/interview/start")
    fun startInterview(@Body request: ResumeRequest): Call<ResumeResponse>

}