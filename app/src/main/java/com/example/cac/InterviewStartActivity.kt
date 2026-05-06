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
import android.os.Handler
import android.os.Looper
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

class InterviewStartActivity : AppCompatActivity() {

    // 뷰 바인딩 변수들
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

//질문 그래프


    // 녹음 관련 변수
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isReadyToUpload = false


    private var isSaved = false          // 별표 저장 상태
    private var isFirstQuestion = true    // 첫 질문 여부 (툴팁 제어)
    private var isLastQuestion = false     // 마지막 질문 여부 (버튼 제어)
    private var isTypingFinished = false

    // 서버 통신용 ID
    private var sessionId: Int = -1
    private var questionId: Int = -1


    private var currentQuestionCount = 1
    private var totalQuestionCount = 3

    private val handler = Handler(Looper.getMainLooper())
    private var typeRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_start)

        // 인텐트 데이터 수신
        sessionId = intent.getIntExtra("session_id", -1)
        questionId = intent.getIntExtra("question_id", -1)
        val questionText = intent.getStringExtra("question_text").orEmpty()
        totalQuestionCount = intent.getIntExtra("count", 3)
        initViews()
        txtQuestion.text = questionText

        val progressQuestion = findViewById<ProgressBar>(R.id.progressQuestion)
        progressQuestion.max = totalQuestionCount
        progressQuestion.progress = currentQuestionCount

        val txtProgress = findViewById<TextView>(R.id.txtProgress)
        txtProgress.text = "$currentQuestionCount/$totalQuestionCount"

        txtQuestion.text = questionText

        // 별표 클릭 로직
        imgStar.setOnClickListener {
            toggleStarStatus()
        }

        // 마이크 클릭 로직 (녹음 시작/중지/전송)
        btnMic.setOnClickListener {
            handleMicClick()
        }

        //  재녹음 버튼 클릭
        btnRefresh.setOnClickListener {
            resetRecording()
        }

        // 다음 질문 클릭
        btnNextQuestion.setOnClickListener {
            moveToNextQuestion()
        }

        setupTitle()
        setupBottomButtons()

        // 초기 애니메이션
        layoutDots.visibility = View.VISIBLE
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

    //  별표 토글
    private fun toggleStarStatus() {
        isSaved = !isSaved
        if (isSaved) {
            imgStar.setImageResource(R.drawable.ic_star_filled)
            imgStar.setColorFilter(Color.parseColor("#FFD700")) // 노란색 별
            tooltipLayout.visibility = View.GONE
            requestToggleSave(true)
        } else {
            imgStar.setImageResource(R.drawable.ic_star_outline)
            imgStar.setColorFilter(Color.WHITE)
            requestToggleSave(false)
        }
    }

    private fun handleMicClick() {
        if (!isRecording && !isReadyToUpload) {
            if (checkPermissions()) startRecording() else requestPermissions()
        } else if (isRecording) {
            stopRecording()
        } else if (isReadyToUpload) {
            uploadAnswer()
        }
    }

    private fun startRecording() {
        audioFile = File(externalCacheDir, "interview_answer.m4a")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                updateUIForRecording()
                simulateSTT()
            } catch (e: IOException) { e.printStackTrace() }
        }
    }

    private fun stopRecording() {
        try { mediaRecorder?.stop() } catch (e: Exception) { e.printStackTrace() }
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
        isReadyToUpload = true
        btnMic.setImageResource(R.drawable.ic_check)
        micBackground.alpha = 1.0f
        btnRefresh.visibility = View.VISIBLE
    }

    private fun uploadAnswer() {
        if (audioFile == null || !audioFile!!.exists()) return

        btnMic.visibility = View.INVISIBLE
        layoutDots.visibility = View.VISIBLE // 분석 중 로딩 표시

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestFile = audioFile!!.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("audio_file", audioFile!!.name, requestFile)

                // 답변 제출
                val response = RetrofitClient.api.submitAnswer(questionId, sessionId, audioPart).execute()

                withContext(Dispatchers.Main) {
                    layoutDots.visibility = View.GONE
                    btnMic.visibility = View.VISIBLE
                    if (response.isSuccessful && response.body() != null) {
                        displayFeedback(response.body()!!)
                    } else {
                        Toast.makeText(this@InterviewStartActivity, "분석 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    layoutDots.visibility = View.GONE
                    btnMic.visibility = View.VISIBLE
                    Toast.makeText(this@InterviewStartActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayFeedback(result: com.example.cac.data.AnswerResponse) {
        layoutFeedbackResult.visibility = View.VISIBLE

        if (isFirstQuestion) {
            tooltipLayout.visibility = View.VISIBLE
        }

        // 현재 질문 번호가 마지막인지 확인하여 버튼 교체
        if (currentQuestionCount >= totalQuestionCount) {
            btnNextQuestion.visibility = View.GONE
            btnFinalMockInterview.visibility = View.VISIBLE
        } else {
            btnNextQuestion.visibility = View.VISIBLE
            btnFinalMockInterview.visibility = View.GONE
        }

        if (currentQuestionCount >= totalQuestionCount) {
            btnNextQuestion.visibility = View.GONE
            btnFinalMockInterview.visibility = View.VISIBLE
        } else {
            btnNextQuestion.visibility = View.VISIBLE
        }

        mainScrollView.post {
            mainScrollView.smoothScrollTo(0, layoutFeedbackResult.top - 100)
        }
    }

    private fun resetRecording() {
        isRecording = false
        isReadyToUpload = false
        typeRunnable?.let { handler.removeCallbacks(it) }
        txtAnswer.text = ""
        btnMic.setImageResource(R.drawable.ic_mic)
        layoutDots.visibility = View.VISIBLE
        scrollAnswer.visibility = View.GONE
        btnRefresh.visibility = View.INVISIBLE
        layoutFeedbackResult.visibility = View.GONE
        micBackground.alpha = 1.0f
    }

    private fun moveToNextQuestion() {
        if (currentQuestionCount < totalQuestionCount) {

            currentQuestionCount++
            isFirstQuestion = false

            // 1. 별표 초기화
            isSaved = false
            imgStar.setImageResource(R.drawable.ic_star_outline)
            imgStar.setColorFilter(Color.WHITE)

            // 2. UI 요소 초기화
            btnNextQuestion.visibility = View.GONE
            btnFinalMockInterview.visibility = View.GONE
            tooltipLayout.visibility = View.GONE
            layoutFeedbackResult.visibility = View.GONE

            // 3. 녹음 상태 초기화
            resetRecording()

            // 4. 상단 그래프 및 숫자 업데이트
            val progressQuestion = findViewById<ProgressBar>(R.id.progressQuestion)
            progressQuestion.progress = currentQuestionCount

            val txtProgress = findViewById<TextView>(R.id.txtProgress)
            txtProgress.text = "$currentQuestionCount/$totalQuestionCount"

            // 5. 화면 맨 위로 스크롤
            mainScrollView.smoothScrollTo(0, 0)

            // TODO: 지혜님, 여기서 실제로 다음 질문 텍스트를 바꿔줘야 합니다!
            // 예: txtQuestion.text = "다음 질문입니다."

        } else {

            Toast.makeText(this, "마지막 질문입니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun requestToggleSave(save: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                //질문 저장 토글 API
                RetrofitClient.api.toggleSaveQuestion(questionId).execute()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }


    private fun simulateSTT() {
        val fullText = "네, 사용자 경험을 개선한 프로젝트 경험이 있습니다..."
        var currentIndex = 0
        scrollAnswer.visibility = View.VISIBLE
        layoutDots.visibility = View.GONE

        typeRunnable = object : Runnable {
            override fun run() {
                if (isRecording && currentIndex < fullText.length) {
                    txtAnswer.append(fullText[currentIndex].toString())
                    currentIndex++
                    scrollAnswer.post { scrollAnswer.scrollTo(0, txtAnswer.bottom) }
                    handler.postDelayed(this, 30)
                }
            }
        }
        handler.post(typeRunnable!!)
    }

    private fun updateUIForRecording() {
        micBackground.alpha = 0.8f
        btnRefresh.visibility = View.INVISIBLE
        txtAnswer.text = ""
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun requestPermissions() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1000)

    private fun startDotAnimation() {
        val dots = listOf(R.id.dot1, R.id.dot2, R.id.dot3).map { findViewById<View>(it) }
        dots.forEachIndexed { index, dot ->
            val animator = ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f)
            animator.duration = 1000
            animator.repeatCount = ValueAnimator.INFINITE
            animator.startDelay = (index * 200).toLong()
            animator.start()
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
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}