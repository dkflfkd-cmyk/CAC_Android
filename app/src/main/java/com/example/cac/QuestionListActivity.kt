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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuestionListActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var txtProgress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_list)

        // 뷰 초기화
        progressBar = findViewById(R.id.progressQuestion)
        txtProgress = findViewById(R.id.txtProgress)

        // Intent 데이터 받기
        val sessionId = intent.getIntExtra("session_id", -1)
        val job = intent.getStringExtra("job").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val count = intent.getIntExtra("count", 3)

        // 상단 텍스트 정보 세팅
        findViewById<TextView>(R.id.txtJobTitle).text = job
        findViewById<TextView>(R.id.txtSubInfo).text = type
        updateProgress(0, count) // 초기 0/count 상태

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
            val intent = Intent(this, QuestionSetupActivity::class.java).apply {
                putExtra("PREV_JOB", job)
                putExtra("PREV_TYPE", type)
                putExtra("PREV_COUNT", count)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun updateProgress(current: Int, total: Int) {
        progressBar.max = total
        progressBar.progress = current
        txtProgress.text = "$current/$total"
    }

    private fun fetchQuestionsFromServer(sessionId: Int, count: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.api.getQuestions(sessionId).execute()
                if (response.isSuccessful && response.body() != null) {
                    val questionList = response.body()!!


                    withContext(Dispatchers.Main) {
                        val questionItems = questionList.mapIndexed { index, item ->
                            QuestionItem(

                                id = item.id,
                                number = index + 1,

                                question = item.question_text
                            )
                        }

                        val rv = findViewById<RecyclerView>(R.id.rvQuestionList)
                        rv.layoutManager = LinearLayoutManager(this@QuestionListActivity)




                        rv.adapter = QuestionListAdapter(questionItems) { selectedItem ->

                            android.util.Log.d("ID_CHECK", "클릭한 질문 ID: ${selectedItem.id}")
                            android.util.Log.d("ID_CHECK", "클릭한 질문 내용: ${selectedItem.question}")

                            val intent = Intent(this@QuestionListActivity, InterviewStartActivity::class.java).apply {
                                putExtra("session_id", sessionId)
                                putExtra("question_id", selectedItem.id) // it.id가 아니라 selectedItem.id입니다.
                                putExtra("question_text", selectedItem.question)
                                putExtra("count", count)
                            }
                            startActivity(intent)
                        }

                        // 전체 개수에 맞춰 그래프 세팅
                        updateProgress(0, questionItems.size)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("API_CHECK", "통신 실패", e)
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