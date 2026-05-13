package com.example.cac

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import android.media.MediaRecorder
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
import com.example.cac.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import com.example.cac.data.AnswerResponse
import com.example.cac.data.GeneratedQuestion
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.example.cac.network.AudioAnswerResponse




class InterviewChatActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var btnRecord: View
    private lateinit var btnComplete: View
    private lateinit var btnReplay: View

    private var recordState = 0 // 0:대기, 1:녹음중, 2:중단됨
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    private var currentUserBubble: TextView? = null
    private var currentUserCard: MaterialCardView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var userTypingRunnable: Runnable? = null
    private var typingRunnable: Runnable? = null
    private var typingBubble: View? = null
    private var questionId: Int = 1

    private var currentQuestionText: String = ""
    private val interviewHistory = mutableListOf<Map<String, String>>()

    private var savedAnswerText = ""
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

        // 2. 초기 세팅
        checkPermission()
        fetchUserInfo()
        resetButtonVisibility()

        // 3. 데이터 초기화 및 질문 생성 로직
        sessionId = intent.getIntExtra("session_id", -1)


        if (sessionId != -1) {
            addInterviewerBubble("질문을 생성하고 있습니다. 잠시만 기다려 주세요...")
            prepareAndFetchQuestions()

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
            if (audioFile == null || !audioFile!!.exists()) {
                Toast.makeText(this, "답변을 녹음해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("InterviewTest", "전송할 텍스트: $savedAnswerText")
            uploadAudioAnswer(audioFile!!)
        }

        btnReplay.setOnClickListener { reRecord() }
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
        if (currentUserBubble == null) addUserBubble("...")
        recordState = 1
        (btnRecord as MaterialCardView).setCardBackgroundColor(Color.parseColor("#FF5252"))
        startUserTypingAnimation()

        try {

            audioFile = File(externalCacheDir, "interview_audio.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "녹음 장치 초기화 실패", Toast.LENGTH_SHORT).show()
        }
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
        recordState = 2
        (btnRecord as MaterialCardView).setCardBackgroundColor(Color.parseColor("#4A61ED"))
        mediaRecorder?.apply {
            try { stop() } catch(e: Exception) {}
            release()
        }
        mediaRecorder = null
        speechRecognizer?.stopListening()
        userTypingRunnable?.let { handler.removeCallbacks(it) }

        btnComplete.visibility = View.VISIBLE
        btnReplay.visibility = View.VISIBLE
    }

    private fun reRecord() {
        recordState = 0
        speechRecognizer?.destroy()
        speechRecognizer = null
        mediaRecorder?.release()
        mediaRecorder = null
        userTypingRunnable?.let { handler.removeCallbacks(it) }

        savedAnswerText = ""
        currentUserBubble?.text = "..."
        resetButtonVisibility()
        startRecording()
    }

    private fun startSTT() {
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val newText = matches[0]
                        savedAnswerText = if (savedAnswerText.isEmpty()) newText else "$savedAnswerText $newText"
                        currentUserBubble?.text = savedAnswerText
                    }
                    if (recordState == 1) startListening(sttIntent)
                }
                override fun onPartialResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        handler.removeCallbacks(userTypingRunnable!!)
                        val partialText = matches[0]
                        currentUserBubble?.text = if (savedAnswerText.isEmpty()) partialText else "$savedAnswerText $partialText"
                    }
                }
                override fun onError(error: Int) { if (recordState == 1) startListening(sttIntent) }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            startListening(sttIntent)
        }
    }

    private fun uploadAudioAnswer(file: File) {
        // 1. 오디오 파일 생성 (기존과 동일)
        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("audio_file", file.name, requestFile)

        // 2. 새로운 API 호출 (텍스트 파트들은 이제 필요 없는지 서버 확인 필요, 일단 오디오만 전송)
        RetrofitClient.api.submitInterviewAudio(sessionId, audioPart)
            .enqueue(object : Callback<AudioAnswerResponse> {
                override fun onResponse(call: Call<AudioAnswerResponse>, response: Response<AudioAnswerResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        body?.let {
                            // 사용자의 음성이 텍스트로 변환된 결과 표시
                            addUserBubble(it.stt_text)

                            // 다음 면접 질문 표시
                            if (!it.is_finished) {
                                addInterviewerBubble(it.next_question)
                                currentQuestionText = it.next_question
                            } else {
                                showDonePanelAnimation() // 면접 종료 처리
                            }
                        }
                    } else {
                        Log.e("API_ERROR", "오류 발생: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AudioAnswerResponse>, t: Throwable) {
                    Log.e("API_FAILURE", "통신 실패: ${t.message}")
                }
            })
    }
    private fun fetchNextQuestionFromServer() {
        showTypingAnimation()

        RetrofitClient.api.getQuestions(sessionId).enqueue(object : retrofit2.Callback<List<com.example.cac.data.GeneratedQuestion>> {
            override fun onResponse(
                call: retrofit2.Call<List<com.example.cac.data.GeneratedQuestion>>,
                response: retrofit2.Response<List<com.example.cac.data.GeneratedQuestion>>
            ) {
                removeTypingAnimation()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val questions = response.body()!!


                    val nextQuestionData = questions.find { it.order_num == questionId }
                        ?: questions.firstOrNull() // 못찾으면 첫번째꺼라도

                    if (nextQuestionData != null) {
                        addInterviewerBubble(nextQuestionData.question_text)

                    } else {
                        addInterviewerBubble("준비된 모든 질문이 끝났습니다. 수고하셨습니다!")
                    }
                }
            }

            override fun onFailure(call: retrofit2.Call<List<com.example.cac.data.GeneratedQuestion>>, t: Throwable) {
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
                this.text = text // 파라미터로 받은 질문 텍스트 삽입
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

    private fun addUserBubble(text: String): TextView {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 40)
            gravity = Gravity.END
        }

        val nameText = TextView(this).apply {

            this.text = userName
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 0, 10, 8)
            gravity = Gravity.END
        }

        var targetTv: TextView? = null // 리턴할 변수

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

            val tv = TextView(context).apply {
                this.text = text
                setPadding(45, 30, 45, 30)
                setTextColor(Color.parseColor("#333333"))
                textSize = 15f
                maxWidth = (resources.displayMetrics.widthPixels * 0.75).toInt()
                targetTv = this
            }
            addView(tv)
        }

        layout.addView(nameText)
        layout.addView(card)
        chatContainer.addView(layout)
        scrollToBottom()

        currentUserBubble = targetTv
        return targetTv!!
    }

    private fun startUserTypingAnimation() {
        var dot = 1

        currentUserCard?.layoutParams?.width = LinearLayout.LayoutParams.WRAP_CONTENT

        userTypingRunnable = object : Runnable {
            override fun run() {

                if (recordState != 1) return

                dot = (dot % 3) + 1
                currentUserBubble?.text = ".".repeat(dot)


                currentUserCard?.requestLayout()
                handler.postDelayed(this, 500)
            }
        }
        handler.post(userTypingRunnable!!)
    }

    private fun simulateInterviewerQuestion(text: String) {
        showTypingAnimation()
        handler.postDelayed({
            removeTypingAnimation()
            addInterviewerBubble(text)
        }, 1500)
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
        btnReplay.visibility = View.INVISIBLE
        btnComplete.visibility = View.INVISIBLE
    }

    private fun scrollToBottom() { chatScrollView.post { chatScrollView.fullScroll(ScrollView.FOCUS_DOWN) } }

    private fun showDonePanelAnimation() {
        val donePanel = findViewById<LinearLayout>(R.id.layoutDonePanel)

        donePanel.visibility = View.VISIBLE
        donePanel.alpha = 0.0f // 처음에 투명하게

        donePanel.animate()
            .alpha(1.0f) // 선명하게
            .setDuration(1500) // 1.5초 동안
            .withEndAction {
                // 애니메이션이 끝나고 2초 뒤에 결과 화면으로 이동
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this@InterviewChatActivity, InterviewResultActivity::class.java)
                    intent.putExtra("session_id", sessionId)
                    startActivity(intent)
                    finish()
                }, 2000)
            }
            .start()
    }
    private fun checkPermission() {
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
}