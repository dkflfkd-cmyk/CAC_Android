package com.example.cac

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.InterviewItem
import com.example.cac.network.DashboardHistory
import com.example.cac.network.DashboardResponse
import com.example.cac.network.RetrofitClient
import com.example.cac.ui.adapter.InterviewAdapter
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var txtAvgDocumentScore: TextView
    private lateinit var txtAvgInterviewScore: TextView
    private lateinit var txtScoreImprovement: TextView
    private lateinit var txtTotalCount: TextView
    private lateinit var txtGrowthRate: TextView

    private lateinit var lineChart: LineChart
    private lateinit var radarChart: RadarChart
    private lateinit var todoContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        bindViews()
        setupTitle()
        setupBottomButtons()
        setupRecentInterview()
        setupLearningTodoFromPrefs()
        setupRadarFromPrefs()
        loadDashboard()
//        loadRecentInterviews()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindViews() {
        txtAvgDocumentScore = findViewById(R.id.txtAvgDocumentScore)
        txtAvgInterviewScore = findViewById(R.id.txtAvgInterviewScore)
        txtScoreImprovement = findViewById(R.id.txtScoreImprovement)
        txtTotalCount = findViewById(R.id.txtTotalCount)
        txtGrowthRate = findViewById(R.id.txtGrowthRate)

        lineChart = findViewById(R.id.lineChart)
        radarChart = findViewById(R.id.radarChart)
        todoContainer = findViewById(R.id.todoContainer)
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        findViewById<TextView>(R.id.btnRoadmap).setOnClickListener {
            startActivity(Intent(this, RoadmapActivity::class.java))
        }


    }

    private fun setupRecentInterview() {
        val rvRecentInterview = findViewById<RecyclerView>(R.id.rvRecentInterview)


        val interviewList = listOf(
            InterviewItem(1, "2025.01.08", "질문 5개 / 소요시간 15분", 82),
            InterviewItem(2, "2025.01.06", "질문 5개 / 소요시간 16분", 78),
            InterviewItem(3, "2025.01.02", "질문 5개 / 소요시간 14분", 75),
            InterviewItem(4, "2024.12.28", "질문 3개 / 소요시간 10분", 72),
            InterviewItem(5, "2024.12.25", "질문 5개 / 소요시간 15분", 70)
        )

        rvRecentInterview.layoutManager = LinearLayoutManager(this)


        rvRecentInterview.adapter = InterviewAdapter(interviewList.take(3)) { selectedItem ->
            val intent = Intent(this@DashboardActivity, DetailActivity::class.java)
            intent.putExtra("ITEM_DATA", selectedItem)
            startActivity(intent)
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

    private fun loadDashboard() {
        val token = Session.accessToken
        if (token.isNullOrBlank()) {
            Log.d("DASHBOARD", "token 없음")
            return
        }

        RetrofitClient.api.getDashboard("Bearer $token")
            .enqueue(object : Callback<DashboardResponse> {
                override fun onResponse(
                    call: Call<DashboardResponse>,
                    response: Response<DashboardResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.d("DASHBOARD", "response fail code=${response.code()}")
                        return
                    }

                    val body = response.body()
                    Log.d("DASHBOARD", "body=$body")

                    if (body == null) return
                    applyDashboard(body)
                }

                override fun onFailure(call: Call<DashboardResponse>, t: Throwable) {
                    Log.d("DASHBOARD", "fail: ${t.message}")
                }
            })
    }

    // DashboardActivity.kt 안에 추가
//    private fun loadRecentInterviews() {
//        val token = Session.accessToken ?: return
//
//        lifecycleScope.launch {
//            try {
//                // 1. 서버에서 면접 리스트 전체 가져오기[cite: 1]
//                val response = RetrofitClient.api.getInterviews("Bearer $token")
//
//                // 2. 서버 데이터를 UI용 InterviewItem으로 변환
//                val list = response.interviews?.map { item ->
//                    com.example.cac.data.InterviewItem(
//                        session_id = item.session_id ?: -1,
//                        date = item.created_at ?: "날짜 미정",
//                        meta = "${item.target_job ?: "직무 미정"} / ${item.session_id ?: 0}번",
//                        score = item.overall_score ?: 0,
//                        feedback = item.feedback,
//                        pdf_url = item.pdf_url
//                    )
//                } ?: emptyList()
//
//                // 3. 어댑터에 3개만 전달하여 표시
//                val rvRecentInterview = findViewById<RecyclerView>(R.id.rvRecentInterview)
//                rvRecentInterview.layoutManager = LinearLayoutManager(this@DashboardActivity)
//                rvRecentInterview.adapter = InterviewAdapter(list.take(3)) { selectedItem ->
//                    // 클릭 시 DetailActivity로 이동하는 로직도 동일하게 추가 가능
//                    val intent = Intent(this@DashboardActivity, DetailActivity::class.java)
//                    intent.putExtra("ITEM_DATA", selectedItem)
//                    startActivity(intent)
//                }
//
//            } catch (e: Exception) {
//                Log.e("DASHBOARD", "최근 면접 데이터 로드 실패: ${e.message}")
//            }
//        }
//    }




    private fun applyDashboard(data: DashboardResponse) {
        txtAvgDocumentScore.text = ((data.avg_document_score ?: 0.0).toInt()).toString()

        if (data.avg_interview_score == null) {
            txtAvgInterviewScore.text = "-"
            txtAvgInterviewScore.textSize = 50f
        } else {
            txtAvgInterviewScore.text = data.avg_interview_score.toInt().toString()
            txtAvgInterviewScore.textSize = 50f
        }
        txtScoreImprovement.text =
            if (data.score_improvement == null) {
                "0"
            } else {
                val value = data.score_improvement.toInt()
                if (value > 0) "+$value" else "$value"
            }

        txtTotalCount.text = "${data.total_count ?: 0}회"

        txtGrowthRate.text =
            if (data.growth_rate == null) "0%"
            else "${data.growth_rate.toInt()}%"

        setupGrowthChart(data.history ?: emptyList())
        setupRadarFromDashboard(data)
    }

    private fun setupGrowthChart(history: List<DashboardHistory>) {
        val entries = ArrayList<Entry>()

        history.forEachIndexed { index, item ->
            val x = (item.round ?: (index + 1)).toFloat()
            val y = (item.total_score ?: 0.0).toFloat()
            entries.add(Entry(x, y))
        }

        val dataSet = LineDataSet(entries, "")
        dataSet.color = Color.parseColor("#4A5BFF")
        dataSet.setCircleColor(Color.parseColor("#4A5BFF"))
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false

        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.textColor = Color.parseColor("#999999")

        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisLeft.axisMaximum = 100f
        lineChart.axisLeft.textColor = Color.parseColor("#999999")

        lineChart.invalidate()
    }

    private fun setupRadarFromDashboard(data: DashboardResponse) {
        val avg = data.competency_avg ?: return

        val technical = (avg.technical ?: 0.0).toInt()
        val passion = (avg.passion ?: 0.0).toInt()
        val communication = (avg.communication ?: 0.0).toInt()
        val collaboration = (avg.collaboration ?: 0.0).toInt()
        val problemSolving = (avg.problem_solving ?: 0.0).toInt()

        setupRadarChart(
            technical = technical,
            passion = passion,
            communication = communication,
            collaboration = collaboration,
            problemSolving = problemSolving
        )
    }

    private fun setupLearningTodoFromPrefs() {
        val learningList = loadLearningTodo()

        if (learningList.isNotEmpty()) {
            showLearningTodo(learningList)
        } else {
            showLearningTodo(
                listOf(
                    "추천 학습 데이터가 없습니다",
                    "이력서 분석을 먼저 진행해주세요"
                )
            )
        }
    }

    private fun showLearningTodo(items: List<String>) {
        todoContainer.removeAllViews()

        for (item in items) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(16, 16, 16, 16)
            row.setBackgroundResource(R.drawable.bg_task_default)

            val check = ImageView(this)
            check.setImageResource(R.drawable.ic_circle)

            val text = TextView(this)
            text.text = item
            text.textSize = 14f
            text.setTextColor(Color.parseColor("#333333"))
            text.setPadding(10, 0, 0, 0)

            row.addView(check)
            row.addView(text)

            var isDone = false
            row.setOnClickListener {
                isDone = !isDone

                if (isDone) {
                    row.setBackgroundResource(R.drawable.bg_task_done)
                    text.setTextColor(Color.WHITE)
                    text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    check.setImageResource(R.drawable.ic_check)
                    check.clearColorFilter()
                    check.imageTintList = null
                } else {
                    row.setBackgroundResource(R.drawable.bg_task_default)
                    text.setTextColor(Color.parseColor("#333333"))
                    text.paintFlags = text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    check.setImageResource(R.drawable.ic_circle)
                    check.setColorFilter(Color.GRAY)
                }
            }

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 10

            todoContainer.addView(row, lp)
        }
    }

    private fun loadLearningTodo(): List<String> {
        val prefs = getSharedPreferences("cac_pref", Context.MODE_PRIVATE)
        val saved = prefs.getString("learning_todo", null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(saved)
            List(jsonArray.length()) { i ->
                jsonArray.optString(i)
            }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun setupRadarFromPrefs() {
        val scores = loadCompetencyScores()
        setupRadarChart(
            technical = scores[0],
            passion = scores[1],
            communication = scores[2],
            collaboration = scores[3],
            problemSolving = scores[4]
        )
    }

    private fun setupRadarChart(
        technical: Int,
        passion: Int,
        communication: Int,
        collaboration: Int,
        problemSolving: Int
    ) {
        val entries = arrayListOf(
            RadarEntry(technical.toFloat()),
            RadarEntry(communication.toFloat()),
            RadarEntry(problemSolving.toFloat()),
            RadarEntry(collaboration.toFloat()),
            RadarEntry(passion.toFloat())
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

    private fun loadCompetencyScores(): IntArray {
        val prefs = getSharedPreferences("cac_pref", Context.MODE_PRIVATE)

        val technical = prefs.getInt("score_technical", 0)
        val passion = prefs.getInt("score_passion", 0)
        val communication = prefs.getInt("score_communication", 0)
        val collaboration = prefs.getInt("score_collaboration", 0)
        val problemSolving = prefs.getInt("score_problem_solving", 0)

        return intArrayOf(
            technical,
            passion,
            communication,
            collaboration,
            problemSolving
        )
    }
}