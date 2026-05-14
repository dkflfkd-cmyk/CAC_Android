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
import com.example.cac.network.InterviewHistoryItem
import com.example.cac.network.InterviewHistoryResponse
import android.view.View
import com.github.mikephil.charting.formatter.ValueFormatter



class InterviewResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interview_result)


        val sessionId = intent.getIntExtra("session_id", -1)
        if (sessionId != -1) {
            fetchInterviewResult(sessionId)
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
            val intent = Intent(this, MYActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchInterviewResult(sessionId: Int) {
        RetrofitClient.api.getInterviewFeedback(sessionId).enqueue(object : retrofit2.Callback<InterviewResultResponse> {

            override fun onResponse(call: Call<InterviewResultResponse>, response: Response<InterviewResultResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val feedbackData = body.feedback


                    val weaknesses = feedbackData.improvements ?: emptyList()

                    // 텍스트뷰 및 카드뷰 연결 (미리 선언)
                    val txtKeyPoint1 = findViewById<TextView>(R.id.txtKeyPoint1)
                    val txtKeyPoint2 = findViewById<TextView>(R.id.txtKeyPoint2)
                    val txtKeyPoint3 = findViewById<TextView>(R.id.txtKeyPoint3)

                    val cardKeyPoint2 = findViewById<View>(R.id.cardKeyPoint2)
                    val cardKeyPoint3 = findViewById<View>(R.id.cardKeyPoint3)


                    if (weaknesses.isNotEmpty()) {
                        // 1번 포인트는 항상 노출
                        txtKeyPoint1.text = weaknesses[0]

                        // 2번 포인트 처리
                        if (weaknesses.size >= 2) {
                            txtKeyPoint2?.text = weaknesses[1]
                            cardKeyPoint2?.visibility = View.VISIBLE
                        } else {
                            cardKeyPoint2?.visibility = View.GONE
                        }

                        // 3번 포인트 처리
                        if (weaknesses.size >= 3) {
                            txtKeyPoint3?.text = weaknesses[2]
                            cardKeyPoint3?.visibility = View.VISIBLE
                        } else {
                            cardKeyPoint3?.visibility = View.GONE
                        }
                    } else {
                        // 개선 포인트 데이터가 아예 없을 경우
                        txtKeyPoint1.text = "종합적으로 훌륭한 인터뷰였습니다."
                        cardKeyPoint2?.visibility = View.GONE
                        cardKeyPoint3?.visibility = View.GONE
                    }

                    // [1] 기본 정보 세팅
                    findViewById<TextView>(R.id.txtTotalScore).text = feedbackData.overall_score.toInt().toString()

                    // [2] 피드백 리스트 및 차트 세팅
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerFeedback)
                    recyclerView.layoutManager = LinearLayoutManager(this@InterviewResultActivity)
                    recyclerView.adapter = InterviewFeedbackAdapter(feedbackData.question_feedbacks)
                    setupRadarChart(feedbackData.competency_scores)

                    // [3] 성장 데이터 호출
                    val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    val realUserId = sharedPref.getString("USER_ID", "default_user") ?: "default_user"

                    fetchGrowthData(realUserId)

                } else {
                    Toast.makeText(this@InterviewResultActivity, "결과를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<InterviewResultResponse>, t: Throwable) {
                Toast.makeText(this@InterviewResultActivity, "오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun fetchGrowthData(userId: String) {
        RetrofitClient.api.getInterviewHistory(userId).enqueue(object : retrofit2.Callback<InterviewHistoryResponse> {
            override fun onResponse(call: Call<InterviewHistoryResponse>, response: Response<InterviewHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val history = result.history


                    val txtGrowthRate = findViewById<TextView>(R.id.txtStatGrowthRate)
                    val growthValue = result.growth_rate.toInt()
                    val themeBlue = android.graphics.Color.parseColor("#3950E7")

                    txtGrowthRate?.apply {
                        if (growthValue > 0) {
                            // 상승 시
                            text = "+$growthValue%"
                            setTextColor(themeBlue)
                        } else {
                            // 0이거나 하락 시
                            text = "$growthValue%"
                            setTextColor(android.graphics.Color.BLACK)
                        }
                    }

                    // 2. 총 연습 횟수 세팅
                    findViewById<TextView>(R.id.txtStatTotalPractice)?.text = "${result.total_count}회"

                    // 3. 꺾은선 그래프 업데이트
                    val lineChart = findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chartGrowth)
                    if (lineChart != null && history.isNotEmpty()) {
                        setupGrowthLineChart(lineChart, history)
                    }
                }
            }

            override fun onFailure(call: Call<InterviewHistoryResponse>, t: Throwable) {
                android.util.Log.e("GrowthAPI", "이력 데이터 로드 실패: ${t.message}")
            }
        })
    }




    private fun setupGrowthLineChart(lineChart: com.github.mikephil.charting.charts.LineChart, history: List<InterviewHistoryItem>) {

        val entries = ArrayList<com.github.mikephil.charting.data.Entry>()

        history.forEachIndexed { index, item ->

            val scoreValue = item.overall_score.toFloat()
            entries.add(com.github.mikephil.charting.data.Entry(index.toFloat(), scoreValue))
        }

        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "점수 변화").apply {
            color = android.graphics.Color.parseColor("#4A90E2")
            setCircleColor(android.graphics.Color.parseColor("#4A90E2"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = android.graphics.Color.WHITE

            // 이미지처럼 점수 숫자 표시
            setDrawValues(true)
            valueTextSize = 11f
            valueTextColor = android.graphics.Color.DKGRAY
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }

            setDrawFilled(true)
            fillColor = android.graphics.Color.parseColor("#D0E3F7")
            fillAlpha = 80
            mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.apply {
            clear()
            data = com.github.mikephil.charting.data.LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false

            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                axisMinimum = -0.3f
                axisMaximum = history.size.toFloat() - 0.7f

                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
                }
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f // 100점 기준으로 고정해서 0점 바닥 탈출
                setDrawGridLines(true)
                gridColor = android.graphics.Color.parseColor("#EEEEEE")
            }
            axisRight.isEnabled = false

            setTouchEnabled(false) // 정적 이미지 느낌
            animateX(800)
            invalidate()
        }
    }

    private fun setupRadarChart(scores: Map<String, Int>) {
        val radarChart = findViewById<RadarChart>(R.id.radarChart) ?: return

        // 1. DashboardActivity와 완전히 동일한 순서로 데이터 입력
        val entries = arrayListOf(
            RadarEntry((scores["technical"] ?: 0).toFloat()),
            RadarEntry((scores["communication"] ?: 0).toFloat()),
            RadarEntry((scores["problem_solving"] ?: 0).toFloat()),
            RadarEntry((scores["collaboration"] ?: 0).toFloat()),
            RadarEntry((scores["passion"] ?: 0).toFloat())
        )

        val dataSet = RadarDataSet(entries, "역량 점수")
        dataSet.color = Color.parseColor("#6C7BFF")
        dataSet.fillColor = Color.parseColor("#6C7BFF")
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 80
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false)

        val data = RadarData(dataSet)
        radarChart.data = data


        val labels = listOf("기술 역량", "커뮤니케이션", "문제해결", "협업능력", "열정")

        radarChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return labels[value.toInt() % labels.size]
            }
        }

        radarChart.xAxis.textSize = 12f
        radarChart.xAxis.textColor = Color.parseColor("#111111")


        radarChart.xAxis.setYOffset(-10f)
        radarChart.setExtraOffsets(-10f, -10f, -10f, -10f)

        radarChart.yAxis.axisMinimum = 0f
        radarChart.yAxis.axisMaximum = 100f
        radarChart.yAxis.labelCount = 5
        radarChart.yAxis.textColor = Color.parseColor("#999999")
        radarChart.yAxis.textSize = 10f


        radarChart.description.isEnabled = false
        radarChart.legend.isEnabled = false
        radarChart.webLineWidth = 1f
        radarChart.webColor = Color.parseColor("#BDBDBD")
        radarChart.webLineWidthInner = 1f
        radarChart.webColorInner = Color.parseColor("#D9D9D9")
        radarChart.setBackgroundColor(Color.WHITE)

        radarChart.invalidate()
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