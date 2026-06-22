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
import android.view.View

class cardRoadmapActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_roadmap)




        setupTitle()



        // 홈 버튼
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }



        // 대시보드 버튼
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }



        // 학습추천 버튼
        findViewById<TextView>(R.id.btnhs).setOnClickListener {
            // startActivity(Intent(this, StudyActivity::class.java))
        }
    }


    private fun setupTitle() {
        val title = findViewById<TextView>(R.id.txtTitle)
        val text = "Career AI Coach"
        val spannable = SpannableString(text)

        val blue = Color.parseColor("#3950E7")
        val gray = Color.parseColor("#8A8A8A")

        // 글자별 색상 입히기 (인덱스 주의)
        try {
            spannable.setSpan(ForegroundColorSpan(blue), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // C
            spannable.setSpan(ForegroundColorSpan(gray), 1, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // areer
            spannable.setSpan(ForegroundColorSpan(gray), 6, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 공백
            spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // A
            spannable.setSpan(ForegroundColorSpan(gray), 8, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // I
            spannable.setSpan(ForegroundColorSpan(gray), 9, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 공백
            spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // C
            spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // oach

            spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            title.text = spannable
        } catch (e: Exception) {
            // 혹시 에러 나면 그냥 쌩 텍스트라도 나오게 설정
            title.text = text
        }
    }

}