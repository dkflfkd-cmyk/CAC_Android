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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.InterviewItem
import com.example.cac.data.InterviewRepository
import com.example.cac.data.ResumeRepository
import com.example.cac.data.SavedQuestionItem
import com.example.cac.data.SavedQuestionRepository
import com.example.cac.network.ResumeItem
import com.example.cac.network.RetrofitClient
import com.example.cac.ui.adapter.InterviewAdapter
import com.example.cac.ui.adapter.ResumeAdapter
import com.example.cac.ui.adapter.SavedQuestionAdapter
import kotlinx.coroutines.launch

class MYActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_myactivity)

        setupSideMenu()
        BottomNavHelper.apply(this, BottomNavHelper.Tab.MY)
        findViewById<RecyclerView>(R.id.rvInterview).layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.rvResume).layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.rvSavedQuestions).layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.btnInterviewAll).setOnClickListener {
            startActivity(Intent(this, AllInterviewActivity::class.java))
        }
        findViewById<TextView>(R.id.btnResumeAll).setOnClickListener {
            startActivity(Intent(this, AllResumeActivity::class.java))
        }
        findViewById<TextView>(R.id.btnSavedQuestionsAll).setOnClickListener {
            startActivity(Intent(this, AllSavedQuestionsActivity::class.java))
        }
        findViewById<TextView>(R.id.btnInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }

        setupTitle()
        applyDefaultProfile()
        loadMyPage()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadMyPage() {
        val token = SessionManager.getToken(this)
        if (token.isNullOrBlank()) return

        RetrofitClient.api.me("Bearer $token").enqueue(object : retrofit2.Callback<Map<String, Any>> {
            override fun onResponse(
                call: retrofit2.Call<Map<String, Any>>,
                response: retrofit2.Response<Map<String, Any>>
            ) {
                if (!response.isSuccessful) {
                    if (response.code() == 401 || response.code() == 403) {
                        handleExpiredSession()
                        return
                    }
                    loadFallbackSections()
                    return
                }
                val body = response.body()
                if (body == null) {
                    loadFallbackSections()
                    return
                }

                bindProfile(body)
                bindResumePreview(parseResumeItems(body["resumes"]).take(3))
                bindInterviewPreview(parseInterviewItems(body["interviews"]).take(3))
                bindSavedQuestionPreview(parseSavedQuestions(body["saved_questions"]).take(2))
            }

            override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                android.util.Log.e("MYActivity", "내 정보 로드 실패", t)
                loadFallbackSections()
            }
        })
    }

    private fun loadFallbackSections() {
        loadResumes()
        loadInterviews()
        loadSavedQuestions()
    }

    private fun bindProfile(body: Map<String, Any>) {
        val username = fieldOf(body, "username", "name", "nickname", "user_id")
        val grade = fieldOf(body, "grade", "education", "school_status", "academic_status")
        val major = fieldOf(body, "major", "department")
        val goal = fieldOf(body, "target_job", "goal", "desired_job", "job")

        findViewById<TextView>(R.id.txtName).text = username ?: "사용자"
        findViewById<TextView>(R.id.txtStatus1).text = "학년\n${grade ?: "미등록"}"
        findViewById<TextView>(R.id.txtStatus2).text = "전공\n${major ?: "미등록"}"
        findViewById<TextView>(R.id.txtStatus3).text = "목표\n${goal ?: "미등록"}"
    }

    private fun applyDefaultProfile() {
        findViewById<TextView>(R.id.txtName).text = SessionManager.getUserId(this) ?: "사용자"
        findViewById<TextView>(R.id.txtStatus1).text = "학년\n불러오는 중"
        findViewById<TextView>(R.id.txtStatus2).text = "전공\n불러오는 중"
        findViewById<TextView>(R.id.txtStatus3).text = "목표\n불러오는 중"
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("현재 계정에서 로그아웃할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("로그아웃") { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        Session.accessToken = null
        SessionManager.logout(this)
        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun handleExpiredSession() {
        Session.accessToken = null
        SessionManager.logout(this)
        Toast.makeText(this, "로그인이 만료되었습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun loadResumes() {
        lifecycleScope.launch {
            try {
                val resumes = ResumeRepository.loadResumeItems(this@MYActivity, 3)
                bindResumePreview(resumes)
            } catch (e: Exception) {
                bindResumePreview(emptyList())
                android.util.Log.e("MYActivity", "이력서 목록 로드 실패", e)
            }
        }
    }

    private fun loadInterviews() {
        lifecycleScope.launch {
            try {
                val list = InterviewRepository.loadInterviewItems(this@MYActivity)
                bindInterviewPreview(list.take(3))
            } catch (e: Exception) {
                bindInterviewPreview(emptyList())
                android.util.Log.e("MYActivity", "면접 기록 로드 실패", e)
            }
        }
    }

    private fun loadSavedQuestions() {
        lifecycleScope.launch {
            try {
                val questions = SavedQuestionRepository.loadSavedQuestionItems(this@MYActivity)
                bindSavedQuestionPreview(questions.take(2))
            } catch (e: Exception) {
                bindSavedQuestionPreview(emptyList())
                android.util.Log.e("MYActivity", "저장 질문 로드 실패", e)
            }
        }
    }

    private fun bindResumePreview(items: List<ResumeItem>) {
        val rv = findViewById<RecyclerView>(R.id.rvResume)
        val empty = findViewById<TextView>(R.id.txtEmptyResume)
        if (items.isEmpty()) {
            rv.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            empty.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = ResumeAdapter(items) { item ->
                val resumeId = item.resumeId ?: return@ResumeAdapter
                startActivity(Intent(this@MYActivity, cardResumeActivity::class.java).apply {
                    putExtra("resume_id", resumeId)
                    putExtra("file_name", item.fileName)
                    putExtra("created_at", item.date)
                    putExtra("target_job", item.targetJob)
                    putExtra("pdf_url", item.pdfUrl)
                })
            }
        }
    }

    private fun bindInterviewPreview(items: List<InterviewItem>) {
        val rv = findViewById<RecyclerView>(R.id.rvInterview)
        val empty = findViewById<TextView>(R.id.txtEmptyInterview)
        if (items.isEmpty()) {
            rv.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            empty.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = InterviewAdapter(items) { selectedItem ->
                val intent = Intent(this@MYActivity, DetailActivity::class.java)
                intent.putExtra("ITEM_DATA", selectedItem)
                startActivity(intent)
            }
        }
    }

    private fun bindSavedQuestionPreview(items: List<SavedQuestionItem>) {
        val rv = findViewById<RecyclerView>(R.id.rvSavedQuestions)
        val empty = findViewById<TextView>(R.id.txtEmptyView)
        if (items.isEmpty()) {
            rv.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            empty.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = SavedQuestionAdapter(items)
        }
    }

    private fun parseResumeItems(value: Any?): List<ResumeItem> {
        return (value as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.toResumeItem() }
    }

    private fun parseInterviewItems(value: Any?): List<InterviewItem> {
        return (value as? List<*>).orEmpty().mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val sessionId = map.intOf("session_id", "id") ?: return@mapNotNull null
            InterviewItem(
                session_id = sessionId,
                date = map.stringOf("created_at", "date") ?: "날짜 미정",
                meta = "${map.stringOf("target_job", "job") ?: "직무 미정"} / 세션 $sessionId",
                score = map.intOf("overall_score", "score") ?: 0,
                feedback = map.stringOf("feedback"),
                pdf_url = map.stringOf("pdf_url")
            )
        }
    }

    private fun parseSavedQuestions(value: Any?): List<SavedQuestionItem> {
        return (value as? List<*>).orEmpty().mapNotNull { item ->
            when (item) {
                is String -> SavedQuestionItem(questionText = item)
                is Map<*, *> -> {
                    val questionText = item.stringOf("question_text", "content", "question", "text", "title")
                        ?: return@mapNotNull null
                    SavedQuestionItem(
                        questionId = item.intOf("question_id", "id"),
                        questionText = questionText,
                        questionType = item.stringOf("question_type", "type"),
                        targetJob = item.stringOf("target_job", "job"),
                        createdAt = item.stringOf("created_at", "date")
                    )
                }
                else -> item?.toString()?.let { SavedQuestionItem(questionText = it) }
            }
        }.filter { it.questionText.isNotBlank() && it.questionText != "null" }
    }

    private fun fieldOf(body: Map<String, Any>, vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            body[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
        }
    }

    private fun Map<*, *>.toResumeItem(): ResumeItem {
        return ResumeItem(
            resumeId = intOf("resume_id", "id", "analysis_id"),
            fileName = stringOf("original_filename", "filename", "file_name", "title", "name") ?: "이력서",
            date = stringOf("created_at", "date", "uploaded_at"),
            targetJob = stringOf("target_job", "job"),
            pdfUrl = stringOf("pdf_url", "url")
        )
    }

    private fun Map<*, *>.stringOf(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
        }
    }

    private fun Map<*, *>.intOf(vararg keys: String): Int? {
        return keys.firstNotNullOfOrNull { key ->
            when (val value = this[key]) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
        }
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
