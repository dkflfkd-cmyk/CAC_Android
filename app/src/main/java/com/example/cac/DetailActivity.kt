package com.example.cac // 본인의 패키지 경로로 수정

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cac.data.InterviewItem // 데이터 클래스 임포트

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // 1. 데이터 받기
        val item = intent.getSerializableExtra("ITEM_DATA") as? InterviewItem

        // 2. 화면에 값 넣기
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvFeedback = findViewById<TextView>(R.id.tvFeedback)

        item?.let {
            tvScore.text = "점수: ${it.score}점"
            tvFeedback.text = it.feedback ?: "피드백이 아직 준비되지 않았습니다."
        }
    }
}