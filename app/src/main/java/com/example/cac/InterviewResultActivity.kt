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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.example.cac.network.InterviewResultResponse
import com.example.cac.network.InterviewQuestionFeedback
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.widget.ProgressBar



class InterviewResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interview_result)


        val sessionId = intent.getIntExtra("session_id", -1)
        if (sessionId != -1) {
            fetchInterviewResult(sessionId)
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupTitle()


        val btnHome = findViewById<TextView>(R.id.btnNoticeh)      // 홈
        val btnResume = findViewById<TextView>(R.id.btnresume)    // 이력서분석
        val btnDashboard = findViewById<TextView>(R.id.btnNoticev) // 대시보드
        val btnMy = findViewById<TextView>(R.id.btnNoticev2)      // MY


        val btnNextInterview = findViewById<MaterialButton>(R.id.btnNextInterview)


        btnNextInterview.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 홈 버튼
        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 이력서 분석 버튼
        btnResume.setOnClickListener {
            // cardResumeActivity 이름 확인 필요 (대문자/소문자)
            val intent = Intent(this, cardResumeActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 대시보드 버튼
        btnDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // MY 버튼
        btnMy.setOnClickListener {
            // startActivity(Intent(this, MyPageActivity::class.java))
        }
    }

    private fun fetchInterviewResult(sessionId: Int) {
        // 1. Call 타입을 FeedbackResponse -> InterviewResultResponse로 변경
        RetrofitClient.api.getInterviewFeedback(sessionId).enqueue(object : retrofit2.Callback<InterviewResultResponse> {

            // 2. onResponse 파라미터 타입도 변경
            override fun onResponse(call: Call<InterviewResultResponse>, response: Response<InterviewResultResponse>) {
                android.util.Log.e("@@RESULT", "서버 응답 확인: ${response.body().toString()}")
                if (response.isSuccessful && response.body() != null) {
                    // 바뀐 모델 구조에 맞춰 데이터 가져오기
                    val feedbackData = response.body()!!.feedback

                    // 점수 세팅
                    findViewById<TextView>(R.id.txtTotalScore).text = feedbackData.overall_score.toInt().toString()

                    // 리사이클러뷰 연결
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerFeedback)
                    recyclerView.layoutManager = LinearLayoutManager(this@InterviewResultActivity)

                    // 어댑터에 전달하는 리스트 타입도 확인 필요 (InterviewQuestionFeedback)
                    recyclerView.adapter = InterviewFeedbackAdapter(feedbackData.question_feedbacks)

                    // 차트 업데이트
                    setupRadarChart(feedbackData.competency_scores)

                } else {
                    Toast.makeText(this@InterviewResultActivity, "결과를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<InterviewResultResponse>, t: Throwable) {
                Toast.makeText(this@InterviewResultActivity, "오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRadarChart(scores: Map<String, Int>) {
        // XML의 chartRadarContainer 내부에 RadarChart가 추가되어 있어야 함 (아래 2번 항목 참고)
        val radarChart = findViewById<RadarChart>(R.id.radarChart) ?: return

        val entries = ArrayList<RadarEntry>()
        val labels = arrayOf("기술", "소통", "문제해결", "협업", "열정")
        val keys = arrayOf("technical", "communication", "problem_solving", "collaboration", "passion")

        for (key in keys) {
            entries.add(RadarEntry((scores[key] ?: 0).toFloat()))
        }

        val dataSet = RadarDataSet(entries, "역량 지표")
        dataSet.apply {
            color = Color.parseColor("#3950E7")
            fillColor = Color.parseColor("#3950E7")
            setDrawFilled(true)
            fillAlpha = 150
            lineWidth = 2f
            valueTextSize = 0f
        }

        radarChart.apply {
            data = RadarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawLabels(false)
            }
            animateY(1000)
            invalidate()
        }
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