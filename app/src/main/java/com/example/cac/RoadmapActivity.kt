package com.example.cac

import android.content.Intent
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.adapter.GuideCheckAdapter
import com.example.cac.adapter.RoadmapTimelineAdapter
import com.example.cac.network.CareerRoadmapRequest
import com.example.cac.network.RetrofitClient
import com.example.cac.network.RoadmapResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RoadmapActivity : AppCompatActivity() {
    private lateinit var rvTimeline: RecyclerView
    private lateinit var rvGuide: RecyclerView
    private lateinit var tvGuideStatus: TextView
    private lateinit var loadingView: View
    private var currentUserId: String = "guest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_roadmap)

        setupSideMenu()
        rvTimeline = findViewById(R.id.rvTimeline)
        rvGuide = findViewById(R.id.rvGuide)
        tvGuideStatus = findViewById(R.id.tvGuideStatus)
        loadingView = findViewById(R.id.loadingRoadmap)
        rvTimeline.layoutManager = LinearLayoutManager(this)
        rvGuide.layoutManager = LinearLayoutManager(this)

        setupTitle()
        setupBottomButtons()
        loadProfileAndRoadmap()
    }

    private fun setupBottomButtons() {
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<TextView>(R.id.btnhs).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }

    private fun loadProfileAndRoadmap() {
        val token = SessionManager.getToken(this)
        val fallbackUserId = SessionManager.getUserId(this)
        if (token.isNullOrBlank()) {
            fallbackUserId?.let {
                currentUserId = it
                loadRoadmap(it)
            }
            return
        }

        RetrofitClient.api.me("Bearer $token", 0, 0, 0).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                val body = response.body()
                val userId = body?.get("user_id")?.toString()?.takeIf { it.isNotBlank() } ?: fallbackUserId
                bindProfile(
                    username = body?.get("username")?.toString(),
                    grade = body?.get("grade")?.toString(),
                    major = body?.get("major")?.toString(),
                    targetJob = body?.get("target_job")?.toString()
                )
                userId?.let {
                    currentUserId = it
                    loadRoadmap(it)
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                fallbackUserId?.let {
                    currentUserId = it
                    loadRoadmap(it)
                }
            }
        })
    }

    private fun bindProfile(username: String?, grade: String?, major: String?, targetJob: String?) {
                findViewById<TextView>(R.id.tvProfileName)?.text = username?.takeIf { it.isNotBlank() } ?: "내 프로필"
                findViewById<TextView>(R.id.tvProfileGrade)?.text = grade?.takeIf { it.isNotBlank() } ?: "미등록"
                findViewById<TextView>(R.id.tvProfileMajor)?.text = major?.takeIf { it.isNotBlank() } ?: "미등록"
                findViewById<TextView>(R.id.tvProfileGoal)?.text = targetJob?.takeIf { it.isNotBlank() } ?: "목표 미등록"
                findViewById<TextView>(R.id.tvTargetJob)?.text = targetJob?.takeIf { it.isNotBlank() } ?: "목표 직무 미등록"
    }

    private fun loadRoadmap(userId: String) {
        setLoading(true)
        RetrofitClient.api.getCareerRoadmap(CareerRoadmapRequest(userId)).enqueue(object : Callback<RoadmapResponse> {
            override fun onResponse(call: Call<RoadmapResponse>, response: Response<RoadmapResponse>) {
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    setLoading(false)
                    Toast.makeText(this@RoadmapActivity, "커리어 경로를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    return
                }
                setLoading(false)
                bindRoadmap(body)
            }

            override fun onFailure(call: Call<RoadmapResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@RoadmapActivity, "커리어 경로를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun bindRoadmap(data: RoadmapResponse) {
        val stages = data.stages.orEmpty()
        rvTimeline.adapter = RoadmapTimelineAdapter(stages)

        val guideItems = stages.flatMap { it.recommendations.orEmpty() }
            .ifEmpty { data.recommendations.orEmpty() }
            .take(5)
        val savedDoneItems = loadDoneGuideItems(guideItems)
        tvGuideStatus.text = "${savedDoneItems.size}/${guideItems.size} 완료"
        rvGuide.adapter = GuideCheckAdapter(guideItems, savedDoneItems) { doneCount, doneItems ->
            tvGuideStatus.text = "${doneCount}/${guideItems.size} 완료"
            saveDoneGuideItems(doneItems)
        }

        val score = (data.jobMatchScore ?: data.matchScore ?: 72).coerceIn(0, 100)
        findViewById<TextView>(R.id.tvMatchScore).text = "${score}%"
        findViewById<ProgressBar>(R.id.pbJobMatch).progress = score
    }

    private fun guidePrefs() = getSharedPreferences("roadmap_guide_state", Context.MODE_PRIVATE)

    private fun guideKey(): String = "done_$currentUserId"

    private fun loadDoneGuideItems(currentItems: List<String>): Set<String> {
        val saved = guidePrefs().getStringSet(guideKey(), emptySet()).orEmpty()
        return saved.filter { currentItems.contains(it) }.toSet()
    }

    private fun saveDoneGuideItems(doneItems: Set<String>) {
        guidePrefs().edit().putStringSet(guideKey(), doneItems).apply()
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
