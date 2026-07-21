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
import android.util.Log
import com.example.cac.data.AnswerResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InterviewStartActivity : AppCompatActivity() {

    private var fullAnswer: String = ""
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var scrollAnswer: NestedScrollView
    private lateinit var btnMic: ImageButton
    private lateinit var micBackground: View

    private var questionIdList: ArrayList<Int> = arrayListOf()
    private lateinit var layoutDots: View
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var txtAnswer: TextView
    private lateinit var txtProgress: TextView
    private lateinit var progressQuestion: ProgressBar

    private lateinit var txtAnalyzing: TextView
    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null
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

    private var questionList: ArrayList<String> = arrayListOf()
    private var currentIndex: Int = 0
    private var totalQuestionCount: Int = 3

    // [추가] 각 질문들의 답변 상태(True/False)를 저장할 리스트 변수
    private var answeredStateList: ArrayList<Boolean> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_start)

        initViews()

        // 전달받은 데이터 읽기
        questionList = intent.getStringArrayListExtra("question_list") ?: arrayListOf()
        questionIdList = intent.getIntegerArrayListExtra("question_id_list") ?: arrayListOf()
        currentIndex = intent.getIntExtra("current_index", 0)
        sessionId = intent.getIntExtra("session_id", -1)
        totalQuestionCount = intent.getIntExtra("count", 3)
        if (questionList.isNotEmpty()) {
            totalQuestionCount = questionList.size
        }

    //답변 여부
        val receivedStates = intent.getBooleanArrayExtra("answered_state_list")?.toList()
        if (receivedStates != null) {
            answeredStateList = ArrayList(receivedStates)
        } else {

            answeredStateList = ArrayList(Collections.nCopies(totalQuestionCount, false))
        }

        if (questionIdList.isNotEmpty() && currentIndex < questionIdList.size) {
            questionId = questionIdList[currentIndex]
        } else {
            questionId = intent.getIntExtra("question_id", -1)
        }

        android.util.Log.d("INTERVIEW_CHECK", "현재 질문 순서: ${currentIndex + 1}, 서버 전송 ID: $questionId")

        if (questionList.isNotEmpty() && currentIndex < questionList.size) {
            txtQuestion.text = questionList[currentIndex]
        } else {
            txtQuestion.text = intent.getStringExtra("question_text").orEmpty()
        }

        // 상단 프로그레스 바 및 텍스트 업데이트 로직 분리 호출
        updateProgressGraph()

        initSpeechRecognizer()

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
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        txtAnswer = findViewById(R.id.txtAnswer)
        txtProgress = findViewById(R.id.txtProgress)
        progressQuestion = findViewById(R.id.progressQuestion)
        txtAnalyzing = findViewById(R.id.txtAnalyzing)
        btnRefresh = findViewById(R.id.btnRefresh)
        layoutFeedbackResult = findViewById(R.id.layoutFeedbackResult)
        txtQuestion = findViewById(R.id.txtQuestion)
        imgStar = findViewById(R.id.imgStar)
        tooltipLayout = findViewById(R.id.tooltipLayout)
        btnNextQuestion = findViewById(R.id.btnNextQuestion)
        btnFinalMockInterview = findViewById(R.id.btnFinalMockInterview)

        btnFinalMockInterview.setOnClickListener {
            val intent = Intent(this, InterviewActivity::class.java)
            intent.putExtra("session_id", sessionId)
            startActivity(intent)
        }
    }


    private fun updateProgressGraph() {

        val completedCount = answeredStateList.count { it }
        progressQuestion.max = totalQuestionCount
        progressQuestion.progress = completedCount
        txtProgress.text = "$completedCount/$totalQuestionCount"


    }

    private fun moveToNextQuestion() {
        val nextIndex = nextUnansweredIndex()
        if (nextIndex != null && nextIndex in questionList.indices) {
            val intent = Intent(this, InterviewStartActivity::class.java).apply {
                putStringArrayListExtra("question_list", questionList)
                putIntegerArrayListExtra("question_id_list", questionIdList)

                putExtra("answered_state_list", answeredStateList.toBooleanArray())
                putExtra("current_index", nextIndex)
                putExtra("session_id", sessionId)
                putExtra("count", totalQuestionCount)
            }
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "마지막 질문입니다.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun nextUnansweredIndex(): Int? {
        if (answeredStateList.isEmpty()) return null
        for (index in (currentIndex + 1) until answeredStateList.size) {
            if (!answeredStateList[index]) return index
        }
        for (index in 0..currentIndex.coerceAtMost(answeredStateList.lastIndex)) {
            if (!answeredStateList[index]) return index
        }
        return null
    }

    private fun isAllQuestionsAnswered(): Boolean {
        return answeredStateList.isNotEmpty() && answeredStateList.all { it }
    }

    private fun ArrayList<Boolean>.getBooleanArray(): BooleanArray {
        val array = BooleanArray(this.size)
        for (i in this.indices) {
            array[i] = this[i]
        }
        return array
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
            isRecording = true
            txtAnswer.text = "답변을 녹음 중입니다..."
            scrollAnswer.visibility = View.VISIBLE
            btnMic.alpha = 0.5f
            initSpeechRecognizer()
            speechRecognizer.startListening(recognitionIntent)
        } catch (e: Exception) { Log.e("AUDIO", "녹음 시작 실패", e) }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) { e.printStackTrace() }
        isRecording = false
        speechRecognizer.stopListening()
        btnMic.visibility = View.INVISIBLE
        layoutDots.visibility = View.VISIBLE
        uploadAnswer()
    }

    private fun uploadAnswer() {
        val file = audioFile ?: return
        startAnalyzingUI()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestFile = file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("audio_file", file.name, requestFile)
                val response = RetrofitClient.api.submitAnswer(questionId, sessionId, filePart).execute()

                withContext(Dispatchers.Main) {
                    stopAnalyzingUI()
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        txtAnswer.text = data.stt_text
                        btnMic.setImageResource(R.drawable.ic_check)
                        btnMic.visibility = View.VISIBLE
                        btnMic.alpha = 1.0f

                        // 답변 업로드 성공 시 현재 질문 인덱스의 상태를 true로 변경
                        if (currentIndex < answeredStateList.size) {
                            answeredStateList[currentIndex] = true
                        }
                        // 그래프 실시간 최신화 반영
                        updateProgressGraph()

                        displayFeedback(data)
                    } else { resetUIOnError("분석 실패") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { stopAnalyzingUI(); resetUIOnError("네트워크 오류") }
            }
        }
    }

    private fun startAnalyzingUI() {
        txtAnswer.text = ""
        btnMic.visibility = View.GONE
        layoutDots.visibility = View.GONE
        txtAnalyzing.visibility = View.VISIBLE
        val loadingDots = arrayOf(".", "..", "...", "")
        var count = 0
        loadingHandler = Handler(Looper.getMainLooper())
        loadingRunnable = object : Runnable {
            override fun run() {
                txtAnalyzing.text = "분석 중${loadingDots[count % 4]}"
                count++
                loadingHandler?.postDelayed(this, 500)
            }
        }
        loadingHandler?.post(loadingRunnable!!)
    }

    private fun stopAnalyzingUI() {
        loadingHandler?.removeCallbacks(loadingRunnable!!)
        txtAnalyzing.visibility = View.GONE
    }

    private fun displayFeedback(result: AnswerResponse) {
        layoutFeedbackResult.visibility = View.VISIBLE
        findViewById<TextView>(R.id.txtSpeedValue).text = result.audio_scores?.speed_feedback ?: "데이터 없음"
        findViewById<TextView>(R.id.txtContentValue).text = "${result.feedback?.strength}\n\n${result.feedback?.weakness}"
        findViewById<TextView>(R.id.txtTipValue).text = result.feedback?.suggestion ?: "팁 없음"

        if (isAllQuestionsAnswered()) {
            btnNextQuestion.visibility = View.GONE
            btnFinalMockInterview.visibility = View.VISIBLE
        } else {
            btnNextQuestion.visibility = View.VISIBLE
            btnFinalMockInterview.visibility = View.GONE
        }
        mainScrollView.post { mainScrollView.smoothScrollTo(0, layoutFeedbackResult.top) }
    }

    private fun resetUIOnError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        layoutDots.visibility = View.GONE
        btnMic.visibility = View.VISIBLE
        btnMic.setImageResource(R.drawable.ic_mic)
        txtAnswer.text = "다시 녹음해 주세요."
    }

    private fun resetRecording() {
        fullAnswer = ""
        isRecording = false
        isReadyToUpload = false
        txtAnswer.text = ""
        btnMic.setImageResource(R.drawable.ic_mic)
        btnMic.visibility = View.VISIBLE
        layoutFeedbackResult.visibility = View.GONE
    }

    private fun toggleStarStatus() {
        val token = SessionManager.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        isSaved = !isSaved
        updateStarUI()

        RetrofitClient.api.toggleSaveQuestion("Bearer $token", questionId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (!response.isSuccessful) { isSaved = !isSaved; updateStarUI() }
            }
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) { isSaved = !isSaved; updateStarUI() }
        })
    }

    private fun updateStarUI() {
        imgStar.setImageResource(if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        imgStar.setColorFilter(if (isSaved) Color.parseColor("#FFD700") else Color.WHITE)
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) txtAnswer.text = matches[0]
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) fullAnswer = matches[0]
            }
            override fun onError(error: Int) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
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
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#3950E7")), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener { startActivity(Intent(this, DashboardActivity::class.java)) }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
    }

    override fun onDestroy() { super.onDestroy(); speechRecognizer.destroy() }
}
