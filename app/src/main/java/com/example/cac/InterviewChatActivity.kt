package com.example.cac

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cac.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import com.google.gson.Gson
import com.example.cac.network.AudioAnswerResponse
import com.example.cac.network.InterviewResultResponse
import com.example.cac.network.NewSpeechAnalysisSummaryResponse

class InterviewChatActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var btnRecord: View
    private lateinit var btnComplete: View
    private lateinit var btnReplay: View

    private var recordState = 0 // 0:대기, 1:녹음중, 2:중단됨
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var currentProcessingBubble: TextView? = null
    private var isAnalyzing = false

    private val handler = Handler(Looper.getMainLooper())
    private var userTypingRunnable: Runnable? = null
    private var typingRunnable: Runnable? = null
    private var typingBubble: View? = null
    private var questionId: Int = 1
    private var isSubmitting = false
    private var currentQuestionText: String = ""

    private var sessionId: Int = -1
    private var userName: String = "사용자"

    // 💡 AI 서버의 처리 속도를 고려하여 최대 재시도 횟수를 10회(총 20초)로 늘립니다.
    private var retryCount = 0
    private val MAX_RETRY = 10

    private var speechRetryCount = 0
    private val MAX_SPEECH_RETRY = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_chat)

        // 1. 뷰 바인딩
        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        btnRecord = findViewById(R.id.btnRecord)
        btnComplete = findViewById(R.id.btnComplete)
        btnReplay = findViewById(R.id.btnReplay)
        findViewById<TextView>(R.id.btnExit).setOnClickListener { finish() }

        // 2. 초기 세팅
        checkAudioPermission()
        fetchUserInfo()
        resetButtonVisibility()

        // 3. 데이터 초기화 및 질문 생성 로직
        sessionId = intent.getIntExtra("session_id", -1)

        if (sessionId != -1) {
            val firstQuestion = intent.getStringExtra("first_question")
                ?.takeIf { it.isNotBlank() }
                ?: "안녕하세요! 지금부터 면접을 시작하려 합니다. 자기소개 부탁드려도 될까요?"
            addInterviewerBubble(firstQuestion)
        } else {
            addInterviewerBubble("세션 정보를 찾을 수 없습니다.")
        }

        // 4. 버튼 리스너 세팅
        btnRecord.setOnClickListener {
            when (recordState) {
                0, 2 -> startRecording()
                1 -> stopRecording()
            }
        }

        btnComplete.setOnClickListener {
            if (recordState == 1 || recordState == 2) {
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    recordState = 2
                    resetButtonVisibility()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Log.d("AudioPathCheck", "실제 녹음 파일의 전체 주소: ${audioFile?.absolutePath}")

            if (audioFile == null || !audioFile!!.exists() || audioFile!!.length() <= 0L) {
                Toast.makeText(this, "답변을 녹음해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadAudioAnswer(audioFile!!)
        }

        btnReplay.setOnClickListener {
            if (audioFile == null || !audioFile!!.exists() || audioFile!!.length() <= 0L) {
                Toast.makeText(this, "답변을 녹음해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                reRecord()
            }
        }
    }

    private fun fetchUserInfo() {
        val rawToken = SessionManager.getToken(this) ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
        Log.d("UserInfo", "내 정보 요청 시작 - 토큰 유무: ${token.isNotBlank()}")


        val sharedPreferences = getSharedPreferences("CacPrefs", MODE_PRIVATE)

        RetrofitClient.api.me(token, 0, 0, 0).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("UserInfo", "서버 응답 데이터 전체: $body")

                    val fetchedName = body["username"] ?: body["name"] ?: body["nickname"]

                    if (fetchedName != null) {
                        userName = fetchedName.toString()
                        Log.d("UserInfo", "✅ 사용자 이름 연동 성공: $userName")


                        sharedPreferences.edit().putString("cached_user_name", userName).apply()
                    } else {
                        Log.w("UserInfo", "⚠️ 응답은 성공했으나 이름 데이터를 찾을 수 없습니다.")
                    }
                } else {
                    Log.e("UserInfo", "❌ 내 정보 조회 실패 (상태 코드: ${response.code()}) -> 로컬 캐시 이름 로드")


                    userName = sharedPreferences.getString("cached_user_name", "사용자") ?: "사용자"
                    Log.d("UserInfo", "복구된 사용자 이름: $userName")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("UserInfo", "❌ 통신 에러 발생: ${t.message} -> 로컬 캐시 이름 로드")

                userName = sharedPreferences.getString("cached_user_name", "사용자") ?: "사용자"
            }
        })
    }

    private fun startRecording() {
        isAnalyzing = false

        if (currentProcessingBubble == null) {
            currentProcessingBubble = createEmptyUserBubble()
        }

        if (recordState == 2 && mediaRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                recordState = 1
                btnRecord.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF5252"))
                resetButtonVisibility()
                startUserTypingAnimation()
                return
            }
        }

        recordState = 1
        btnRecord.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF5252"))
        resetButtonVisibility()
        startUserTypingAnimation()

        try {
            audioFile = File(externalCacheDir, "interview_audio.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createEmptyUserBubble(): TextView {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 40)
            gravity = Gravity.END
        }

        layout.addView(TextView(this).apply {
            text = userName
            textSize = 12f
            setPadding(0, 0, 10, 8)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
        })

        var tv: TextView? = null
        val card = MaterialCardView(this).apply {
            radius = 30f
            setCardBackgroundColor(Color.parseColor("#F0F2FF"))
            elevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }

            tv = TextView(context).apply {
                text = "."
                setPadding(40, 30, 40, 30)
                textSize = 15f
                maxWidth = (resources.displayMetrics.widthPixels * 0.7).toInt()
            }
            addView(tv)
        }
        layout.addView(card)
        chatContainer.addView(layout)
        scrollToBottom()
        return tv!!
    }

    private fun stopRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                recordState = 2
                isAnalyzing = false
                btnRecord.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3950E7"))
                resetButtonVisibility()
            } catch (e: Exception) {
                e.printStackTrace()
                finishRecordingCompletely()
            }
        } else {
            finishRecordingCompletely()
        }
    }

    private fun finishRecordingCompletely() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            recordState = 2
            isAnalyzing = false
            btnRecord.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3950E7"))
            resetButtonVisibility()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun reRecord() {
        recordState = 0
        isAnalyzing = false

        mediaRecorder?.apply {
            try { stop() } catch (e: Exception) {}
            release()
        }
        mediaRecorder = null

        userTypingRunnable?.let { handler.removeCallbacks(it) }
        currentProcessingBubble?.text = "."

        resetButtonVisibility()
        startRecording()
    }

    private fun uploadAudioAnswer(file: File) {
        if (isSubmitting) return
        isSubmitting = true
        isAnalyzing = true
        startUserTypingAnimation()

        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("audio_file", file.name, requestFile)

        RetrofitClient.api.submitInterviewAudio(sessionId, audioPart)
            .enqueue(object : Callback<AudioAnswerResponse> {
                override fun onResponse(call: Call<AudioAnswerResponse>, response: Response<AudioAnswerResponse>) {
                    isSubmitting = false
                    isAnalyzing = false

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        Log.d(
                            "InterviewFlow",
                            "답변 제출 완료 - is_finished=${body.is_finished}, next_question=${body.next_question}"
                        )
                        currentProcessingBubble?.text = body.stt_text
                        currentProcessingBubble = null

                        recordState = 0
                        resetButtonVisibility()

                        showTypingAnimation()
                        handler.postDelayed({
                            removeTypingAnimation()
                            if (!body.is_finished) {
                                addInterviewerBubble(body.next_question)
                            } else {
                                showDonePanelAnimation()
                            }
                        }, 1500)
                    } else {
                        handleUploadFailure()
                    }
                }

                override fun onFailure(call: Call<AudioAnswerResponse>, t: Throwable) {
                    handleUploadFailure()
                }
            })
    }

    private fun handleUploadFailure() {
        isSubmitting = false
        isAnalyzing = false
        recordState = 2
        resetButtonVisibility()
        removeTypingAnimation()
        currentProcessingBubble?.text = "(분석 실패 - 다시 시도해주세요)"
    }

    private fun addInterviewerBubble(text: String) {
        currentQuestionText = text
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 40)
        }
        val profileImg = ImageView(this).apply {
            setImageResource(R.drawable.ic_interviewer)
            layoutParams = LinearLayout.LayoutParams(90, 90).apply { topMargin = 10 }
        }
        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 0, 0)
        }
        textLayout.addView(TextView(this).apply {
            this.text = "면접관"
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            setPadding(10, 0, 0, 8)
        })
        val card = MaterialCardView(this).apply {
            radius = 30f
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#E0E0E0")
            strokeWidth = 2
            elevation = 0f
            val tv = TextView(context).apply {
                this.text = text
                setPadding(40, 30, 40, 30)
                setTextColor(Color.parseColor("#333333"))
                textSize = 15f
                maxWidth = (resources.displayMetrics.widthPixels * 0.65).toInt()
            }
            addView(tv)
        }

        textLayout.addView(card)
        layout.addView(profileImg)
        layout.addView(textLayout)
        chatContainer.addView(layout)
        scrollToBottom()
    }

    private fun startUserTypingAnimation() {
        userTypingRunnable?.let { handler.removeCallbacks(it) }

        userTypingRunnable = object : Runnable {
            override fun run() {
                if (recordState != 1 && !isAnalyzing) return
                if (currentProcessingBubble == null) return

                val dots = ".".repeat((System.currentTimeMillis() / 500 % 3).toInt() + 1)
                currentProcessingBubble?.text = if (isAnalyzing) "분석 중$dots" else dots

                handler.postDelayed(this, 500)
            }
        }
        handler.post(userTypingRunnable!!)
    }

    private fun showTypingAnimation() {
        val layout = LinearLayout(this).apply { setPadding(0, 0, 0, 40) }
        val card = MaterialCardView(this).apply {
            radius = 30f
            setCardBackgroundColor(Color.parseColor("#F5F5F5"))
            elevation = 0f
            addView(TextView(context).apply {
                text = "."
                setPadding(40, 25, 40, 25)
            })
        }
        layout.addView(card)
        chatContainer.addView(layout)
        typingBubble = layout
        scrollToBottom()

        var dot = 1
        typingRunnable = object : Runnable {
            override fun run() {
                dot = (dot % 3) + 1
                (card.getChildAt(0) as TextView).text = ".".repeat(dot)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(typingRunnable!!)
    }

    private fun removeTypingAnimation() {
        typingRunnable?.let { handler.removeCallbacks(it) }
        typingBubble?.let { chatContainer.removeView(it) }
        typingBubble = null
    }

    private fun resetButtonVisibility() {
        when (recordState) {
            0, 1 -> {
                btnRecord.visibility = View.VISIBLE
                btnComplete.visibility = View.GONE
                btnReplay.visibility = View.GONE
            }
            2 -> {
                btnRecord.visibility = View.VISIBLE
                btnReplay.visibility = View.VISIBLE
                btnComplete.visibility = View.VISIBLE
            }
        }
    }

    private fun scrollToBottom() {
        chatScrollView.post { chatScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun showDonePanelAnimation() {
        val donePanel = findViewById<LinearLayout>(R.id.layoutDonePanel)
        val txtStatus = findViewById<TextView>(R.id.txtDoneStatus)

        donePanel?.visibility = View.VISIBLE
        donePanel?.alpha = 0.0f

        donePanel?.animate()
            ?.alpha(1.0f)
            ?.setDuration(1500)
            ?.withEndAction {
                txtStatus?.text = "AI가 음성 발화를 정밀 분석 중입니다...\n잠시만 기다려 주세요."
                speechRetryCount = 0
                requestSpeechAnalysisAndNavigate(sessionId)
            }
            ?.start()
    }




    private fun requestSpeechAnalysisAndNavigate(sessionId: Int) {
        Log.d("SpeechAnalysis", "발화 분석 조회 시작 (GET)... ($speechRetryCount/$MAX_SPEECH_RETRY)")


        RetrofitClient.api.getSpeechAnalysis(sessionId).enqueue(object : Callback<NewSpeechAnalysisSummaryResponse> {
            override fun onResponse(
                call: Call<NewSpeechAnalysisSummaryResponse>,
                response: Response<NewSpeechAnalysisSummaryResponse>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d("SpeechAnalysis", "✅ 발화 분석 조회 완료!")
                    val speechAnalysisJson = Gson().toJson(response.body())
                    retryCount = 0
                    checkFeedbackReadyAndNavigate(sessionId, speechAnalysisJson)
                } else {
                    Log.e("SpeechAnalysis", "발화 분석 대기 중 (코드: ${response.code()})")
                    handleSpeechAnalysisRetry(sessionId)
                }
            }

            override fun onFailure(call: Call<NewSpeechAnalysisSummaryResponse>, t: Throwable) {
                Log.e("SpeechAnalysis", "발화 분석 통신 실패: ${t.message}")
                handleSpeechAnalysisRetry(sessionId)
            }
        })
    }

    private fun handleSpeechAnalysisRetry(sessionId: Int) {
        if (speechRetryCount < MAX_SPEECH_RETRY) {
            speechRetryCount++
            Log.d("SpeechAnalysis", "서버 오디오 가공 대기 중... 2초 뒤 자동으로 다시 조회합니다. ($speechRetryCount/$MAX_SPEECH_RETRY)")
            handler.postDelayed({
                requestSpeechAnalysisAndNavigate(sessionId)
            }, 2000)
        } else {
            Log.e("SpeechAnalysis", "❌ 발화 분석 대기 시간 초과(스킵). 빈 상태로 리포트 체크를 진행합니다.")
            retryCount = 0
            checkFeedbackReadyAndNavigate(sessionId, null)
        }
    }

    private fun checkFeedbackReadyAndNavigate(sessionId: Int, speechAnalysisJson: String?) {
        Log.d("InterviewWait", "피드백 리포트 조회 시작... ($retryCount/$MAX_RETRY)")

        RetrofitClient.api.getInterviewResult(sessionId).enqueue(object : Callback<InterviewResultResponse> {
            override fun onResponse(
                call: Call<InterviewResultResponse>,
                response: Response<InterviewResultResponse>
            ) {
                val body = response.body()


                if (response.isSuccessful && body != null && body.feedback != null) {
                    Log.d("InterviewWait", "✅ 피드백 리포트 생성 완료! 결과 화면으로 전환합니다.")


                    val feedbackJson = Gson().toJson(body)
                    navigateToResultActivity(sessionId, feedbackJson, speechAnalysisJson)
                } else {
                    if (retryCount < MAX_RETRY) {
                        retryCount++
                        Log.d("InterviewWait", "서버가 리포트를 생성하고 있습니다. ($retryCount/$MAX_RETRY) 2초 뒤 재시도...")
                        handler.postDelayed({
                            checkFeedbackReadyAndNavigate(sessionId, speechAnalysisJson)
                        }, 2000)
                    } else {
                        Log.e("InterviewWait", "❌ 리포트 생성 대기 시간 초과로 강제 화면 이동 유도")
                        Toast.makeText(this@InterviewChatActivity, "분석 완료 처리가 지연되고 있습니다. 잠시 후 결과창을 확인해 주세요.", Toast.LENGTH_LONG).show()
                        navigateToResultActivity(sessionId, null, speechAnalysisJson)
                    }
                }
            }

            override fun onFailure(call: Call<InterviewResultResponse>, t: Throwable) {
                Log.e("InterviewWait", "네트워크 통신 실패: ${t.message}")
                navigateToResultActivity(sessionId, null, speechAnalysisJson)
            }
        })
    }

    private fun navigateToResultActivity(
        sessionId: Int,
        feedbackJson: String? = null,
        speechAnalysisJson: String? = null
    ) {
        val intent = Intent(this@InterviewChatActivity, InterviewResultActivity::class.java)
        intent.putExtra("session_id", sessionId)

        if (!feedbackJson.isNullOrBlank()) {
            intent.putExtra("interview_result_json", feedbackJson)
        }
        if (!speechAnalysisJson.isNullOrBlank()) {
            intent.putExtra("speech_analysis_json", speechAnalysisJson)
        }

        startActivity(intent)
        finish()
    }

    private fun checkAudioPermission() {
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "마이크 권한 승인", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userTypingRunnable?.let { handler.removeCallbacks(it) }
        typingRunnable?.let { handler.removeCallbacks(it) }

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }
}
