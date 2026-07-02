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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cac.data.InterviewItem
import com.example.cac.ui.adapter.InterviewAdapter
import com.example.cac.ui.adapter.ResumeAdapter
import com.example.cac.SessionManager
import com.example.cac.network.RetrofitClient
import com.example.cac.ui.adapter.SavedQuestionAdapter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MYActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_myactivity)

        //면접 기록 전체 보기 버튼
        findViewById<TextView>(R.id.btnInterviewAll).setOnClickListener {
            val intent = Intent(this, AllInterviewActivity::class.java)
            startActivity(intent)
        }

        // 이력서 기록 전체 보기 버튼
        findViewById<TextView>(R.id.btnResumeAll).setOnClickListener {
            val intent = Intent(this, AllResumeActivity::class.java)
            startActivity(intent)
        }

        //확인
        android.util.Log.d("MY_DEBUG", "calling loadMeAndApply()")
        loadMeAndApply()



// 면접 기록
        val rvInterview = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvInterview)
        val rvResume = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvResume)

        rvInterview.layoutManager = LinearLayoutManager(this)
        rvResume.layoutManager = LinearLayoutManager(this)





//면접 기록 끝

        val btnInterview = findViewById<TextView>(R.id.btnInterview)
        btnInterview.setOnClickListener {
            val intent = Intent(this, InterviewActivity::class.java)
            startActivity(intent)
        }

        val btnhome = findViewById<TextView>(R.id.btnNoticeh)
        btnhome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnDS = findViewById<TextView>(R.id.btnNoticev)
        btnDS.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }




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
        spannable.setSpan(ForegroundColorSpan(gray), 11, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        title.text = spannable


        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        title.text = spannable

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //확인

        android.util.Log.d("MY_DEBUG", "MYActivity onCreate called")

    }





    private fun loadResumes() {
        val token = SessionManager.getToken(this) ?: return
        val authHeader = "Bearer $token"

        lifecycleScope.launch {
            try {

                val response = RetrofitClient.api.getResumes(authHeader)
                val fullList = response.resumes ?: emptyList()


                val previewList = fullList.take(3)

                val rvResume = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvResume)


                rvResume.adapter = ResumeAdapter(previewList)

            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "이력서 로드 실패: ${e.message}")
            }
        }
    }
    // MYActivity.kt 파일
    private fun loadInterviews() {
        val token = SessionManager.getToken(this) ?: return
        val authHeader = "Bearer $token"

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getInterviews(authHeader)


                val list = response.interviews?.map { item ->
                    com.example.cac.data.InterviewItem(
                        session_id = item.session_id ?: -1,
                        date = item.created_at ?: "날짜 미정",
                        meta = "${item.target_job ?: "직무 미정"} / ${item.session_id ?: 0}번",
                        score = item.overall_score ?: 0,
                        feedback = item.feedback, // 서버 데이터의 피드백을 UI용 데이터로 복사
                        pdf_url = item.pdf_url    // 서버 데이터의 URL을 UI용 데이터로 복사
                    )
                } ?: emptyList()

                val rvInterview = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvInterview)


                rvInterview.adapter = InterviewAdapter(list.take(3)) { selectedItem ->
                    val intent = Intent(this@MYActivity, DetailActivity::class.java)
                    intent.putExtra("ITEM_DATA", selectedItem)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "면접 기록 로드 실패: ${e.message}")
            }
        }
    }

    private fun loadMeAndApply() {

        //확인
        android.util.Log.d("MY_DEBUG", "loadMeAndApply start token=" + SessionManager.getToken(this))


        val token = SessionManager.getToken(this)



        if (token.isNullOrBlank()) {
            android.util.Log.d("ME_RESPONSE", "토큰 없음")
            return
        }

        RetrofitClient.api.me("Bearer $token")
            .enqueue(object : retrofit2.Callback<Map<String, Any>> {

                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    if (!response.isSuccessful) {
                        android.util.Log.d("ME_RESPONSE", "실패 code=${response.code()}")
                        return
                    }

                    val body = response.body()
                    android.util.Log.d("ME_RESPONSE", body.toString())

                    val userId = body?.get("user_id")?.toString() ?: return

                    val sessionId = intent.getIntExtra("session_id", -1) // -1은 기본값

                    if (sessionId != -1) {
                        loadSavedQuestions(sessionId) // 함수 호출
                    } else {}

                    loadResumes()
                    loadInterviews()

                    val username = body?.get("username")?.toString()
                    val grade = body?.get("grade")?.toString()
                    val major = body?.get("major")?.toString()
                    val goal = body?.get("target_job")?.toString()

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtName).text =
                        username ?: "홍길동"

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtStatus1).text =
                        "학년\n${grade ?: "미등록"}"

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtStatus2).text =
                        "전공\n${major ?: "미등록"}"

                    findViewById<android.widget.TextView>(com.example.cac.R.id.txtStatus3).text =
                        "목표\n${goal ?: "미등록"}"
                }

                override fun onFailure(
                    call: retrofit2.Call<Map<String, Any>>,
                    t: Throwable
                ) {
                    android.util.Log.d("ME_RESPONSE", "네트워크 오류: ${t.message}")
                }
            })
    }



    private fun checkServerResponse(userId: String) {
        val token = SessionManager.getToken(this)
        if (token == null) {
            android.util.Log.e("API_ERROR", "토큰이 없습니다. 로그인을 다시 해주세요.")
            return
        }

        val authHeader = "Bearer $token"

        // 1. 코루틴으로 감싸기
        lifecycleScope.launch {
            try {
                // 2. enqueue 없이 직접 호출 (결과를 바로 리스트로 받음)
                val list = RetrofitClient.api.getSavedQuestions(authHeader, userId)

                val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSavedQuestions)
                val txtEmpty = findViewById<android.widget.TextView>(R.id.txtEmptyView)

                android.util.Log.d("API_DEBUG", "서버 응답 리스트 크기: ${list.size}")

                if (list.isEmpty()) {
                    txtEmpty.visibility = android.view.View.VISIBLE
                    rv.visibility = android.view.View.GONE
                } else {
                    txtEmpty.visibility = android.view.View.GONE
                    rv.visibility = android.view.View.VISIBLE
                    // 레이아웃 매니저가 설정되지 않았다면 설정
                    if (rv.layoutManager == null) {
                        rv.layoutManager = LinearLayoutManager(this@MYActivity)
                    }
                    rv.adapter = SavedQuestionAdapter(list)
                }
            } catch (e: Exception) {
                // 3. 에러 발생 시 처리 (onFailure 대신 try-catch)
                android.util.Log.e("API_DEBUG", "연결 에러: ${e.message}")
                findViewById<android.widget.TextView>(R.id.txtEmptyView).visibility = android.view.View.VISIBLE
                findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSavedQuestions).visibility = android.view.View.GONE
            }
        }
    }
    private fun loadSavedQuestions(sessionId: Int) { // 파라미터를 sessionId로 변경
        val token = SessionManager.getToken(this) ?: return
        val authHeader = "Bearer $token"

        lifecycleScope.launch {
            try {
                // 이제 수정된 API 함수를 호출합니다
                val list = RetrofitClient.api.getQuestions(authHeader, sessionId)

                val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSavedQuestions)
                val txtEmpty = findViewById<android.widget.TextView>(R.id.txtEmptyView)

                if (list.isEmpty()) {
                    txtEmpty.visibility = android.view.View.VISIBLE
                    rv.visibility = android.view.View.GONE
                } else {
                    txtEmpty.visibility = android.view.View.GONE
                    rv.visibility = android.view.View.VISIBLE
                    // 어댑터 연결 (가져온 list 전달)
                    rv.layoutManager = LinearLayoutManager(this@MYActivity)
                    rv.adapter = SavedQuestionAdapter(list)
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "질문 로드 실패: ${e.message}")
                // 에러 처리
            }
        }
    }
}