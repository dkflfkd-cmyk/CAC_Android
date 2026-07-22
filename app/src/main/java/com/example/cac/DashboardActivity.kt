package com.example.cac

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.InterviewItem
import com.example.cac.data.InterviewRepository
import com.example.cac.network.RetrofitClient
import com.example.cac.ui.adapter.InterviewAdapter
import com.example.cac.view.PentagonRadarChartView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
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
    private lateinit var interviewLineChart: LineChart
    private lateinit var txtInterviewScoreImprovement: TextView
    private lateinit var txtInterviewCount: TextView
    private lateinit var txtInterviewGrowthRate: TextView
    private lateinit var txtEmptyInterviewGrowth: TextView
    private lateinit var interviewGrowthStats: View
    private lateinit var radarChart: PentagonRadarChartView
    private lateinit var rvRecentInterview: RecyclerView
    private lateinit var txtEmptyRecentInterview: TextView
    private lateinit var cardSpeechPattern: View
    private lateinit var cardJobFit: View
    private lateinit var jobFitContainer: LinearLayout
    private lateinit var txtSpeakingSpeedScore: TextView
    private lateinit var txtConfidenceScore: TextView
    private lateinit var txtIntonationScore: TextView
    private lateinit var txtVolumeScore: TextView
    private lateinit var txtFillerAvg: TextView
    private lateinit var txtPauseAvg: TextView
    private lateinit var progressSpeakingSpeed: ProgressBar
    private lateinit var progressConfidence: ProgressBar
    private lateinit var progressIntonation: ProgressBar
    private lateinit var progressVolume: ProgressBar
    private var dashboardProvidedInterviews = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        setupSideMenu()
        bindViews()
        setupTitle()
        setupBottomButtons()
        BottomNavHelper.apply(this, BottomNavHelper.Tab.DASHBOARD)
        setupRecentInterviewList()
        loadDashboard()
        loadRecentInterviews()

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
        interviewLineChart = findViewById(R.id.interviewLineChart)
        txtInterviewScoreImprovement = findViewById(R.id.txtInterviewScoreImprovement)
        txtInterviewCount = findViewById(R.id.txtInterviewCount)
        txtInterviewGrowthRate = findViewById(R.id.txtInterviewGrowthRate)
        txtEmptyInterviewGrowth = findViewById(R.id.txtEmptyInterviewGrowth)
        interviewGrowthStats = findViewById(R.id.interviewGrowthStats)
        radarChart = findViewById(R.id.radarChart)
        rvRecentInterview = findViewById(R.id.rvRecentInterview)
        txtEmptyRecentInterview = findViewById(R.id.txtEmptyRecentInterview)
        cardSpeechPattern = findViewById(R.id.cardSpeechPattern)
        cardJobFit = findViewById(R.id.cardJobFit)
        jobFitContainer = findViewById(R.id.jobFitContainer)
        txtSpeakingSpeedScore = findViewById(R.id.txtSpeakingSpeedScore)
        txtConfidenceScore = findViewById(R.id.txtConfidenceScore)
        txtIntonationScore = findViewById(R.id.txtIntonationScore)
        txtVolumeScore = findViewById(R.id.txtVolumeScore)
        txtFillerAvg = findViewById(R.id.txtFillerAvg)
        txtPauseAvg = findViewById(R.id.txtPauseAvg)
        progressSpeakingSpeed = findViewById(R.id.progressSpeakingSpeed)
        progressConfidence = findViewById(R.id.progressConfidence)
        progressIntonation = findViewById(R.id.progressIntonation)
        progressVolume = findViewById(R.id.progressVolume)
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            recreate()
        }
        findViewById<TextView>(R.id.btnRoadmap).setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
        }
        findViewById<TextView>(R.id.btnhs).setOnClickListener {
            startActivity(Intent(this, RoadmapActivity::class.java))
        }
        findViewById<TextView>(R.id.btnAllRecords).setOnClickListener {
            startActivity(Intent(this, AllInterviewActivity::class.java))
        }
    }

    private fun setupRecentInterviewList() {
        rvRecentInterview.layoutManager = LinearLayoutManager(this)
        rvRecentInterview.adapter = InterviewAdapter(emptyList()) {}
        showRecentInterviewEmpty(true)
    }

    private fun loadDashboard() {
        val token = authToken() ?: return
        RetrofitClient.api.getDashboard("Bearer $token").enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val body = response.body()
                if (!response.isSuccessful || body == null) return
                applyDashboard(body)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "대시보드 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadRecentInterviews() {
        lifecycleScope.launch {
            try {
                val list = InterviewRepository.loadInterviewItems(this@DashboardActivity)
                if (dashboardProvidedInterviews) return@launch

                bindRecentInterviewItems(list.take(3))

                if (txtAvgInterviewScore.text.isNullOrBlank() || txtAvgInterviewScore.text == "-") {
                    val avg = list.map { it.score }.filter { it > 0 }.average().takeIf { !it.isNaN() }
                    txtAvgInterviewScore.text = avg?.toInt()?.toString() ?: "-"
                }
                if (txtTotalCount.text == "0회") txtTotalCount.text = "${list.size}회"
            } catch (e: Exception) {
                showRecentInterviewEmpty(true)
                Toast.makeText(this@DashboardActivity, "최근 면접 기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyDashboard(data: JsonObject) {
        val history = data.arrayOf("history").objects()

        txtAvgDocumentScore.text = data.doubleOf("avg_document_score")?.toInt()?.toString() ?: "-"
        txtAvgInterviewScore.text = data.doubleOf("avg_interview_score")?.toInt()?.toString() ?: "-"
        txtScoreImprovement.text = formatSigned(data.doubleOf("score_improvement"))
        txtTotalCount.text = "${data.intOf("total_count") ?: 0}회"
        txtGrowthRate.text = data.doubleOf("growth_rate")?.let { "${it.toInt()}%" } ?: "0%"

        setupGrowthChart(history)
        setupInterviewGrowthChart(
            data.arrayOf("interview_history").objects(),
            data.doubleOf("interview_score_improvement"),
            data.intOf("interview_count"),
            data.doubleOf("interview_growth_rate")
        )
        setupRadarChart(data.objectOf("competency_avg"))
        bindSpeechPattern(data.objectOf("speech_pattern"))
        bindJobFit(data.get("job_fit"))
        bindDashboardInterviewHistory(data.arrayOf("interview_history").objects())
    }

    private fun setupGrowthChart(history: List<JsonObject>) {
        val entries = history.mapIndexed { index, item ->
            Entry(
                index.toFloat(),
                (item.doubleOf("total_score", "resume_score", "cover_letter_score") ?: 0.0).toFloat()
            )
        }
        val dataSet = LineDataSet(entries, "성장 추이").apply {
            color = Color.parseColor("#3950E7")
            setCircleColor(Color.parseColor("#3950E7"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.textColor = Color.parseColor("#999999")
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisLeft.textColor = Color.parseColor("#999999")
            invalidate()
        }
    }

    private fun setupInterviewGrowthChart(
        history: List<JsonObject>,
        scoreImprovement: Double?,
        interviewCount: Int?,
        growthRate: Double?
    ) {
        txtInterviewScoreImprovement.text = scoreImprovement?.let { formatSigned(it) } ?: "-"
        txtInterviewCount.text = "${interviewCount ?: history.size}회"
        txtInterviewGrowthRate.text = growthRate?.let { "${it.toInt()}%" } ?: "-"

        if (history.isEmpty()) {
            interviewLineChart.visibility = View.GONE
            interviewGrowthStats.visibility = View.GONE
            txtEmptyInterviewGrowth.visibility = View.VISIBLE
            return
        }

        interviewLineChart.visibility = View.VISIBLE
        interviewGrowthStats.visibility = View.VISIBLE
        txtEmptyInterviewGrowth.visibility = View.GONE

        val entries = history.mapIndexed { index, item ->
            Entry(
                index.toFloat(),
                (item.doubleOf("score", "overall_score", "total_score") ?: 0.0).toFloat()
            )
        }
        val dataSet = LineDataSet(entries, "면접 성장 추이").apply {
            color = Color.parseColor("#5E6DFF")
            setCircleColor(Color.parseColor("#5E6DFF"))
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        interviewLineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            axisRight.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.textColor = Color.parseColor("#999999")
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisLeft.textColor = Color.parseColor("#999999")
            invalidate()
        }
    }

    private fun setupRadarChart(avg: JsonObject?) {
        val scores = floatArrayOf(
            (avg?.doubleOf("technical") ?: 0.0).toFloat(),
            (avg?.doubleOf("communication") ?: 0.0).toFloat(),
            (avg?.doubleOf("problem_solving") ?: 0.0).toFloat(),
            (avg?.doubleOf("collaboration") ?: 0.0).toFloat(),
            (avg?.doubleOf("passion") ?: 0.0).toFloat()
        )
        radarChart.setData(listOf("기술", "소통", "문제해결", "협업", "열정"), scores, 120f)
    }

    private fun bindSpeechPattern(speech: JsonObject?) {
        if (speech == null) {
            cardSpeechPattern.visibility = View.GONE
            return
        }
        cardSpeechPattern.visibility = View.VISIBLE
        bindSpeechMetric(
            txtSpeakingSpeedScore,
            progressSpeakingSpeed,
            speech.doubleOf("speaking_speed_score")
        )
        bindSpeechMetric(
            txtConfidenceScore,
            progressConfidence,
            speech.doubleOf("confidence_score")
        )
        bindSpeechMetric(
            txtIntonationScore,
            progressIntonation,
            speech.doubleOf("intonation_score")
        )
        bindSpeechMetric(
            txtVolumeScore,
            progressVolume,
            speech.doubleOf("volume_score")
        )
        txtFillerAvg.text = formatDecimal(speech.doubleOf("filler_word_avg"), 1)
        txtPauseAvg.text = formatDecimal(speech.doubleOf("pause_count_avg"), 1)
    }

    private fun bindSpeechMetric(label: TextView, progress: ProgressBar, value: Double?) {
        val score = value?.toInt()?.coerceIn(0, 100) ?: 0
        label.text = "$score/100"
        progress.progress = score
    }

    private fun bindJobFit(element: JsonElement?) {
        val jobs = jobFitObjects(element)
        if (jobs.isEmpty()) {
            cardJobFit.visibility = View.GONE
            return
        }

        cardJobFit.visibility = View.VISIBLE
        jobFitContainer.removeAllViews()
        jobs.take(4).forEach { job ->
            jobFitContainer.addView(jobFitCard(job))
        }
    }

    private fun bindDashboardInterviewHistory(items: List<JsonObject>) {
        if (items.isEmpty()) return
        val interviews = items.take(3).mapNotNull { item ->
            val sessionId = item.intOf("session_id") ?: return@mapNotNull null
            InterviewItem(
                session_id = sessionId,
                date = item.stringOf("date") ?: "날짜 미정",
                meta = "${item.intOf("round") ?: sessionId}회차 면접",
                score = item.intOf("score") ?: 0
            )
        }
        if (interviews.isEmpty()) return
        dashboardProvidedInterviews = true
        bindRecentInterviewItems(interviews)
    }

    private fun bindRecentInterviewItems(items: List<InterviewItem>) {
        showRecentInterviewEmpty(items.isEmpty())
        rvRecentInterview.adapter = InterviewAdapter(items) { selectedItem ->
            startActivity(Intent(this@DashboardActivity, DetailActivity::class.java).apply {
                putExtra("ITEM_DATA", selectedItem)
            })
        }
    }

    private fun showRecentInterviewEmpty(isEmpty: Boolean) {
        rvRecentInterview.visibility = if (isEmpty) View.GONE else View.VISIBLE
        txtEmptyRecentInterview.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun jobFitCard(job: JsonObject): View {
        val title = formatJobTitle(
            job.stringOf("target_job", "job_title", "job", "recommended_job", "position", "name")
                ?: "추천 직무"
        )
        val score = job.doubleOf("fit_score", "score", "match_rate", "fit_rate", "fitScore", "match_score")
            ?.toInt()
            ?.coerceIn(0, 100)
            ?: 0
        val userSkills = job.skillsOf("user_skills", "skills", "owned_skills", "extracted_skills")
        val requiredSkills = job.skillsOf("required_skills", "needed_skills", "target_job_skills")
        val missingSkills = job.skillsOf("missing_skills", "lack_skills")

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            setBackgroundResource(R.drawable.bg_dashboard_job_card)

            val topRow = LinearLayout(this@DashboardActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(this@DashboardActivity).apply {
                text = title
                textSize = 17f
                setTextColor(Color.parseColor("#222222"))
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            topRow.addView(LinearLayout(this@DashboardActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                addView(TextView(this@DashboardActivity).apply {
                    text = "${score}%"
                    textSize = 34f
                    setTextColor(Color.parseColor(if (score >= 90) "#00C425" else "#4357FF"))
                })
                addView(TextView(this@DashboardActivity).apply {
                    text = "적합도"
                    textSize = 11f
                    setTextColor(Color.parseColor("#999999"))
                    translationX = dp(8).toFloat()
                })
            })
            addView(topRow)

            addView(ProgressBar(this@DashboardActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = score
                progressDrawable = getDrawable(R.drawable.bg_dashboard_progress)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(9)
            ).apply { topMargin = dp(12) })

            addSkillSection("◎ 보유 스킬", userSkills)
            val needSkills = missingSkills.ifEmpty { requiredSkills }
            addSkillSection("↗ 필요 스킬", needSkills)
        }.also { view ->
            view.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
    }

    private fun LinearLayout.addSkillSection(title: String, skills: List<String>) {
        addView(TextView(this@DashboardActivity).apply {
            text = title
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, dp(16), 0, 0)
        })
        val row = LinearLayout(this@DashboardActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        val shown = skills.take(3).ifEmpty { listOf("데이터 없음") }
        shown.forEach { skill ->
            row.addView(TextView(this@DashboardActivity).apply {
                text = skill
                textSize = 11f
                setTextColor(Color.WHITE)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setBackgroundResource(R.drawable.bg_dashboard_black_chip)
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(6) })
        }
        addView(row)
    }

    private fun formatJobTitle(raw: String): String {
        return when (raw.lowercase()) {
            "uiux", "ui/ux" -> "UI/UX 개발자"
            "frontend", "front-end", "프론트엔드" -> "프론트엔드 개발자"
            "backend", "back-end", "백엔드" -> "백엔드 개발자"
            else -> raw
        }
    }

    private fun formatSigned(value: Double?): String {
        val intValue = value?.toInt() ?: 0
        return if (intValue > 0) "+$intValue" else intValue.toString()
    }

    private fun formatDecimal(value: Double?, digits: Int): String {
        val number = value ?: 0.0
        return if (digits <= 0) {
            number.toInt().toString()
        } else {
            "%.${digits}f".format(number)
        }
    }

    private fun jobFitObjects(element: JsonElement?): List<JsonObject> {
        if (element == null || element.isJsonNull) return emptyList()
        if (element.isJsonArray) return element.asJsonArray.objects()
        if (!element.isJsonObject) return emptyList()

        val obj = element.asJsonObject
        val nestedKeys = listOf("jobs", "recommendations", "items", "job_fits", "data")
        nestedKeys.forEach { key ->
            val nested = obj.get(key)
            if (nested != null && nested.isJsonArray) return nested.asJsonArray.objects()
        }
        return listOf(obj)
    }

    private fun JsonObject.doubleOf(vararg keys: String): Double? {
        return keys.firstNotNullOfOrNull { key ->
            val value = get(key) ?: return@firstNotNullOfOrNull null
            when {
                value.isJsonNull -> null
                value.isJsonPrimitive && value.asJsonPrimitive.isNumber -> value.asDouble
                value.isJsonPrimitive && value.asJsonPrimitive.isString -> value.asString
                    .replace("%", "")
                    .trim()
                    .toDoubleOrNull()
                else -> null
            }
        }
    }

    private fun JsonObject.intOf(vararg keys: String): Int? = doubleOf(*keys)?.toInt()

    private fun JsonObject.stringOf(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            val value = get(key) ?: return@firstNotNullOfOrNull null
            if (value.isJsonNull || !value.isJsonPrimitive) {
                null
            } else {
                value.asString.takeIf { it.isNotBlank() && it != "null" }
            }
        }
    }

    private fun JsonObject.objectOf(vararg keys: String): JsonObject? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { it.isJsonObject }?.asJsonObject
        }
    }

    private fun JsonObject.arrayOf(vararg keys: String): JsonArray? {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.takeIf { it.isJsonArray }?.asJsonArray
        }
    }

    private fun JsonArray?.objects(): List<JsonObject> {
        return this?.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }.orEmpty()
    }

    private fun JsonArray?.strings(): List<String> {
        return this?.flatMap { it.skillStrings() }.orEmpty()
    }

    private fun JsonObject.skillsOf(vararg keys: String): List<String> {
        return keys.firstNotNullOfOrNull { key ->
            get(key)?.skillStrings()?.takeIf { it.isNotEmpty() }
        }.orEmpty()
    }

    private fun JsonElement.skillStrings(): List<String> {
        if (isJsonNull) return emptyList()
        if (isJsonArray) return asJsonArray.flatMap { it.skillStrings() }
        if (isJsonObject) {
            val obj = asJsonObject
            return obj.stringOf("skill", "name", "title", "label", "text")?.let { listOf(it) }
                ?: obj.arrayOf("items", "skills", "data")?.strings()
                ?: emptyList()
        }
        if (isJsonPrimitive) {
            return asString
                .split(",", "/", "|", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() && it != "null" }
        }
        return emptyList()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun authToken(): String? {
        return SessionManager.getToken(this) ?: Session.accessToken
    }

    private fun setupTitle() {
        val title = findViewById<TextView>(R.id.txtTitle)
        val text = "Career AI Coach"
        val spannable = SpannableString(text)
        val blue = Color.parseColor("#3950E7")
        val gray = Color.parseColor("#8A8A8A")
        spannable.setSpan(ForegroundColorSpan(blue), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 1, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 7, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 8, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(blue), 10, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable
    }
}
