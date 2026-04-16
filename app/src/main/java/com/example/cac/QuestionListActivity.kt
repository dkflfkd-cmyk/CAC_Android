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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.QuestionItem
import com.example.cac.ui.adapter.QuestionListAdapter

class QuestionListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question_list)

        val job = intent.getStringExtra("job").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val count = intent.getIntExtra("count", 3)

        val txtJobTitle = findViewById<TextView>(R.id.txtJobTitle)
        val txtSubInfo = findViewById<TextView>(R.id.txtSubInfo)
        val txtProgress = findViewById<TextView>(R.id.txtProgress)
        val rvQuestionList = findViewById<RecyclerView>(R.id.rvQuestionList)

        txtJobTitle.text = job
        txtSubInfo.text = type
        txtProgress.text = "1/$count"

        val questionList = makeDummyQuestions(type, count)

        rvQuestionList.layoutManager = LinearLayoutManager(this)
        rvQuestionList.adapter = QuestionListAdapter(questionList) { position ->
            val intent = Intent(this, InterviewStartActivity::class.java)
            intent.putExtra("job", job)
            intent.putExtra("type", type)
            intent.putExtra("count", count)
            intent.putExtra("questionIndex", position)
            intent.putExtra("questionText", questionList[position].question)
            startActivity(intent)
        }

        setupTitle()
        setupBottomButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun makeDummyQuestions(type: String, count: Int): List<QuestionItem> {
        val base = when (type) {
            "기술" -> listOf(
                "사용자 경험을 개선한 프로젝트 사례를 설명해주세요.",
                "디자인 시스템을 구축한 경험이 있나요?",
                "개발자와 협업 중 충돌이 있었을 때 어떻게 해결했나요?",
                "Figma와 Adobe XD의 차이점은 무엇인가요?",
                "반응형 UI를 설계할 때 가장 중요하게 보는 점은 무엇인가요?"
            )
            "인성" -> listOf(
                "본인의 장점과 단점을 말씀해주세요.",
                "갈등 상황을 해결한 경험이 있나요?",
                "실패를 극복한 경험을 말씀해주세요.",
                "팀에서 본인은 어떤 역할을 맡는 편인가요?",
                "스트레스를 어떻게 관리하나요?"
            )
            "프로젝트" -> listOf(
                "가장 기억에 남는 프로젝트를 소개해주세요.",
                "프로젝트에서 맡았던 역할은 무엇인가요?",
                "프로젝트 중 가장 어려웠던 점은 무엇이었나요?",
                "프로젝트 성과를 어떻게 측정했나요?",
                "다시 한다면 개선하고 싶은 점은 무엇인가요?"
            )
            else -> listOf(
                "운영체제와 프로세스의 차이를 설명해주세요.",
                "HTTP와 HTTPS 차이를 설명해주세요.",
                "스택과 큐의 차이를 설명해주세요.",
                "DB 정규화가 무엇인가요?",
                "객체지향의 4대 특징을 설명해주세요."
            )
        }

        return List(count) { index ->
            QuestionItem(
                index + 1,
                base[index % base.size]
            )
        }
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
        }

        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
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