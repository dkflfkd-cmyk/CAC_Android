package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.activity.enableEdgeToEdge
import android.widget.Toast
import com.example.cac.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.cac.network.FeedbackResponse



class InterviewResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interview_result)


        val sessionId = intent.getIntExtra("session_id", -1)
        if (sessionId != -1) {
            fetchInterviewFeedback(sessionId)
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }


        val job = intent.getStringExtra("job").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val count = intent.getIntExtra("count", 0)

        setupTitle()

        val txtKeyPoint1 = findViewById<TextView>(R.id.txtKeyPoint1)
        val txtKeyPoint2 = findViewById<TextView>(R.id.txtKeyPoint2)
        val txtKeyPoint3 = findViewById<TextView>(R.id.txtKeyPoint3)
        val btnNextInterview = findViewById<MaterialButton>(R.id.btnNextInterview)

        // 하단 탭 버튼들 연결
        val btnNoticeh = findViewById<TextView>(R.id.btnNoticeh)
        val btnNoticev = findViewById<TextView>(R.id.btnNoticev)
        val btnresume = findViewById<TextView>(R.id.btnresume)



        // 새로운 면접 시작 버튼 클릭 시 메인 화면
        btnNextInterview.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 하단 홈 탭 버튼 클릭 이벤트
        btnNoticeh.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // 하단 이력서 버튼 클릭 이벤트
        btnresume.setOnClickListener {
            startActivity(Intent(this, cardResumeActivity::class.java))
            finish()
        }

        // 하단 대시보드 탭 버튼 클릭 이벤트
        btnNoticev.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }


    }

    private fun fetchInterviewFeedback(sessionId: Int) {

        RetrofitClient.api.getInterviewFeedback(sessionId).enqueue(object : retrofit2.Callback<FeedbackResponse> {
            override fun onResponse(call: Call<FeedbackResponse>, response: retrofit2.Response<FeedbackResponse>) {
                if (response.isSuccessful) {
                    val feedback = response.body()


                    findViewById<TextView>(R.id.txtTotalScore).text = feedback?.total_score.toString()
                    findViewById<TextView>(R.id.txtKeyPoint1).text = feedback?.key_points?.getOrNull(0)
                    findViewById<TextView>(R.id.txtKeyPoint2).text = feedback?.key_points?.getOrNull(1)
                    findViewById<TextView>(R.id.txtKeyPoint3).text = feedback?.key_points?.getOrNull(2)


                }
            }
            override fun onFailure(call: Call<FeedbackResponse>, t: Throwable) {
                Toast.makeText(this@InterviewResultActivity, "결과 조회 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTitle() {
        val title = findViewById<TextView>(R.id.txtTitle)
        val text = "Career AI Coach"
        val spannable = SpannableString(text)

        val blue = Color.parseColor("#3950E7")
        val gray = Color.parseColor("#8A8A8A")

        // 1. Career (C만 파란색, 나머지 회색)
        spannable.setSpan(ForegroundColorSpan(blue), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 2. 공백 (회색)
        spannable.setSpan(ForegroundColorSpan(gray), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 3. AI (A는 파란색, I는 회색)
        spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 8, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 4. 공백 (회색)
        spannable.setSpan(ForegroundColorSpan(gray), 9, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 5. Coach (C만 파란색, 나머지 회색)
        spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 6. 전체 볼드(Bold) 스타일 지정
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        title.text = spannable
    }

}