package com.example.cac.network

import com.example.cac.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("auth/signup")
    fun signup(@Body body: SignupRequest): Call<Map<String, Any>>

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
    fun me(@Header("Authorization") bearerToken: String): Call<Map<String, Any>>

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
    fun getAnalysisPublic(@Path("resume_id") resumeId: Int): Call<ResponseBody>

    // 대시보드
    @GET("users/me/dashboard")
    fun getDashboard(@Header("Authorization") token: String): Call<DashboardResponse>

    @POST("interview/start")
    fun startInterview(@Body request: ResumeRequest): Call<ResumeResponse>

    // 세션 생성
    @POST("api/v1/sessions")
    fun createSession(@Body request: SessionRequest): Call<SessionResponse>

    // AI 질문 생성
    @POST("api/v1/sessions/{session_id}/generate-questions")
    fun generateQuestions(@Path("session_id") sessionId: Int): Call<List<GeneratedQuestion>>

    // 세션별 질문 전체 조회
    @GET("api/v1/sessions/{session_id}/questions")
    fun getQuestions(
        @Path("session_id") sessionId: Int,
        @Query("count") count: Int? = null,
        @Query("question_types") type: String? = null
    ): Call<List<GeneratedQuestion>>

    // 질문저장(즐겨찾기)
    @PATCH("api/v1/questions/{question_id}")
    fun toggleSaveQuestion(@Path("question_id") questionId: Int): Call<Map<String, Any>>

    // 답변 제출 및 분석
    @Multipart
    @POST("api/v1/questions/{question_id}/answers")
    fun submitAnswer(
        @Path("question_id") questionId: Int,
        @Query("session_id") sessionId: Int,
        @Part audioFile: MultipartBody.Part,
        @Part("history") history: RequestBody? = null
    ): Call<AnswerResponse>

    // 피드백 요약
    @GET("api/v1/sessions/{session_id}/summary")
    fun getSessionSummary(@Path("session_id") sessionId: Int): Call<ResponseBody>

    // 발화 분석 API (Multipart가 필요한 실제 파일 분석용)
    @Multipart
    @POST("api/v1/interview/sessions/{session_id}/analyze-speech")
    fun analyzeSpeech(
        @Path("session_id") sessionId: Int,
        @Part audioFile: MultipartBody.Part
    ): Call<SpeechDetailResponse>

    @GET("api/v1/interview/sessions/{session_id}/feedback")
    fun getInterviewFeedback(@Path("session_id") sessionId: Int): Call<InterviewResultResponse>

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
    fun createInterviewSession(@Body request: InterviewSessionRequest): Call<SessionResponse>

    // 면접 이력 조회
    @GET("api/v1/interview/users/{user_id}/history")
    fun getInterviewHistory(@Path("user_id") userId: String): Call<List<InterviewHistoryItem>>

    // 세션 분석 상세 조회
    @GET("api/v1/interview/sessions/{session_id}/analysis")
    fun getSessionAnalysis(@Path("session_id") sessionId: Int): Call<ResponseBody>

    // 저장된 질문 목록 보기
    @GET("api/v1/users/{user_id}/saved-questions")
    fun getSavedQuestions(@Path("user_id") userId: String): Call<List<String>>
}

// --- 데이터 클래스  ---

data class InterviewResultResponse(
    val session_id: Int,
    val feedback: InterviewResultData
)

data class InterviewResultData(
    val overall_score: Double,
    val competency_scores: Map<String, Int>,
    val competency_comments: Map<String, String>,
    val question_feedbacks: List<InterviewQuestionFeedback>
)

data class InterviewQuestionFeedback(
    val question: String,
    val feedback: String,
    val score: Int,
    val analysis: SpeechDetailResponse?
)

data class SpeechDetailResponse(
    val speech_rate: Double,
    val filler_words_count: Int,
    val silence_duration: Double,
    val clarity: Double
)

data class InterviewHistoryItem(
    val session_id: Int,
    val date: String,
    val overall_score: Double
)

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

data class QuestionResponse(
    val resume_id: Int,
    val job_title: String,
    val questions: List<String>
)