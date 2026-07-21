package com.example.cac.network

import com.example.cac.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import com.google.gson.annotations.SerializedName


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
    fun me(
        @Header("Authorization") bearerToken: String,
        @Query("resume_limit") resumeLimit: Int? = null,
        @Query("interview_limit") interviewLimit: Int? = null,
        @Query("saved_question_limit") savedQuestionLimit: Int? = null
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
    fun getAnalysisPublic(@Path("resume_id") resumeId: Int): Call<ResponseBody>


    //커리어경로
    @POST("career/recommend")
    fun getCareerRoadmap(@Body request: CareerRoadmapRequest): Call<RoadmapResponse>


    // 마이페이지
    @GET("auth/me/resumes")
    suspend fun getResumes(@Header("Authorization") token: String): ResumeResponse
    @GET("auth/me/interviews")
    suspend fun getInterviews(@Header("Authorization") token: String): InterviewResponse
    @GET("auth/me/saved-questions")
    suspend fun getMySavedQuestions(@Header("Authorization") token: String): ResponseBody

    // 대시보드
    @GET("users/me/dashboard")
    fun getDashboard(@Header("Authorization") token: String): Call<com.google.gson.JsonObject>

    @POST("interview/start")
    fun startInterview(@Body request: ResumeRequest): Call<ResumeResponse>

    // 세션 생성
    @POST("api/v1/sessions")
    fun createSession(@Body request: SessionRequest): Call<SessionResponse>

    // AI 질문 생성
    @POST("api/v1/sessions/{session_id}/generate-questions")
    fun generateQuestions(
        @Path("session_id") sessionId: Int
    ): Call<List<GeneratedQuestion>>

    // 세션별 질문 전체 조회
    @GET("api/v1/sessions/{session_id}/questions")
    fun getQuestions(
        @Path("session_id") sessionId: Int,
        @Query("count") count: Int? = null,
        @Query("question_types") type: String? = null
    ): Call<List<GeneratedQuestion>>

    // 질문저장(즐겨찾기)
    @PATCH("api/v1/questions/{question_id}")
    fun toggleSaveQuestion(
        @Header("Authorization") token: String,
        @Path("question_id") questionId: Int
    ): Call<Map<String, Any>>

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
    fun getSessionSummary(
        @Path("session_id") sessionId: Int
    ): Call<ResponseBody>

    @GET("api/v1/sessions/{session_id}/result")
    fun getSessionResultRaw(
        @Path("session_id") sessionId: Int
    ): Call<ResponseBody>

    // 발화 분석 API
    @Multipart
    @POST("api/v1/interview/sessions/{session_id}/analyze-speech")
    fun analyzeSpeech(
        @Path("session_id") sessionId: Int,
        @Part audioFile: MultipartBody.Part
    ): Call<SpeechAnalysisResponse>


    @GET("api/v1/interview/sessions/{session_id}/feedback")
    fun getInterviewResult(
        @Path("session_id") sessionId: Int
    ): Call<InterviewResultResponse>


    // 확인용
    @GET("api/v1/users/{user_id}/saved-questions")
    fun getSavedQuestionsRaw(
        @Path("user_id")
        userId: String
    ): Call<okhttp3.ResponseBody>


    @GET("api/v1/sessions/{session_id}/questions")
    suspend fun getQuestions(
        @Header("Authorization") authHeader: String,
        @Path("session_id") sessionId: Int
    ): List<Question>

    // 질문 저장
    @GET("api/v1/users/{user_id}/saved-questions")
    suspend fun getSavedQuestions(
        @Header("Authorization") token: String,
        @Path("user_id") userId: String
    ): List<String>

    //종합 면접채팅 발화
    @GET("api/v1/interview/sessions/{session_id}/analyze-speech")
    fun getSpeechAnalysis(
        @Path("session_id") sessionId: Int
    ): Call<NewSpeechAnalysisSummaryResponse>

    @GET("api/v1/interview/sessions/{session_id}/analyze-speech")
    fun getSpeechAnalysisRaw(
        @Path("session_id") sessionId: Int
    ): Call<ResponseBody>

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
    fun getInterviewHistory(@Path("user_id") userId: String): Call<InterviewHistoryResponse>

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
    val question_feedbacks: List<InterviewQuestionFeedback>,
    val strengths: List<String>?,
    val improvements: List<String>?
)

data class NewSpeechAnalysisSummaryResponse(
    val session_id: Int,
    val speech_analysis: NewSpeechAnalysisData
)

data class NewSpeechAnalysisData(
    @SerializedName("speaking_speed_wpm") val speaking_speed_wpm: Double?,
    @SerializedName("intonation_score") val intonation_score: Double?,   // 발음 명료도
    @SerializedName("confidence_avg") val confidence_avg: Double?,      // 자신감
    @SerializedName("volume_score") val volume_score: Double?,          // 목소리 크기
    @SerializedName("pause_count_avg") val pause_count_avg: Double?,     // 쉼 횟수 평균
    @SerializedName("filler_word_total") val filler_word_total: Double?, // 필러워드 총합
    @SerializedName("speech_ratio_avg") val speech_ratio_avg: Double?,    // 발화 비율
    val question_count: Int
)

data class InterviewQuestionFeedback(
    val question_id: Int,
    @SerializedName("question") val question_text: String,
    @SerializedName("answer_summary") val answer_text: String,
    val score: Double,
    val strength: String? = null,
    val weakness: String? = null,
    @SerializedName("feedback") val suggestion: String? = null
)

data class AnswerDetail(
    val answer_id: Int?,
    val stt_text: String?,
    val filler_word_count: Int?,
    val audio_scores: AudioScores?
)

data class AudioScores(
    val tempo: Double?,               // 말하기 속도
    val intonation_score: Double?,     // 억양 점수
    val confidence_score: Double?,     // 자신감 점수
    val volume_score: Double?,         // 목소리 크기 점수
    val pause_count: Int?,             // 정적/쉼 횟수
    val speech_ratio: Double?,
    val duration_sec: Double?,


    val speed_feedback: String? = "",
    val volume_feedback: String? = "",
    val stutter_feedback: String? = "",
    val intonation_feedback: String? = ""
)


data class SpeechAnalysisResponse(
    @SerializedName("avg_tempo") val avg_tempo: Double = 0.0,
    @SerializedName("filler_word_count") val filler_words_count: Int = 0,
    @SerializedName("pause_count") val silence_duration: Double = 0.0,
    @SerializedName("voice_volume") val volume_eval: String? = null,
    @SerializedName("pronunciation_clarity") val clarity_eval: String? = null,
    @SerializedName("confidence") val confidence_eval: String? = null
) {
    val clarity: Double get() = when (clarity_eval) { "명료함", "우수", "좋음" -> 95.0; "보통" -> 70.0; else -> 45.0 }
    val confidence: Double get() = when (confidence_eval) { "우수", "높음", "좋음", "자신감 있음" -> 90.0; "보통" -> 70.0; else -> 50.0 }
    val volume: Double get() = when (volume_eval) { "적절", "우수", "좋음" -> 85.0; "보통" -> 65.0; else -> 45.0 }
}

data class FeedbackDetail(
    val strength: String?,
    val weakness: String?,
    val suggestion: String?,
    val score: Int?
)
data class SpeechDetailResponse(
    @SerializedName("speaking_speed_wpm") val speech_rate: Double = 0.0,
    @SerializedName("filler_word_count") val filler_words_count: Int = 0,
    @SerializedName("pause_count") val silence_duration: Double = 0.0,
    @SerializedName("voice_volume") val volume_eval: String? = null,
    @SerializedName("pronunciation_clarity") val clarity_eval: String? = null,
    @SerializedName("confidence") val confidence_eval: String? = null
) {
    val clarity: Double get() = when (clarity_eval) { "명료함", "우수", "좋음" -> 95.0; "보통" -> 70.0; else -> 45.0 }
    val confidence: Double get() = when (confidence_eval) { "우수", "높음", "좋음", "자신감 있음" -> 90.0; "보통" -> 70.0; else -> 50.0 }
    val volume: Double get() = when (volume_eval) { "적절", "우수", "좋음" -> 85.0; "보통" -> 65.0; else -> 45.0 }
}
data class InterviewHistoryItem(
    val session_id: Int,
    val date: String,
    @SerializedName("score") val overall_score: Double,
    val change: Int?
)





data class InterviewHistoryResponse(
    val history: List<InterviewHistoryItem>,
    val total_count: Int,
    val growth_rate: Double,
    val score: Int,
    val first_score: Int,
    val latest_score: Int
)



//마이페이지------------------------
data class ResumeResponse(
    val resumes: List<ResumeItem>,
    val resume_total: Int,
    val resume_has_more: Boolean
)

data class ResumeItem(
    @SerializedName(value = "resume_id", alternate = ["id"]) val resumeId: Int? = null,
    @SerializedName(value = "original_filename", alternate = ["filename", "file_name"]) val fileName: String = "이력서",
    @SerializedName(value = "created_at", alternate = ["date"]) val date: String? = null,
    @SerializedName("target_job") val targetJob: String? = null,
    @SerializedName("pdf_url") val pdfUrl: String? = null
)
data class ApiInterviewItem(
    val session_id: Int?,
    val target_job: String?,
    val created_at: String?,
    val overall_score: Int?,
    val feedback: String?,
    val pdf_url: String?
)

data class InterviewResponse(
    val interviews: List<ApiInterviewItem>?
)
//질문리스트
data class Question(
    @SerializedName("id") val id: Int,
    @SerializedName("content") val content: String
)
//---------------------------

//커리어경로------------------------
// 단계별 데이터
data class CareerRoadmapRequest(
    val user_id: String
)

data class Stage(
    val stage_level: Int? = null,
    val stage_name: String? = null,
    val recommendations: List<String>? = null
)

data class RoadmapResponse(
    val type: String? = null,
    val user_id: String? = null,
    val roadmap_title: String? = null,
    val stages: List<Stage>? = null,
    val recommendations: List<String>? = null,
    val message: String? = null,
    @SerializedName("job_match_score") val jobMatchScore: Int? = null,
    @SerializedName("match_score") val matchScore: Int? = null
)

//-------------------------------------
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
