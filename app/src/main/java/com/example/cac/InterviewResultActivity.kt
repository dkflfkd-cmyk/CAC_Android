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
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.example.cac.adapter.InterviewFeedbackAdapter
import com.example.cac.network.RetrofitClient
import com.example.cac.network.InterviewResultResponse
import com.example.cac.network.InterviewResultData
import com.example.cac.network.InterviewQuestionFeedback
import com.example.cac.network.InterviewHistoryResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InterviewResultActivity : AppCompatActivity() {

    private lateinit var radarChart: RadarChart
    private lateinit var recyclerViewFeedback: RecyclerView
    private var sessionId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interview_result)

        setupTitle()

        radarChart = findViewById(R.id.radarChart)
        recyclerViewFeedback = findViewById(R.id.recyclerFeedback)
        recyclerViewFeedback.layoutManager = LinearLayoutManager(this)

        sessionId = intent.getIntExtra("session_id", -1)

        val feedbackJson = intent.getStringExtra("interview_result_json")
        val speechAnalysisJson = intent.getStringExtra("speech_analysis_json")

        if (!feedbackJson.isNullOrBlank()) {
            try {
                val resultResponse = Gson().fromJson(feedbackJson, InterviewResultResponse::class.java)

                var speechAnalysisMap: Map<String, Any>? = null
                if (!speechAnalysisJson.isNullOrBlank()) {
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    speechAnalysisMap = Gson().fromJson(speechAnalysisJson, mapType)
                }


                displayResultData(resultResponse.feedback, speechAnalysisMap)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            loadInterviewReportFromServer()
        }


        val sharedPref = getSharedPreferences("CacPrefs", MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", "test1414") ?: "test1414"
        loadGrowthTrendFromServer(userId)

        // 하단 네비게이션

        findViewById<View>(R.id.btnNoticeh)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.btnNextInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.btnNoticev)?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.btnNoticev2)?.setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
            finish()
        }
    }

    private fun loadInterviewReportFromServer() {
        if (sessionId == -1) return

        RetrofitClient.api.getInterviewResult(sessionId).enqueue(object : Callback<InterviewResultResponse> {
            override fun onResponse(call: Call<InterviewResultResponse>, response: Response<InterviewResultResponse>) {
                val body = response.body()

                // 💡 Scope 오류 수정: 지역 변수가 아닌 실제 파라미터(body.feedback, null)를 넘김
                if (response.isSuccessful && body != null && body.feedback != null) {
                    displayResultData(body.feedback, null)
                } else {
                    Toast.makeText(this@InterviewResultActivity, "결과 리포트를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<InterviewResultResponse>, t: Throwable) {
                Toast.makeText(this@InterviewResultActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 💡 누락되었던 성장 추이 API 호출 함수 추가
    private fun loadGrowthTrendFromServer(userId: String) {
        RetrofitClient.api.getInterviewHistory(userId).enqueue(object : Callback<InterviewHistoryResponse> {
            override fun onResponse(call: Call<InterviewHistoryResponse>, response: Response<InterviewHistoryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    setupGrowthTrendUI(response.body()!!)
                }
            }
            override fun onFailure(call: Call<InterviewHistoryResponse>, t: Throwable) {
                Log.e("GrowthTrend", "성장 추이 로드 실패: ${t.message}")
            }
        })
    }


    private fun displayResultData(feedbackData: InterviewResultData, speechAnalysis: Map<String, Any>?) {

        val feedbacks = feedbackData.question_feedbacks

        // 1. 리사이클러뷰 상세 피드백 바인딩
        val adapter = InterviewFeedbackAdapter(feedbacks)
        recyclerViewFeedback.adapter = adapter



        val speechData = speechAnalysis?.get("speech_analysis") as? Map<*, *> ?: speechAnalysis



        // 1. 말하기 속도
        val speedStr = speechData?.get("speaking_speed_wpm")?.toString()
        val speed = speedStr?.toFloatOrNull() ?: 85f

        // 2. 발음 명료도
        val pronunciationStr = speechData?.get("intonation_score")?.toString()
        val pronunciation = pronunciationStr?.toFloatOrNull() ?: 90f

        // 3. 자신감
        val confidenceStr = speechData?.get("confidence_avg")?.toString()
        val confidence = confidenceStr?.toFloatOrNull() ?: 82f

        // 4. 목소리 크기
        val volStr = speechData?.get("volume_score")?.toString()
        val volume = volStr?.toFloatOrNull() ?: 88f

        // 5. 필러워드 총합
        val fillerCount = speechData?.get("filler_word_total")?.toString()?.toFloatOrNull()?.toInt() ?: 3

        // 6. 쉼 횟수 평균
        val pauseCount = speechData?.get("pause_count_avg")?.toString()?.toFloatOrNull()?.toInt() ?: 5



        findViewById<TextView>(R.id.txtSpeedScore)?.text = "${speed.toInt()}/100"
        findViewById<ProgressBar>(R.id.progressSpeed)?.progress = speed.toInt()

        findViewById<TextView>(R.id.txtPronunciationScore)?.text = "${pronunciation.toInt()}/100"
        findViewById<ProgressBar>(R.id.progressPronunciation)?.progress = pronunciation.toInt()

        findViewById<TextView>(R.id.txtConfidenceScore)?.text = "${confidence.toInt()}/100"
        findViewById<ProgressBar>(R.id.progressConfidence)?.progress = confidence.toInt()

        findViewById<TextView>(R.id.txtVolumeScore)?.text = "${volume.toInt()}/100"
        findViewById<ProgressBar>(R.id.progressVolume)?.progress = volume.toInt()

        findViewById<TextView>(R.id.txtFillerCountBadge)?.text = "${fillerCount}회"
        findViewById<TextView>(R.id.txtPauseCountBadge)?.text = "${pauseCount}회"



        val tvFillerStatus = findViewById<TextView>(R.id.txtFillerStatus)
        if (fillerCount > 5) {
            tvFillerStatus?.text = "⚠ 주의"
            tvFillerStatus?.setBackgroundColor(Color.parseColor("#FF5252")) // 빨간색 알림
        } else {
            tvFillerStatus?.text = "✔ 양호"
            tvFillerStatus?.setBackgroundColor(Color.parseColor("#111111")) // 기본 검은색
        }

        val tvPauseStatus = findViewById<TextView>(R.id.txtPauseStatus)
        if (pauseCount > 5) {
            tvPauseStatus?.text = "⚠ 주의"
            tvPauseStatus?.setBackgroundColor(Color.parseColor("#FF5252")) // 빨간색 알림
        } else {
            tvPauseStatus?.text = "✔ 적절"
            tvPauseStatus?.setBackgroundColor(Color.parseColor("#111111")) // 기본 검은색
        }



        val improvements = feedbackData.improvements
        if (!improvements.isNullOrEmpty()) {
            findViewById<TextView>(R.id.txtKeyPoint1)?.text = improvements.getOrNull(0) ?: ""
            findViewById<TextView>(R.id.txtKeyPoint2)?.text = improvements.getOrNull(1) ?: ""
            findViewById<TextView>(R.id.txtKeyPoint3)?.text = improvements.getOrNull(2) ?: ""
        } else {
            val weaknesses = feedbacks.mapNotNull { it.weakness }.filter { it.isNotBlank() }
            if (weaknesses.isNotEmpty()) {
                findViewById<TextView>(R.id.txtKeyPoint1)?.text = weaknesses.getOrNull(0) ?: "답변에 구체적인 수치나 성과를 포함하면 더 설득력이 높아집니다."
                findViewById<TextView>(R.id.txtKeyPoint2)?.text = weaknesses.getOrNull(1) ?: "협업 경험을 말할 때 본인의 역할과 기여도를 명확하게 하세요."
                findViewById<TextView>(R.id.txtKeyPoint3)?.text = weaknesses.getOrNull(2) ?: "답변 시간을 조금 더 효율적으로 관리하면 좋겠습니다."
            }
        }

        val compScores = feedbackData.competency_scores ?: emptyMap()
        val technical = compScores["technical"]?.toFloat() ?: 50f
        val communication = compScores["communication"]?.toFloat() ?: 50f
        val problemSolving = compScores["problem_solving"]?.toFloat() ?: 50f
        val collaboration = compScores["collaboration"]?.toFloat() ?: 50f
        val passion = compScores["passion"]?.toFloat() ?: 50f

        val scores = floatArrayOf(technical, communication, problemSolving, collaboration, passion)

        val overallScore = feedbackData.overall_score.toInt()
        findViewById<TextView>(R.id.txtTotalScore)?.text = overallScore.toString()

        setupRadarChart(scores)
    }

    private fun setupRadarChart(scores: FloatArray) {
        val entries = ArrayList<RadarEntry>()
        for (score in scores) {
            entries.add(RadarEntry(score))
        }

        val dataSet = RadarDataSet(entries, "종합 면접 역량 평가").apply {
            color = Color.parseColor("#C4CAF8")      // 연보라 테두리
            fillColor = Color.parseColor("#C4CAF8")  // 연보라 채우기
            setDrawFilled(true)
            fillAlpha = 180
            lineWidth = 2f
            setDrawValues(false)
        }

        val radarData = RadarData(dataSet)
        radarChart.data = radarData

        radarChart.webColor = Color.parseColor("#E0E0E0")
        radarChart.webColorInner = Color.parseColor("#ECEFF1")
        radarChart.webLineWidth = 1.5f
        radarChart.webLineWidthInner = 1f
        radarChart.description.isEnabled = false
        radarChart.legend.isEnabled = false

        val labels = arrayOf("기술", "소통", "문제해결", "협업", "열정")
        radarChart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt() % labels.size
                    return if (index >= 0) labels[index] else ""
                }
            }
            textSize = 12f
            textColor = Color.parseColor("#333333")
            typeface = Typeface.DEFAULT_BOLD
        }

        radarChart.yAxis.apply {
            axisMinimum = 0f
            axisMaximum = 100f
            setLabelCount(6, true)
            textSize = 9f
            textColor = Color.parseColor("#9E9E9E")
            setDrawLabels(true)
        }

        radarChart.apply {
            isRotationEnabled = false
            setExtraOffsets(50f, 40f, 50f, 40f)
            notifyDataSetChanged()
            invalidate()
        }
    }

    // 서버 통신 성공 시 이 함수를 호출하세요.
    private fun setupGrowthTrendUI(historyResponse: InterviewHistoryResponse) {

        val txtScoreUp = findViewById<TextView>(R.id.txtStatScoreUp)
        val txtPracticeCount = findViewById<TextView>(R.id.txtStatPracticeCount)
        val txtGrowthRate = findViewById<TextView>(R.id.txtStatGrowthRate)

        // 1. 점수 향상 (색상 로직 추가)
        val scoreDiff = historyResponse.latest_score - historyResponse.first_score
        txtScoreUp?.apply {
            if (scoreDiff > 0) {
                text = "+$scoreDiff"
                setTextColor(Color.parseColor("#3950E7")) // 파란색 (상승)
            } else if (scoreDiff < 0) {
                text = "$scoreDiff"
                setTextColor(Color.RED) // 빨간색 (하락)
            } else {
                text = "0"
                setTextColor(Color.parseColor("#111111")) // 검은색 (변동 없음)
            }
        }

        // 2. 총 연습 횟수
        txtPracticeCount?.text = "${historyResponse.total_count}회"

        // 3. 성장률 (색상 로직 추가)
        val growthValue = historyResponse.growth_rate.toInt()
        txtGrowthRate?.apply {
            if (growthValue > 0) {
                text = "+$growthValue%"
                setTextColor(Color.parseColor("#3950E7")) // 파란색 (상승)
            } else if (growthValue < 0) {
                text = "$growthValue%"
                setTextColor(Color.RED) // 빨간색 (하락)
            } else {
                text = "0%"
                setTextColor(Color.parseColor("#111111")) // 검은색 (변동 없음)
            }
        }

        // 4. 꺾은선 그래프(LineChart) 데이터 바인딩
        val lineChart = findViewById<LineChart>(R.id.chartGrowth) ?: return
        val entries = ArrayList<Entry>()

        val historyList = historyResponse.history
        historyList.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.overall_score.toFloat()))
        }

        val lineDataSet = LineDataSet(entries, "성장 추이").apply {
            color = Color.parseColor("#C4CAF8") // 선 색상 (연보라)
            setCircleColor(Color.parseColor("#3950E7")) // 꼭짓점 원 색상 (메인 블루)
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = Color.WHITE
            valueTextSize = 10f
            valueTextColor = Color.parseColor("#666666")
            mode = LineDataSet.Mode.CUBIC_BEZIER // 선을 부드러운 곡선으로 처리
        }

        lineChart.apply {
            data = LineData(lineDataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#888888")
                axisMinimum = -0.5f
                axisMaximum = (historyList.size - 0.5f).coerceAtLeast(0f)
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
                textColor = Color.parseColor("#888888")
            }

            axisRight.isEnabled = false
            animateX(1000)
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

        title.text = spannable
    }
}