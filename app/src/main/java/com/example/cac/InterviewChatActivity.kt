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
import com.google.gson.Gson
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
import com.example.cac.data.GeneratedQuestion
import com.example.cac.network.AudioAnswerResponse

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

        // 2. 초기 세팅 (이름 변경으로 충돌 해결)
        checkAudioPermission()
        fetchUserInfo()
        resetButtonVisibility()

        // 3. 데이터 초기화 및 질문 생성 로직
        sessionId = intent.getIntExtra("session_id", -1)

        if (sessionId != -1) {
            addInterviewerBubble("안녕하세요! 지금부터 면접을 시작하려 합니다. 자기소개 부탁드려도 될까요?")
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
        val token = "Bearer ${SessionManager.getToken(this)}"
        RetrofitClient.api.me(token).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    userName = response.body()?.get("username")?.toString() ?: "사용자"
                }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
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
                startUserTypingAnimation()
                return
            }
        }

        recordState = 1
        btnRecord.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF5252"))
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

    private fun prepareAndFetchQuestions() {
        RetrofitClient.api.generateQuestions(sessionId).enqueue(object : Callback<List<GeneratedQuestion>> {
            override fun onResponse(call: Call<List<GeneratedQuestion>>, response: Response<List<GeneratedQuestion>>) {
                if (response.isSuccessful) {
                    fetchNextQuestionFromServer()
                } else {
                    addInterviewerBubble("질문 생성에 실패했습니다. 다시 시도해 주세요.")
                }
            }
            override fun onFailure(call: Call<List<GeneratedQuestion>>, t: Throwable) {
                addInterviewerBubble("네트워크 오류가 발생했습니다.")
            }
        })
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

    private fun fetchNextQuestionFromServer() {
        showTypingAnimation()

        RetrofitClient.api.getQuestions(sessionId).enqueue(object : Callback<List<GeneratedQuestion>> {
            override fun onResponse(call: Call<List<GeneratedQuestion>>, response: Response<List<GeneratedQuestion>>) {
                removeTypingAnimation()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val questions = response.body()!!
                    val nextQuestionData = questions.find { it.order_num == questionId }
                        ?: questions.firstOrNull()

                    if (nextQuestionData != null) {
                        addInterviewerBubble(nextQuestionData.question_text)
                    } else {
                        addInterviewerBubble("준비된 모든 질문이 끝났습니다. 수고하셨습니다!")
                    }
                }
            }

            override fun onFailure(call: Call<List<GeneratedQuestion>>, t: Throwable) {
                removeTypingAnimation()
                addInterviewerBubble("네트워크 오류로 다음 질문을 가져오지 못했습니다.")
            }
        })
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
                requestSpeechAnalysisAndNavigate(sessionId)
            }
            ?.start()
    }

    private fun requestSpeechAnalysisAndNavigate(sessionId: Int) {
        val audioFile = File(externalCacheDir, "interview_audio.m4a")

        val audioMultipart: MultipartBody.Part = if (audioFile.exists() && audioFile.length() > 0) {
            val requestBody = audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
        } else {
            navigateToResultActivity(sessionId)
            return
        }


        RetrofitClient.api.analyzeSpeech(sessionId, audioMultipart).enqueue(object : Callback<com.example.cac.network.SpeechAnalysisResponse> {


            override fun onResponse(
                call: Call<com.example.cac.network.SpeechAnalysisResponse>,
                response: Response<com.example.cac.network.SpeechAnalysisResponse>
            ) {
                var speechAnalysisJson: String? = null

                if (response.isSuccessful && response.body() != null) {
                    Log.d("SpeechAnalysis", "발화 분석 완료 성공! 이어서 피드백 생성 여부 체크 시작")
                    speechAnalysisJson = com.google.gson.Gson().toJson(response.body())
                } else {
                    Log.e("SpeechAnalysis", "발화 분석 서버 에러 발생 코드: ${response.code()}")
                }

                checkFeedbackReadyAndNavigate(sessionId, speechAnalysisJson)
            }


            override fun onFailure(call: Call<com.example.cac.network.SpeechAnalysisResponse>, t: Throwable) {
                Log.e("SpeechAnalysis", "발화 분석 네트워크 통신 실패 원인: ${t.message}")
                checkFeedbackReadyAndNavigate(sessionId, null)
            }
        })
    }

    private fun checkFeedbackReadyAndNavigate(sessionId: Int, speechAnalysisJson: String?) { // ◀ 파라미터 추가
        RetrofitClient.api.getInterviewFeedback(sessionId).enqueue(object : Callback<com.example.cac.network.InterviewResultResponse> {
            override fun onResponse(call: Call<com.example.cac.network.InterviewResultResponse>, response: Response<com.example.cac.network.InterviewResultResponse>) {
                val body = response.body()

                if (response.isSuccessful && body != null &&
                    !body.feedback.question_feedbacks.isNullOrEmpty()) {

                    Log.d("InterviewWait", "피드백 데이터 확인 완료! 결과 화면으로 이동합니다.")


                    val feedbackJson = com.google.gson.Gson().toJson(body)


                    navigateToResultActivity(sessionId, feedbackJson, speechAnalysisJson)
                } else {
                    Log.d("InterviewWait", "데이터가 아직 불완전함(AI 생성 중). 2초 뒤 재시도...")
                    Handler(Looper.getMainLooper()).postDelayed({

                        checkFeedbackReadyAndNavigate(sessionId, speechAnalysisJson)
                    }, 2000)
                }
            }

            override fun onFailure(call: Call<com.example.cac.network.InterviewResultResponse>, t: Throwable) {
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
        }
        mediaRecorder = null
    }
}