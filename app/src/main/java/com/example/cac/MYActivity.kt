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
import com.example.cac.data.InterviewItem
import com.example.cac.data.ResumeItem
import com.example.cac.ui.adapter.InterviewAdapter
import com.example.cac.ui.adapter.ResumeAdapter
import com.example.cac.SessionManager
import com.example.cac.network.RetrofitClient
class MYActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_myactivity)

loadMeAndApply()
        //확인
        android.util.Log.d("MY_DEBUG", "calling loadMeAndApply()")
        loadMeAndApply()



// 면접 기록
        val rvInterview = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvInterview)
        val rvResume = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvResume)

        rvInterview.layoutManager = LinearLayoutManager(this)
        rvResume.layoutManager = LinearLayoutManager(this)

// 일단 테스트 더미데이터
        val interviewList = listOf(
            InterviewItem("2025.01.08", "질문 5개 / 소요시간 15분", 82),
            InterviewItem("2025.01.06", "질문 5개 / 소요시간 16분", 78),
            InterviewItem("2025.01.02", "질문 5개 / 소요시간 14분", 75)
        )

        val resumeList = listOf(
            ResumeItem("2026.01.20 이력서.pdf", "2026.01.20"),
            ResumeItem("2025.03.15 이력서.pdf", "2025.03.15")
        )

        rvInterview.adapter = InterviewAdapter(interviewList)
        rvResume.adapter = ResumeAdapter(resumeList)

//면접 기록 끝

        val btnInterview = findViewById<TextView>(R.id.btnInterview)
        btnInterview.setOnClickListener {
            val intent = Intent(this, InterviewActivity::class.java)
            startActivity(intent)
        }

        val btnhome = findViewById<TextView>(R.id.btnNoticeh)
        btnhome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnDS = findViewById<TextView>(R.id.btnNoticev)
        btnDS.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }




        val title = findViewById<TextView>(R.id.txtTitle)

        val text = "Career AI Coach"
        val spannable = SpannableString(text)

        val blue = Color.parseColor("#3950E7")
        val gray = Color.parseColor("#8A8A8A")

        // Career (C만 파란색, 나머지 회색)
        spannable.setSpan(ForegroundColorSpan(blue), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 공백(회색)
        spannable.setSpan(ForegroundColorSpan(gray), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // AI (A는 파란색, I는 회색)
        spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 8, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // 공백(회색)
        spannable.setSpan(ForegroundColorSpan(gray), 9, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Coach (C만 파란색, 나머지 회색)
        spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        title.text = spannable


        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //확인

        android.util.Log.d("MY_DEBUG", "MYActivity onCreate called")

    }

    private fun loadMeAndApply() {

        //확인
        android.util.Log.d("MY_DEBUG", "loadMeAndApply start token=" + SessionManager.getToken(this))


        val token = SessionManager.getToken(this)

        if (token.isNullOrBlank()) {
            android.util.Log.d("ME_RESPONSE", "토큰 없음")
            return
        }

        RetrofitClient.api.me("Bearer $token")
            .enqueue(object : retrofit2.Callback<Map<String, Any>> {

                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    if (!response.isSuccessful) {
                        android.util.Log.d("ME_RESPONSE", "실패 code=${response.code()}")
                        return
                    }

                    val body = response.body()
                    android.util.Log.d("ME_RESPONSE", body.toString())

                    val username = body?.get("username")?.toString()
                    val grade = body?.get("grade")?.toString()
                    val major = body?.get("major")?.toString()
                    val goal = body?.get("target_job")?.toString()

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtName).text =
                        username ?: "홍길동"

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtStatus1).text =
                        "학년\n${grade ?: "미등록"}"

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtStatus2).text =
                        "전공\n${major ?: "미등록"}"

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtStatus3).text =
                        "목표\n${goal ?: "미등록"}"
                }

                override fun onFailure(
                    call: retrofit2.Call<Map<String, Any>>,
                    t: Throwable
                ) {
                    android.util.Log.d("ME_RESPONSE", "네트워크 오류: ${t.message}")
                }
            })
    }

}