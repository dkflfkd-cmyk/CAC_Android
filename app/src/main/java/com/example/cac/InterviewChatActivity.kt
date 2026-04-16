package com.example.cac

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class InterviewChatActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var btnRecord: Button
    private lateinit var btnComplete: Button

    // 녹음 상태 관리: 0=대기, 1=녹음중, 2=녹음완료(중지됨)
    private var recordState = 0

    // 음성 인식 관련
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentUserBubble: TextView? = null // 현재 실시간으로 글자가 찍히는 텍스트뷰

    // 타이핑 애니메이션 관련
    private val handler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null
    private var typingBubble: MaterialCardView? = null
    private var userTypingRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_chat)

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        btnRecord = findViewById(R.id.btnRecord)
        btnComplete = findViewById(R.id.btnComplete)
        findViewById<TextView>(R.id.btnExit).setOnClickListener { finish() }

        // 권한 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // 1. 처음 시작
        simulateInterviewerQuestion("안녕하세요! 지원자님. 간단하게 자기소개 부탁드립니다.")

        // 2. 녹음 버튼 클릭 로직
        btnRecord.setOnClickListener {
            when (recordState) {
                0 -> startRecording() // 처음 녹음 시작
                1 -> stopRecording()  // 녹음 중지
                2 -> startRecording() // 기존꺼 지우고 다시 녹음 시작
            }
        }

        // 3. 완료 버튼 클릭 로직
        btnComplete.setOnClickListener {
            if (currentUserBubble?.text.isNullOrEmpty()) {
                Toast.makeText(this, "답변을 녹음해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 상태 초기화
            stopRecording()
            currentUserBubble = null
            recordState = 0
            btnRecord.text = "녹음하기"

            // 서버 전송 로직 들어갈 자리 (현재는 스킵)

            // 다음 질문 시뮬레이션
            simulateInterviewerQuestion("자기소개 잘 들었습니다. 그렇다면 본인의 가장 큰 장점은 무엇인가요?")
        }
    }

    // ==========================================
    // UI 및 채팅 버블 생성 로직
    // ==========================================

    private fun simulateInterviewerQuestion(question: String) {
        showTypingAnimation() // 점점점 애니메이션 시작

        // 2초 뒤에 질문 등장
        handler.postDelayed({
            removeTypingAnimation()
            addInterviewerBubble(question)

            // 질문이 끝나면 유저 답변 대기용 버블(점점점) 생성
            addWaitingUserBubble()
        }, 2000)
    }

    private fun addInterviewerBubble(text: String) {
        val card = createBubbleCard(isInterviewer = true)
        val textView = createBubbleText(text, isInterviewer = true)
        card.addView(textView)
        chatContainer.addView(card)
        scrollToBottom()
    }

    // 유저가 녹음하기 전 대기하는 (...)
    private fun addWaitingUserBubble() {
        val card = createBubbleCard(isInterviewer = false)
        val textView = createBubbleText(".", isInterviewer = false) // 시작은 점 1개로
        currentUserBubble = textView
        card.addView(textView)
        chatContainer.addView(card)
        scrollToBottom()


        var dotCount = 1
        userTypingRunnable = object : Runnable {
            override fun run() {
                dotCount = (dotCount % 3) + 1
                textView.text = ".".repeat(dotCount)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(userTypingRunnable!!)
    }


    // 면접관 타이핑 애니메이션 (...)
    private fun showTypingAnimation() {
        // 1. null 걱정이 없는 새로운 변수(bubble)를 만들어 조립합니다.
        val bubble = createBubbleCard(isInterviewer = true)
        val textView = createBubbleText(".", isInterviewer = true)

        bubble.addView(textView)
        chatContainer.addView(bubble) // 💡 이제 빨간 줄 에러가 나지 않습니다!

        // 2. 나중에 애니메이션을 지울 수 있도록 전역 변수에 담아둡니다.
        typingBubble = bubble

        scrollToBottom()

        var dotCount = 1
        typingRunnable = object : Runnable {
            override fun run() {
                dotCount = (dotCount % 3) + 1
                textView.text = ".".repeat(dotCount)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(typingRunnable!!)
    }

    private fun removeTypingAnimation() {
        typingRunnable?.let { handler.removeCallbacks(it) }
        typingBubble?.let { chatContainer.removeView(it) }
    }

    // 공통 말풍선 UI 생성기
    private fun createBubbleCard(isInterviewer: Boolean): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = 24f
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(if (isInterviewer) Color.WHITE else Color.parseColor("#3950E7"))

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (isInterviewer) Gravity.START else Gravity.END
                bottomMargin = 30
            }
            layoutParams = lp
        }
        return card
    }

    private fun createBubbleText(text: String, isInterviewer: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(if (isInterviewer) Color.parseColor("#333333") else Color.WHITE)
            setPadding(40, 24, 40, 24)
        }
    }

    private fun scrollToBottom() {
        chatScrollView.post { chatScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    // ==========================================
    // STT (음성 인식) 로직
    // ==========================================

    private fun startRecording() {

        userTypingRunnable?.let { handler.removeCallbacks(it) }

        recordState = 1
        btnRecord.text = "녹음 중지"
        btnRecord.setBackgroundColor(Color.parseColor("#FF5252")) // 빨간색으로 변경


        currentUserBubble?.text = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            //말하는 도중 실시간으로 텍스트 업데이트
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentUserBubble?.text = matches[0]
                    scrollToBottom()
                }
            }

            // 말하기 완료 시 최종 텍스트
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    currentUserBubble?.text = matches[0]
                    stopRecording() // 자동으로 멈춤
                }
            }

            override fun onError(error: Int) {
                stopRecording()
                Toast.makeText(this@InterviewChatActivity, "음성 인식 오류 발생", Toast.LENGTH_SHORT).show()
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun stopRecording() {
        recordState = 2
        btnRecord.text = "다시 녹음하기"
        btnRecord.setBackgroundColor(Color.parseColor("#3950E7")) // 다시 파란색
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        removeTypingAnimation()
    }
}