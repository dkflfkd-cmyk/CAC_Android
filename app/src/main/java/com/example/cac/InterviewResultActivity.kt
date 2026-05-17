package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Toast
import com.example.cac.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.example.cac.network.InterviewResultResponse
import com.example.cac.network.InterviewQuestionFeedback
import android.view.View
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.cac.network.InterviewHistoryItem
import com.example.cac.network.InterviewHistoryResponse
import java.io.File
import com.google.gson.Gson
import com.example.cac.adapter.InterviewFeedbackAdapter

class InterviewResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_result)

        val sessionId = intent.getIntExtra("session_id", -1)
        val jsonStr = intent.getStringExtra("interview_result_json")

        val speechJsonStr = intent.getStringExtra("speech_analysis_json")

        if (jsonStr != null) {
            try {
                val responseBody = Gson().fromJson(jsonStr, InterviewResultResponse::class.java)


                val speechAnalysisData = if (!speechJsonStr.isNullOrBlank()) {
                    Gson().fromJson(speechJsonStr, com.example.cac.network.SpeechAnalysisResponse::class.java)
                } else null


                bindResultData(responseBody, speechAnalysisData)

                android.util.Log.d("InterviewResult", "이전 화면에서 전달받은 데이터로 즉시 바인딩 완료")
            } catch (e: Exception) {
                android.util.Log.e("InterviewResult", "JSON 파싱 실패, 기존 서버 통신으로 대체: ${e.message}")
                if (sessionId != -1) fetchInterviewResult(sessionId)
            }
        } else {
            if (sessionId != -1) {
                fetchInterviewResult(sessionId)
            }
        }

        setupTitle()

        val btnHome = findViewById<TextView>(R.id.btnNoticeh)
        val btnResume = findViewById<TextView>(R.id.btnresume)
        val btnDashboard = findViewById<TextView>(R.id.btnNoticev)
        val btnMy = findViewById<TextView>(R.id.btnNoticev2)
        val btnNextInterview = findViewById<MaterialButton>(R.id.btnNextInterview)

        btnNextInterview.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnResume.setOnClickListener {
            val intent = Intent(this, cardResumeActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnDashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnMy.setOnClickListener {
            val intent = Intent(this, MYActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun bindResultData(
        responseBody: InterviewResultResponse,
        speechAnalysis: com.example.cac.network.SpeechAnalysisResponse?
    ) {
        val feedbackData = responseBody.feedback
        val questionFeedbacks = feedbackData.question_feedbacks
        val weaknesses = feedbackData.improvements ?: emptyList()
        val strengths = feedbackData.strengths ?: emptyList()

        findViewById<TextView>(R.id.txtTotalScore).text = feedbackData.overall_score.toInt().toString()

        val scores = feedbackData.competency_scores
        setupRadarChart(
            technical = scores["technical"] ?: 0,
            passion = scores["passion"] ?: 0,
            communication = scores["communication"] ?: 0,
            collaboration = scores["collaboration"] ?: 0,
            problemSolving = scores["problem_solving"] ?: 0
        )

        val txtKeyPoint1 = findViewById<TextView>(R.id.txtKeyPoint1)
        val txtKeyPoint2 = findViewById<TextView>(R.id.txtKeyPoint2)
        val txtKeyPoint3 = findViewById<TextView>(R.id.txtKeyPoint3)

        val cardKeyPoint2 = findViewById<View>(R.id.cardKeyPoint2)
        val cardKeyPoint3 = findViewById<View>(R.id.cardKeyPoint3)

        if (weaknesses.isNotEmpty()) {
            txtKeyPoint1.text = weaknesses[0]
            if (weaknesses.size >= 2) {
                txtKeyPoint2?.text = weaknesses[1]
                cardKeyPoint2?.visibility = View.VISIBLE
            } else {
                cardKeyPoint2?.visibility = View.GONE
            }
            if (weaknesses.size >= 3) {
                txtKeyPoint3?.text = weaknesses[2]
                cardKeyPoint3?.visibility = View.VISIBLE
            } else {
                cardKeyPoint3?.visibility = View.GONE
            }
        } else {
            txtKeyPoint1.text = "종합적으로 훌륭한 인터뷰였습니다."
            cardKeyPoint2?.visibility = View.GONE
            cardKeyPoint3?.visibility = View.GONE
        }


        bindAdapter(questionFeedbacks, strengths, speechAnalysis)

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val realUserId = sharedPref.getString("USER_ID", "default_user") ?: "default_user"
        fetchGrowthData(realUserId)
    }

    private fun fetchInterviewResult(sessionId: Int) {
        RetrofitClient.api.getInterviewFeedback(sessionId).enqueue(object : retrofit2.Callback<InterviewResultResponse> {
            override fun onResponse(call: Call<InterviewResultResponse>, response: Response<InterviewResultResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    bindResultData(response.body()!!, null)
                } else {
                    Toast.makeText(this@InterviewResultActivity, "결과 피드백 보고서를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<InterviewResultResponse>, t: Throwable) {
                Toast.makeText(this@InterviewResultActivity, "오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindAdapter(
        questionFeedbacks: List<InterviewQuestionFeedback>,
        strengths: List<String>,
        speechAnalysis: com.example.cac.network.SpeechAnalysisResponse?
    ) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerFeedback)
        if (recyclerView != null) {
            recyclerView.layoutManager = LinearLayoutManager(this@InterviewResultActivity)
            val cleanFeedbacks = questionFeedbacks.toList()
            val cleanStrengths = strengths.toList()


            recyclerView.adapter = InterviewFeedbackAdapter(cleanFeedbacks, cleanStrengths, speechAnalysis)
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun fetchGrowthData(userId: String) {
        RetrofitClient.api.getInterviewHistory(userId).enqueue(object : retrofit2.Callback<InterviewHistoryResponse> {
            override fun onResponse(call: Call<InterviewHistoryResponse>, response: Response<InterviewHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    val history = result.history

                    val txtStatScoreUp = findViewById<TextView>(R.id.txtStatScoreUp)
                    val scoreChange = history.lastOrNull()?.change ?: 0

                    txtStatScoreUp?.apply {
                        if (scoreChange > 0) {
                            text = "+$scoreChange"
                            setTextColor(Color.parseColor("#3950E7"))
                        } else if (scoreChange < 0) {
                            text = "$scoreChange"
                            setTextColor(Color.RED)
                        } else {
                            text = "0"
                            setTextColor(Color.BLACK)
                        }
                    }

                    findViewById<TextView>(R.id.txtStatTotalPractice)?.text = "${result.total_count}회"

                    val txtGrowthRate = findViewById<TextView>(R.id.txtStatGrowthRate)
                    val growthValue = result.growth_rate.toInt()

                    txtGrowthRate?.apply {
                        if (growthValue > 0) {
                            text = "+$growthValue%"
                            setTextColor(Color.parseColor("#3950E7"))
                        } else if (growthValue < 0) {
                            text = "$growthValue%"
                            setTextColor(Color.RED)
                        } else {
                            text = "0%"
                            setTextColor(Color.BLACK)
                        }
                    }

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
            color = Color.parseColor("#4A90E2")
            setCircleColor(Color.parseColor("#4A90E2"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            setDrawValues(true)
            valueTextSize = 11f
            valueTextColor = Color.DKGRAY
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }
            setDrawFilled(true)
            fillColor = Color.parseColor("#D0E3F7")
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
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = (value.toInt() + 1).toString()
                }
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
            }
            axisRight.isEnabled = false
            setTouchEnabled(false)
            invalidate()
        }
    }


    private fun setupRadarChart(
        technical: Int,
        passion: Int,
        communication: Int,
        collaboration: Int,
        problemSolving: Int
    ) {
        val radarChart = findViewById<RadarChart>(R.id.radarChart) ?: return

        val entries = arrayListOf(
            RadarEntry(technical.toFloat()),
            RadarEntry(communication.toFloat()),
            RadarEntry(problemSolving.toFloat()),
            RadarEntry(collaboration.toFloat()),
            RadarEntry(passion.toFloat())
        )

        val dataSet = RadarDataSet(entries, "역량 점수").apply {
            color = Color.parseColor("#3950E7")      // 다각형 외곽선 색상
            fillColor = Color.parseColor("#6C7BFF")  // 다각형 내부 채우기 색상
            setDrawFilled(true)
            fillAlpha = 45
            lineWidth = 2.5f

            // 그래프 꼭짓점마다 수치 점수 표기
            setDrawValues(true)
            valueTextSize = 12f
            valueTextColor = Color.parseColor("#3950E7")
            valueTypeface = Typeface.DEFAULT_BOLD
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }

            isDrawHighlightCircleEnabled = true
            setDrawHighlightIndicators(false)
        }

        val data = RadarData(dataSet)
        radarChart.data = data

        val labels = listOf("기술 역량", "커뮤니케이션", "문제해결", "협업능력", "열정")

        // X축 설정
        radarChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt() % labels.size
                    return labels[index]
                }
            }
            textSize = 11f
            textColor = Color.parseColor("#444444")
            typeface = Typeface.DEFAULT_BOLD
        }

        // Y축 설정
        radarChart.yAxis.apply {
            axisMinimum = 0f
            axisMaximum = 100f


            setCenterAxisLabels(false)


            setLabelCount(6, true)
            setDrawLabels(false)
        }


        radarChart.apply {
            description.isEnabled = false
            legend.isEnabled = false


            webLineWidth = 1.5f
            webColor = Color.parseColor("#CCCCCC")
            webLineWidthInner = 1.0f
            webColorInner = Color.parseColor("#E5E5E5")


            skipWebLineCount = 0

            setBackgroundColor(Color.WHITE)
            isRotationEnabled = false


            setExtraOffsets(65f, 40f, 65f, 40f)


            notifyDataSetChanged()
            invalidate()
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