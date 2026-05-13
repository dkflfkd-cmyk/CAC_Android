package com.example.cac

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.QuestionItem
import com.example.cac.network.RetrofitClient
import com.example.cac.ui.adapter.QuestionListAdapter
import kotlinx.coroutines.Dispatchers
import android.animation.ObjectAnimator
import android.animation.ValueAnimator

import android.widget.LinearLayout
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class QuestionListActivity : AppCompatActivity() {

    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var progressBar: ProgressBar
    private lateinit var txtProgress: TextView

    private var loadingHandler: Handler? = null

    private lateinit var layoutDots: LinearLayout
    private var loadingRunnable: Runnable? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_list)




        // 뷰 초기화
        progressBar = findViewById(R.id.progressQuestion)
        txtProgress = findViewById(R.id.txtProgress)

        layoutDots = findViewById(R.id.layoutDots)
        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        progressBar = findViewById(R.id.progressQuestion)
        txtProgress = findViewById(R.id.txtProgress)
        startLoadingAnimation()

        // Intent 데이터 받기
        val sessionId = intent.getIntExtra("session_id", -1)
        val job = intent.getStringExtra("job").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val isFromSetup = intent.getBooleanExtra("is_from_setup", false) // 세팅 화면에서 온 건지 확인
        val count = intent.getIntExtra("count", 3)


        // 상단 텍스트 정보 세팅
        findViewById<TextView>(R.id.txtJobTitle).text = job
        findViewById<TextView>(R.id.txtSubInfo).text = type
        updateProgress(0, count)

        if (sessionId != -1) {

// AI 질문 생성

            RetrofitClient.api.generateQuestions(sessionId).enqueue(object : retrofit2.Callback<List<com.example.cac.data.GeneratedQuestion>> {

                override fun onResponse(
                    call: retrofit2.Call<List<com.example.cac.data.GeneratedQuestion>>,
                    response: retrofit2.Response<List<com.example.cac.data.GeneratedQuestion>>
                ) {

                    if (response.isSuccessful) {

                        fetchQuestionsFromServer(sessionId, count)

                    }

                }

                override fun onFailure(call: retrofit2.Call<List<com.example.cac.data.GeneratedQuestion>>, t: Throwable) {
                    android.util.Log.e("API_CHECK", "통신 오류", t)
                }

            })

        }

        setupTitle()
        setupBottomButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 다시받기 버튼
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRecreateQuestions).setOnClickListener {

            val prefs = getSharedPreferences("InterviewPrefs", MODE_PRIVATE)
            prefs.edit().remove("last_session_id").apply()


            val intent = Intent(this, QuestionSetupActivity::class.java).apply {
                // 기존에 선택했던 값들은 기억하도록 넘겨줍니다.
                putExtra("PREV_JOB", job)
                putExtra("PREV_TYPE", type)
                putExtra("PREV_COUNT", count)


                putExtra("is_retry", true)

                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }



    private fun startLoadingAnimation() {
        layoutDots.visibility = View.VISIBLE
        val dots = arrayOf(dot1, dot2, dot3)

        dots.forEachIndexed { index, dot ->

            ObjectAnimator.ofFloat(dot, "alpha", 0.3f, 1f, 0.3f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                startDelay = (index * 200).toLong()
                start()
            }

            ObjectAnimator.ofFloat(dot, "translationY", 0f, -15f, 0f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                startDelay = (index * 200).toLong()
                start()
            }
        }
    }

    private fun stopLoadingAnimation() {

        loadingHandler?.removeCallbacks(loadingRunnable ?: return)

        layoutDots.visibility = View.GONE
    }


    private fun updateProgress(current: Int, total: Int) {
        progressBar.max = total
        progressBar.progress = current
        txtProgress.text = "$current/$total"
    }

    private fun fetchQuestionsFromServer(sessionId: Int, totalCount: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getQuestions(sessionId).execute()
                if (response.isSuccessful && response.body() != null) {
                    val rawList = response.body()!!

                    withContext(Dispatchers.Main) {
                        // 1. 로딩 애니메이션 중지
                        stopLoadingAnimation()

                        // 2. 서버 데이터 중복 제거 및 정렬

                        val questionList = rawList.distinctBy { it.id }
                            .sortedBy { it.order_num }

                        val currentTotal = questionList.size
                        updateProgress(0, currentTotal)

                        // UI용 리스트 아이템 생성
                        val questionItems = questionList.mapIndexed { index, item ->
                            QuestionItem(
                                id = item.id,
                                number = index + 1,
                                question = item.question_text
                            )
                        }

                        // 5. 리사이클러뷰 설정
                        val rv = findViewById<RecyclerView>(R.id.rvQuestionList)
                        rv.layoutManager = LinearLayoutManager(this@QuestionListActivity)

                        // 6. 어댑터 연결 및 클릭 이벤트 설정
                        rv.adapter = QuestionListAdapter(questionItems) { selectedItem ->
                            // 클릭한 아이템의 위치(Index) 찾기
                            val clickedIndex = questionItems.indexOf(selectedItem)

                            val intent = Intent(this@QuestionListActivity, InterviewStartActivity::class.java).apply {
                                // 전체 질문 텍스트만 뽑아서 리스트로 전송 (자동 다음 질문용)
                                val allQuestions = ArrayList(questionItems.map { it.question })

                                val allIds = ArrayList(questionItems.map { it.id })
                                putIntegerArrayListExtra("question_id_list", allIds)
                                putStringArrayListExtra("question_list", allQuestions)
                                putExtra("current_index", clickedIndex)

                                // 세션 및 질문 정보 전송
                                putExtra("session_id", sessionId)
                                putExtra("question_id", selectedItem.id)
                                putExtra("question_text", selectedItem.question)
                                putExtra("count", currentTotal)
                            }
                            startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("API_CHECK", "통신 실패", e)
                withContext(Dispatchers.Main) {
                    stopLoadingAnimation()
                    Toast.makeText(this@QuestionListActivity, "질문을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
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
        spannable.setSpan(ForegroundColorSpan(gray), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 8, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 9, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable
    }
}