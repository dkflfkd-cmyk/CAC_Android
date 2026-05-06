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
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class InterviewChatActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var btnRecord: View
    private lateinit var btnComplete: View
    private lateinit var btnReplay: View

    private var recordState = 0 // 0:대기, 1:녹음중, 2:녹음완료(중단)
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentUserBubble: TextView? = null
    private var currentUserCard: MaterialCardView? = null // 재녹음 시 크기 초기화용
    private val handler = Handler(Looper.getMainLooper())
    private var userTypingRunnable: Runnable? = null
    private var typingRunnable: Runnable? = null
    private var typingBubble: View? = null

    // 문장 누적을 위한 임시 저장 변수
    private var savedAnswerText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_chat)

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        btnRecord = findViewById(R.id.btnRecord)
        btnComplete = findViewById(R.id.btnComplete)
        btnReplay = findViewById(R.id.btnReplay)

        // 나가기 버튼 로직
        findViewById<TextView>(R.id.btnExit).setOnClickListener {
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        // 녹음버튼만 노출, 초기 질문
        resetButtonVisibility()
        simulateInterviewerQuestion("사용자 경험을 개선한 프로젝트 사례를 설명해주세요.")

        // 녹음시작 버튼
        btnRecord.setOnClickListener {
            when (recordState) {
                0 -> startRecording() // 초기 대기 상태에서 누르면 녹음 시작
                1 -> stopRecording()  // 녹음 중일 때 누르면 중단
                2 -> startRecording() // 중단된 상태에서 다시 누르면 이어 녹음 재개
            }
        }

        // 확인 버튼
        btnComplete.setOnClickListener {
            if (currentUserBubble?.text.isNullOrEmpty() || currentUserBubble?.text!!.contains(".")) {
                Toast.makeText(this, "답변을 녹음해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            stopRecording()
            recordState = 0
            currentUserBubble = null
            currentUserCard = null
            savedAnswerText = "" // 다음 질문으로 가므로 누적 텍스트 초기화

            resetButtonVisibility()
            simulateInterviewerQuestion("다음 질문입니다. 프로젝트에서 본인의 역할은 무엇이었나요?")
        }

        // 재녹음 버튼
        btnReplay.setOnClickListener {
            if (recordState == 0 && currentUserBubble == null) return@setOnClickListener

            recordState = 0
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            userTypingRunnable?.let { handler.removeCallbacks(it) }

            // 누적 텍스트 및 말풍선 초기화
            savedAnswerText = ""
            currentUserCard?.layoutParams?.width = LinearLayout.LayoutParams.WRAP_CONTENT
            currentUserBubble?.text = "..."
            currentUserCard?.requestLayout()

            btnReplay.visibility = View.INVISIBLE
            btnComplete.visibility = View.INVISIBLE

            startRecording()
            Toast.makeText(this, "다시 녹음을 시작합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetButtonVisibility() {
        btnReplay.visibility = View.INVISIBLE
        btnComplete.visibility = View.INVISIBLE
    }

    private fun startRecording() {
        userTypingRunnable?.let { handler.removeCallbacks(it) }

        if (currentUserBubble == null) {
            addUserBubble("...")
            savedAnswerText = ""
        } else if (recordState == 2) {
            // 녹음을 일시중단했다가 다시 이어하는 경우 기존 텍스트를 기준으로
            savedAnswerText = currentUserBubble?.text.toString()
        }

        recordState = 1
        (btnRecord as MaterialCardView).setCardBackgroundColor(Color.parseColor("#FF5252"))

        var dotCount = 1
        userTypingRunnable = object : Runnable {
            override fun run() {
                if (currentUserBubble?.text != null && !currentUserBubble?.text!!.contains(".")) {
                    return
                }
                dotCount = (dotCount % 3) + 1
                currentUserBubble?.text = ".".repeat(dotCount)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(userTypingRunnable!!)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onPartialResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        handler.removeCallbacks(userTypingRunnable!!)

                        // 현재 인식 중인 중간 텍스트를 기존 누적된 텍스트 뒤에 붙여서 노출
                        val partialText = matches[0]
                        if (savedAnswerText.isEmpty()) {
                            currentUserBubble?.text = partialText
                        } else {
                            currentUserBubble?.text = "$savedAnswerText $partialText"
                        }
                        scrollToBottom()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val finalText = matches[0]
                        if (savedAnswerText.isEmpty()) {
                            savedAnswerText = finalText
                        } else {
                            savedAnswerText = "$savedAnswerText $finalText"
                        }
                        currentUserBubble?.text = savedAnswerText
                    }


                    if (recordState == 1) {
                        startListening(intent)
                    }
                }

                override fun onError(error: Int) {
                    // 침묵 끊기 이어서
                    if (recordState == 1) {
                        startListening(intent)
                    } else {
                        stopRecording()
                    }
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(intent)
        }
    }

    private fun stopRecording() {
        recordState = 2
        (btnRecord as MaterialCardView).setCardBackgroundColor(Color.parseColor("#4A61ED"))
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        userTypingRunnable?.let { handler.removeCallbacks(it) }

        btnReplay.visibility = View.VISIBLE
        btnComplete.visibility = View.VISIBLE
    }

    private fun simulateInterviewerQuestion(question: String) {
        showTypingAnimation()
        handler.postDelayed({
            removeTypingAnimation()
            addInterviewerBubble(question)
        }, 2000)
    }

    private fun showTypingAnimation() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }


        val profileLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_interviewer)
            imageTintList = null
            layoutParams = LinearLayout.LayoutParams(60, 60)
        }

        // 면접관 이름 텍스트
        val name = TextView(this).apply {
            text = "면접관"
            textSize = 12f
            setPadding(12, 0, 0, 0)
            setTextColor(Color.parseColor("#777777"))
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        profileLayout.addView(icon)
        profileLayout.addView(name)

        val card = MaterialCardView(this).apply {
            radius = 32f
            strokeWidth = 3
            strokeColor = Color.parseColor("#BDC5F3")
            setCardBackgroundColor(Color.WHITE)
            elevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val tv = TextView(context).apply {
                text = "."
                setPadding(40, 25, 40, 25)
                setTextColor(Color.parseColor("#333333"))
            }
            addView(tv)
        }

        layout.addView(profileLayout)
        layout.addView(card)
        chatContainer.addView(layout)
        typingBubble = layout
        scrollToBottom()

        var dot = 1
        typingRunnable = object : Runnable {
            override fun run() {
                dot = (dot % 3) + 1
                ((card.getChildAt(0)) as TextView).text = ".".repeat(dot)
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

    private fun addInterviewerBubble(text: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val profileLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_interviewer)
            imageTintList = null
            layoutParams = LinearLayout.LayoutParams(60, 60)
        }

        val name = TextView(this).apply {
            this.text = "면접관"
            textSize = 12f
            setPadding(12, 0, 0, 0)
            setTextColor(Color.parseColor("#777777"))
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        profileLayout.addView(icon)
        profileLayout.addView(name)

        val card = MaterialCardView(this).apply {
            radius = 30f
            strokeColor = Color.parseColor("#BDC5F3")
            strokeWidth = 3
            setCardBackgroundColor(Color.WHITE)
            elevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val tv = TextView(context).apply {
                this.text = text
                setTextColor(Color.parseColor("#333333"))
                setPadding(40, 30, 40, 30)
                textSize = 15f
                maxWidth = (resources.displayMetrics.widthPixels * 0.7).toInt()
            }
            addView(tv)
        }

        layout.addView(profileLayout)
        layout.addView(card)
        chatContainer.addView(layout)
        scrollToBottom()
    }

    private fun addUserBubble(text: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(0, 0, 0, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val nameText = TextView(this).apply {
            this.text = "홍길동"
            textSize = 12f
            setTextColor(Color.parseColor("#777777"))
            setPadding(0, 0, 0, 8)
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
        }

        val card = MaterialCardView(this).apply {
            radius = 30f
            setCardBackgroundColor(Color.parseColor("#F0F2FF"))
            strokeColor = Color.parseColor("#BDC5F3")
            strokeWidth = 2
            elevation = 0f

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
            currentUserCard = this
        }

        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#333333"))
            setPadding(40, 30, 40, 30)
            textSize = 15f
            setSingleLine(false)
            maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            currentUserBubble = this
        }

        card.addView(tv)
        layout.addView(nameText)
        layout.addView(card)
        chatContainer.addView(layout)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        chatScrollView.post { chatScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}