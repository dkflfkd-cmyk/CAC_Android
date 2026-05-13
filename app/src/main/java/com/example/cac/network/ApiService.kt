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
import retrofit2.http.*

interface ApiService {





    @POST("auth/signup")
    fun signup(@Body body: com.example.cac.data.SignupRequest): Call<Map<String, Any>>

//질문리스트
    @GET("api/v1/sessions/{session_id}/questions")
    fun getQuestions(
    @Path("session_id") sessionId: Int,
        @Query("count") count: Int,
        @Query("question_types") type: String
    ): Call<List<GeneratedQuestion>>


    //질문저장(질문리스트)
    @PATCH("api/v1/questions/{question_id}")
    fun toggleSaveQuestion(
        @Path("question_id") questionId: Int
    ): Call<Map<String, Any>>


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


    //질문리스트
    // 1. 세션 생성
    @POST("api/v1/sessions")
    fun createSession(
        @Body request: SessionRequest
    ): Call<SessionResponse>

    // 2. AI 질문 생성
    @POST("api/v1/sessions/{session_id}/generate-questions")
    fun generateQuestions(
        @Path("session_id") sessionId: Int
    ): Call<List<GeneratedQuestion>>

    // 3. 세션별 질문 전체 조회
    @GET("api/v1/sessions/{session_id}/questions")
    fun getQuestions(
        @Path("session_id") sessionId: Int
    ): Call<List<GeneratedQuestion>>



    // 5. 답변 제출 및 분석
    @Multipart
    @POST("api/v1/questions/{question_id}/answers")
    fun submitAnswer(
        @Path("question_id") questionId: Int,
        @Query("session_id") sessionId: Int,
        @Part audioFile: MultipartBody.Part
        // @Part("history") history: RequestBody // PDF에 없으므로 서버 확인 필요
    ): Call<AnswerResponse>

    // 6. 피드백 요약
    @GET("api/v1/sessions/{session_id}/summary")
    fun getSessionSummary(
        @Path("session_id") sessionId: Int
    ): Call<ResponseBody>



    //발화특성
    @Multipart
    @POST("interview/sessions/{session_id}/analyze-speech")
    fun analyzeSpeech(
        @Path("session_id") sessionId: Int,
        @Header("Authorization") token: String,
        @Part audioFile: MultipartBody.Part
    ): Call<Map<String, Any>>


//[AI 면접 채팅]
//@Multipart
//@POST("api/v1/interview/sessions/{session_id}/answer")
//fun submitInterviewAnswer(
//    @Path("session_id") sessionId: Int,
//    @Part audio_file: MultipartBody.Part,
//    @Part history: MultipartBody.Part,   // RequestBody 대신 Part 사용
//    @Part question: MultipartBody.Part,  // RequestBody 대신 Part 사용
//    @Part answer: MultipartBody.Part     // RequestBody 대신 Part 사용
//): Call<AnswerResponse>



    @GET("api/v1/interview/sessions/{session_id}/feedback")
    fun getInterviewFeedback(
        @Path("session_id") sessionId: Int
    ): Call<FeedbackResponse>

    @POST("api/v1/resume/{resume_id}/interview/evaluate")
    fun evaluateInterview(
        @Path("resume_id") resumeId: Int,
        @Body body: Map<String, Any>
    ): Call<AnswerResponse>

    @Multipart
    @POST("api/v1/interview/sessions/{session_id}/answer-audio")
    fun submitInterviewAudio(
        @Path("session_id") sessionId: Int,
        @Part audio_file: MultipartBody.Part
    ): Call<AudioAnswerResponse>

    @POST("api/v1/interview/sessions")
    fun createInterviewSession(
        @Body request: InterviewSessionRequest
    ): Call<SessionResponse>

    //답변 제출 및 분석
    @Multipart
    @POST("api/v1/questions/{question_id}/answers")
    fun submitAnswer(
        @Path("question_id") questionId: Int,
        @Query("session_id") sessionId: Int,
        @Part audioFile: MultipartBody.Part,
        @Part("history") history: okhttp3.RequestBody
    ): Call<AnswerResponse>


    // 저장된 질문 목록 보기
    @GET("api/v1/users/{user_id}/saved-questions")
    fun getSavedQuestions(
        @Path("user_id") userId: String
    ): Call<List<String>>




}

//채팅
data class InterviewSessionRequest(
    val user_id: String,
    val target_job: String
)

data class AudioAnswerResponse(
    val stt_text: String,
    val next_question: String,
    val question_type: String,
    val is_finished: Boolean
)

//질문리스트 받
data class QuestionResponse(
    val resume_id: Int,
    val job_title: String,
    val questions: List<String>
)