package com.example.cac

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.cac.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.*
import android.os.Handler
import android.os.Looper
class InterviewStartActivity : AppCompatActivity() {

    private var fullAnswer: String = ""
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var scrollAnswer: NestedScrollView
    private lateinit var btnMic: ImageButton
    private lateinit var micBackground: View
    private lateinit var layoutDots: View
    private lateinit var txtAnswer: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var layoutFeedbackResult: View
    private lateinit var txtQuestion: TextView
    private lateinit var imgStar: ImageView
    private lateinit var tooltipLayout: View
    private lateinit var btnNextQuestion: View
    private lateinit var btnFinalMockInterview: View

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isReadyToUpload = false
    private lateinit var speechRecognizer: SpeechRecognizer
    private var recognitionIntent: Intent? = null

    private var isSaved = false
    private var isFirstQuestion = true
    private var sessionId: Int = -1
    private var questionId: Int = -1
    private var currentQuestionCount = 1
    private var totalQuestionCount = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_start)

        sessionId = intent.getIntExtra("session_id", -1)
        questionId = intent.getIntExtra("question_id", -1)
        android.util.Log.d("FINAL_CHECK", "제출할 질문 ID: $questionId")
        val questionText = intent.getStringExtra("question_text").orEmpty()
        totalQuestionCount = intent.getIntExtra("count", 3)

        if (questionId == -1) {
            android.util.Log.e("DATA_CHECK", "question_id를 받지 못했습니다!")
        }

        initViews()
        initSpeechRecognizer()

        txtQuestion.text = questionText

        val progressQuestion = findViewById<ProgressBar>(R.id.progressQuestion)
        progressQuestion.max = totalQuestionCount
        progressQuestion.progress = currentQuestionCount
        findViewById<TextView>(R.id.txtProgress).text = "$currentQuestionCount/$totalQuestionCount"

        imgStar.setOnClickListener { toggleStarStatus() }
        btnMic.setOnClickListener { handleMicClick() }
        btnRefresh.setOnClickListener { resetRecording() }
        btnNextQuestion.setOnClickListener { moveToNextQuestion() }

        setupTitle()
        setupBottomButtons()
        startDotAnimation()
    }

    private fun initViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        scrollAnswer = findViewById(R.id.scrollAnswer)
        btnMic = findViewById(R.id.btnMic)
        micBackground = findViewById(R.id.micBackground)
        layoutDots = findViewById(R.id.layoutDots)
        txtAnswer = findViewById(R.id.txtAnswer)
        btnRefresh = findViewById(R.id.btnRefresh)
        layoutFeedbackResult = findViewById(R.id.layoutFeedbackResult)
        txtQuestion = findViewById(R.id.txtQuestion)
        imgStar = findViewById(R.id.imgStar)
        tooltipLayout = findViewById(R.id.tooltipLayout)
        btnNextQuestion = findViewById(R.id.btnNextQuestion)
        btnFinalMockInterview = findViewById(R.id.btnFinalMockInterview)
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {

                scrollAnswer.visibility = View.VISIBLE
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {

                    val currentText = matches[0]
                    txtAnswer.text = if (fullAnswer.isEmpty()) currentText else "$fullAnswer $currentText"
                    scrollAnswer.post { scrollAnswer.fullScroll(View.FOCUS_DOWN) }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // 한 문장이 확실히 끝났으므로 전체 문장 변수에 저장
                    fullAnswer = if (fullAnswer.isEmpty()) matches[0] else "$fullAnswer ${matches[0]}"
                    txtAnswer.text = fullAnswer
                }

                // 녹음 중이라면(버튼을 아직 안 눌렀다면) 자동으로 다시 듣기 시작
                if (isRecording) {
                    speechRecognizer.startListening(recognitionIntent)
                }
            }

            override fun onError(error: Int) {
                // 침묵으로 인해 멈춘 경우 자동으로 다시 듣기 실행
                if (isRecording) {
                    speechRecognizer.startListening(recognitionIntent)
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun handleMicClick() {
        if (!isRecording && !isReadyToUpload) {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions()
            }
        } else if (isRecording) {
            stopRecording()
        } else if (isReadyToUpload) {
            uploadAnswer()
        }
    }

    private fun startRecording() {
        try {

            audioFile = File(externalCacheDir, "interview_audio.m4a")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start() // 실제 녹음 시작
            }

            isRecording = true
            txtAnswer.text = "답변을 녹음 중입니다..."
            scrollAnswer.visibility = View.VISIBLE

            // 아이콘을 빨간색으로 변경하여 녹음 중임을 표시
            btnMic.setColorFilter(android.graphics.Color.parseColor("#FF0000"))

        } catch (e: Exception) {
            android.util.Log.e("AUDIO_ERROR", "녹음 시작 실패", e)
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) { e.printStackTrace() }

        isRecording = false


        btnMic.clearColorFilter()
        btnMic.visibility = View.INVISIBLE
        layoutDots.visibility = View.VISIBLE
        txtAnswer.text = "서버에서 답변을 분석 중입니다..."

        // 바로 서버 전송 시작
        uploadAnswer()
    }

    private fun uploadAnswer() {
        val file = audioFile ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 실제 오디오 파일(.m4a)을 전송용 Body로 생성
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("audio_file", file.name, requestFile)

                // 2. 히스토리 데이터 (서버 명세에 따라 빈 리스트 혹은 실제 데이터 전달)
                val historyJson = com.google.gson.Gson().toJson(emptyList<Map<String, String>>())
                val historyBody = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), historyJson)

                // 3. 서버 호출
                val response = RetrofitClient.api.submitAnswer(
                    questionId = questionId,
                    sessionId = sessionId,
                    audioFile = filePart
                ).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!


                        txtAnswer.text = data.stt_text

                        // 분석 완료 후 UI 복구: 점점점 숨기고 체크 아이콘 표시
                        layoutDots.visibility = View.GONE
                        btnMic.visibility = View.VISIBLE
                        btnMic.setImageResource(R.drawable.ic_check) // 체크 아이콘으로 변경

                        displayFeedback(data)
                    } else {
                        resetUIOnError("분석 실패: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resetUIOnError("네트워크 연결을 확인해 주세요.")
                }
            }
        }
    }
    private fun displayFeedback(result: com.example.cac.data.AnswerResponse) {
        layoutFeedbackResult.visibility = View.VISIBLE
        if (isFirstQuestion) tooltipLayout.visibility = View.VISIBLE

        // XML 구조에 맞게 텍스트 수정
        val speedCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.feedbackItem1)
        val speedValue = (speedCard.getChildAt(0) as LinearLayout).getChildAt(1) as TextView
        speedValue.text = result.feedback_speed ?: "조금 빠름"

        if (currentQuestionCount >= totalQuestionCount) {
            btnNextQuestion.visibility = View.GONE
            btnFinalMockInterview.visibility = View.VISIBLE
        } else {
            btnNextQuestion.visibility = View.VISIBLE
        }
        mainScrollView.post { mainScrollView.smoothScrollTo(0, layoutFeedbackResult.top) }
    }

    private fun resetUIOnError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        layoutDots.visibility = View.GONE
        btnMic.visibility = View.VISIBLE
        btnMic.setImageResource(R.drawable.ic_mic) // 다시 마이크 아이콘으로
        txtAnswer.text = "다시 녹음해 주세요."
    }
    private fun resetRecording() {
        fullAnswer = ""
        isRecording = false
        isReadyToUpload = false
        txtAnswer.text = ""
        btnMic.setImageResource(R.drawable.ic_mic)
        btnMic.visibility = View.VISIBLE
        btnRefresh.visibility = View.INVISIBLE
        layoutFeedbackResult.visibility = View.GONE
        scrollAnswer.visibility = View.GONE
    }

    private fun moveToNextQuestion() {
        if (currentQuestionCount < totalQuestionCount) {
            currentQuestionCount++
            resetRecording()
            findViewById<ProgressBar>(R.id.progressQuestion).progress = currentQuestionCount
            findViewById<TextView>(R.id.txtProgress).text = "$currentQuestionCount/$totalQuestionCount"
            mainScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun toggleStarStatus() {
        isSaved = !isSaved
        imgStar.setImageResource(if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        imgStar.setColorFilter(if (isSaved) Color.parseColor("#FFD700") else Color.WHITE)
        if (isSaved) tooltipLayout.visibility = View.GONE
    }

    private fun updateUIForRecording() {
        micBackground.alpha = 0.5f
        txtAnswer.text = "말씀해 주세요..."
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestPermissions() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1000)

    private fun startDotAnimation() {
        val dots = listOf(R.id.dot1, R.id.dot2, R.id.dot3).map { findViewById<View>(it) }
        dots.forEachIndexed { index, dot ->
            ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                startDelay = (index * 200).toLong()
                start()
            }
        }
    }

    private fun setupTitle() {
        val title = findViewById<TextView>(R.id.txtTitle)
        val text = "Career AI Coach"
        val spannable = SpannableString(text)
        val blue = Color.parseColor("#3950E7")
        val gray = Color.parseColor("#8A8A8A")
        spannable.setSpan(ForegroundColorSpan(blue), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}