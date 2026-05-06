package com.example.cac.network

import android.R
import com.example.cac.data.TokenResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*
import com.example.cac.data.ResumeResponse
import com.example.cac.data.ResumeRequest
import okhttp3.ResponseBody
import com.example.cac.data.GeneratedQuestion
import com.example.cac.data.AnswerResponse
import com.example.cac.data.SessionRequest
import com.example.cac.data.SessionResponse
import okhttp3.RequestBody

interface ApiService {

    // 회원가입

    @POST("auth/signup")
    fun signup(@Body body: com.example.cac.data.SignupRequest): Call<Map<String, Any>>

    // 로그인 (토큰)
    @FormUrlEncoded
    @POST("auth/token")
    fun tokenByUserId(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = "password"
    ): Call<TokenResponse>

    // 내 정보
    @GET("auth/me")
    fun me(
        @Header("Authorization") bearerToken: String
    ): Call<Map<String, Any>>

    // 1️ 이력서 업로드
    @Multipart
    @POST("resumes/upload")
    fun uploadResume(
        @Header("Authorization") bearerToken: String,
        @Part file: MultipartBody.Part
    ): Call<Map<String, Any>>

    // 2️ 이력서 분석
    @POST("resumes/{resume_id}/analyze")
    fun analyzeResumeById(
        @Header("Authorization") bearerToken: String,
        @Path("resume_id") resumeId: Int
    ): Call<Map<String, Any>>

    @GET("resumes/{resume_id}/analysis/public")
    fun getAnalysisPublic(
        @Path("resume_id") resumeId: Int
    ): Call<okhttp3.ResponseBody>

//대시보드
    @GET("users/me/dashboard")
    fun getDashboard(
        @Header("Authorization") token: String
    ): Call<DashboardResponse>



    @POST("interview/start")
    fun startInterview(@Body request: ResumeRequest): Call<ResumeResponse>

    // 1. 면접 세션 생성 (Create Session)

    @POST("api/v1/sessions")
    fun createSession(
        @Body request: SessionRequest
    ): Call<SessionResponse>

    // 2. 질문 생성 (Generate Session Questions)
    @POST("api/v1/sessions/{session_id}/generate-questions")
    fun generateQuestions(
        @Path("session_id") sessionId: Int
    ): Call<List<GeneratedQuestion>>

    // 3. 생성된 질문 가져오기 (Get Questions)
    @GET("api/v1/sessions/{session_id}/questions")
    fun getQuestions(
        @Path("session_id") sessionId: Int
    ): Call<List<GeneratedQuestion>>

    // 4. 질문 저장 토글 (Toggle Save Question)
    @PATCH("api/v1/questions/{question_id}")
    fun toggleSaveQuestion(
        @Path("question_id") questionId: Int
    ): Call<Map<String, Any>>

    // 5. 답변 제출 및 분석 (Submit Answer)
    @Multipart
    @POST("api/v1/questions/{question_id}/answers")
    fun submitAnswer(
        @Path("question_id") questionId: Int,
        @Query("session_id") sessionId: Int,
        @Part audioFile: MultipartBody.Part
    ): Call<AnswerResponse>

    // 6. 저장된 질문 목록 보기 (Get Saved Questions)
    @GET("api/v1/users/{user_id}/saved-questions")
    fun getSavedQuestions(
        @Path("user_id") userId: String
    ): Call<List<GeneratedQuestion>>
}