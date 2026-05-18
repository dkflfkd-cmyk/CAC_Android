package com.example.cac

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cac.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.Uri

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setupTitle()


        // 적성 검사 버튼 클릭 시 웹 브라우저로 이동
        val aptitudeUrl = "http://52.79.211.82:8000/static/aptitude-test.html"

        // 버튼을 눌렀을 때
        findViewById<View>(R.id.btnDoAptitudeTest).setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(aptitudeUrl))
            startActivity(browserIntent)
        }

//        // 카드 배경 전체를 눌러도 이동
//        findViewById<View>(R.id.cardAptitude).setOnClickListener {
//            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(aptitudeUrl))
//            startActivity(browserIntent)
//        }

        setupClickListeners()

    }


    private fun setupTitle() {
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

        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable
    }


    private fun setupClickListeners() {
        // AI 면접 하단 버튼
        findViewById<TextView>(R.id.btnInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
        }

        // 마이페이지 버튼
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
        }

        // 대시보드 버튼
//        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
//            startActivity(Intent(this, DashboardActivity::class.java))
//        }

//테스트
        val btnNoticev = findViewById<View>(R.id.btnNoticev)

        btnNoticev.setOnClickListener {
            val intent = Intent(this, InterviewResultActivity::class.java)
            intent.putExtra("session_id", 199)
            startActivity(intent)
        }


        // 면접 카드
        findViewById<View>(R.id.cardInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
        }

        // 이력서/자소서 분석 카드
        findViewById<View>(R.id.cardResume).setOnClickListener {
            startActivity(Intent(this, cardResumeActivity::class.java))
        }

        // 질문리스트 카드
        findViewById<View>(R.id.cardlist).setOnClickListener {
            startActivity(Intent(this, QuestionSetupActivity::class.java))
        }
    }



}