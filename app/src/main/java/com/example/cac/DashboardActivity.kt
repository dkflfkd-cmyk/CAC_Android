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
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import java.io.IOException
import com.example.cac.ui.adapter.InterviewAdapter
import com.example.cac.data.InterviewItem
import androidx.recyclerview.widget.LinearLayoutManager
import android.graphics.Paint
import android.content.Context
import android.view.Gravity
import com.example.cac.RoadmapActivity
import org.json.JSONArray
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter

private lateinit var todoContainer: LinearLayout
private lateinit var radarChart: RadarChart

class DashboardActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        //차트
        radarChart = findViewById(R.id.radarChart)

        val btnMy = findViewById<TextView>(R.id.btnNoticev2)
        btnMy.setOnClickListener {
            val intent = Intent(this, MYActivity::class.java)
            startActivity(intent)
        }

        //학습현황
        todoContainer = findViewById(R.id.todoContainer)

        val btnhome = findViewById<TextView>(R.id.btnNoticeh)
        btnhome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnRoadmap=findViewById<TextView>(R.id.btnRoadmap)
        btnRoadmap.setOnClickListener {
            val intent = Intent(this, RoadmapActivity::class.java)
            startActivity(intent)
        }

        //면접 기록
        val rvRecentInterview = findViewById<RecyclerView>(R.id.rvRecentInterview)

        val interviewList = listOf(
            InterviewItem("2025.01.08", "질문 5개 / 소요시간 15분", 82),
            InterviewItem("2025.01.06", "질문 5개 / 소요시간 16분", 78),
            InterviewItem("2025.01.02", "질문 5개 / 소요시간 14분", 75),
            InterviewItem("2024.12.28", "질문 3개 / 소요시간 10분", 72),
            InterviewItem("2024.12.25", "질문 5개 / 소요시간 15분", 70)
        )

        rvRecentInterview.layoutManager = LinearLayoutManager(this)

val recentOnly=interviewList.take(5)
        rvRecentInterview.adapter=InterviewAdapter(recentOnly)

//타이틀
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
            spannable.setSpan(
                ForegroundColorSpan(gray),
                11,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            title.text = spannable


            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            title.text = spannable


            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }




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


        val scores = loadCompetencyScores()

        setupRadarChart(
            technical = scores[0],
            passion = scores[1],
            communication = scores[2],
            collaboration = scores[3],
            problemSolving = scores[4]
        )


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
        dataSet.valueTextColor = Color.parseColor("#6C7BFF")
        dataSet.valueTextSize = 12f

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
