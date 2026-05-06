package com.example.cac

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import android.widget.Button

class QuestionListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_list)

        //  Intent 데이터 받기
        val sessionId = intent.getIntExtra("session_id", -1)
        val job = intent.getStringExtra("job").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val count = intent.getIntExtra("count", 3)

        // 세션 아이디가 없으면 진행 불가
        if (sessionId == -1) {
            Toast.makeText(this, "오류: 세션 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val txtJobTitle = findViewById<TextView>(R.id.txtJobTitle)
        val txtSubInfo = findViewById<TextView>(R.id.txtSubInfo)
        val txtProgress = findViewById<TextView>(R.id.txtProgress)
        val rvQuestionList = findViewById<RecyclerView>(R.id.rvQuestionList)

        txtJobTitle.text = job
        txtSubInfo.text = type
        txtProgress.text = "0/$count"

        rvQuestionList.layoutManager = LinearLayoutManager(this)

        // 화면 진입 시 무조건 기존 질문 리스트만 로드
        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val networkCall = RetrofitClient.api.getQuestions(sessionId)
                val serverResponse = networkCall.execute()
                val questionList = serverResponse.body()

                if (serverResponse.isSuccessful && questionList != null) {
                    withContext(Dispatchers.Main) {
                        val totalQuestions = questionList.size
                        txtProgress.text = "0/$totalQuestions"
                        txtJobTitle.text = job
                        txtSubInfo.text = type

                        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressQuestion)
                        progressBar.progress = 0

                        val questionItems = questionList.map { serverData ->
                            QuestionItem(
                                number = serverData.question_id,
                                question = serverData.question_text
                            )
                        }

                        rvQuestionList.adapter = QuestionListAdapter(questionItems) { position ->
                            val intent = Intent(this@QuestionListActivity, InterviewStartActivity::class.java)
                            intent.putExtra("session_id", sessionId)
                            intent.putExtra("question_id", questionItems[position].number)
                            intent.putExtra("question_text", questionItems[position].question)
                            intent.putExtra("job", job)
                            intent.putExtra("type", type)
                            intent.putExtra("count", count)
                            startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setupTitle()
        setupBottomButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

   //다시받기 버튼
        val btnRecreateQuestions = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRecreateQuestions)
        btnRecreateQuestions.setOnClickListener {

            val intent = Intent(this, InterviewActivity::class.java).apply {
                putExtra("PREV_JOB", job)
                putExtra("PREV_TYPE", type)
                putExtra("PREV_COUNT", count)

                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }


    private fun setupBottomButtons() {
        val sessionId = intent.getIntExtra("session_id", -1)
        val job = intent.getStringExtra("job").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val count = intent.getIntExtra("count", 3)

        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {

                putExtra("LAST_SESSION_ID", sessionId)
                putExtra("LAST_JOB", job)
                putExtra("LAST_TYPE", type)
                putExtra("LAST_COUNT", count)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
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