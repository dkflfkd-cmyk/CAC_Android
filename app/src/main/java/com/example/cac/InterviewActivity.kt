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
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.Toast
import com.example.cac.data.ResumeRequest
import com.example.cac.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.example.cac.data.SessionRequest
import com.example.cac.data.SessionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import com.example.cac.network.InterviewSessionRequest
class InterviewActivity : AppCompatActivity() {
    private var selectedJob: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interview)

        setupSideMenu()

        // 챗화면 전환
        val btnStart = findViewById<android.view.View>(R.id.btnStartCircle)

        btnStart.setOnClickListener {
            btnStart.isEnabled = false

            val realUserId = SessionManager.getUserId(this) ?: "1"
            val realTargetJob = selectedJob ?: "안드로이드 개발자"


            val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("USER_ID", realUserId)
                apply()
            }

            val request = InterviewSessionRequest(
                user_id = realUserId,
                target_job = realTargetJob
            )


            RetrofitClient.api.createInterviewSession(request).enqueue(object : Callback<SessionResponse> {
                override fun onResponse(call: Call<SessionResponse>, response: Response<SessionResponse>) {
                    btnStart.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        android.util.Log.d("SERVER_DATA", "면접 세션 생성 성공: ${data.sessionId}")

                        val intent = Intent(this@InterviewActivity, InterviewChatActivity::class.java).apply {
                            putExtra("session_id", data.sessionId)
                            val firstQuestion = data.firstQuestion ?: "반갑습니다. 면접을 시작해볼까요?"
                            putExtra("first_question", firstQuestion)
                        }
                        startActivity(intent)
                    } else {
                        android.util.Log.e("API_ERROR", "에러 코드: ${response.code()}")
                        Toast.makeText(this@InterviewActivity, "면접 세션 생성 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SessionResponse>, t: Throwable) {
                    btnStart.isEnabled = true
                    android.util.Log.e("API_FAILURE", "통신 실패: ${t.message}")
                    Toast.makeText(this@InterviewActivity, "서버 연결 오류", Toast.LENGTH_SHORT).show()
                }
            })
        }

        val btnMy = findViewById<TextView>(R.id.btnNoticev2)
        btnMy.setOnClickListener {
            val intent = Intent(this, MYActivity::class.java)
            startActivity(intent)
        }

        val btnhome = findViewById<TextView>(R.id.btnNoticeh)
        btnhome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnresume = findViewById<TextView>(R.id.btnresume)
        btnresume.setOnClickListener {
            val intent = Intent(this, cardResumeActivity::class.java)
            startActivity(intent)
        }

        val btnDS = findViewById<TextView>(R.id.btnNoticev)
        btnDS.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }

        fun showPicker(title: String, items: Array<String>, onPick: (String) -> Unit) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items) { _, which ->
                    onPick(items[which])
                }
                .show()
        }

        var selectedJob: String? = null
        var selectedType: String? = null
        var selectedCount: Int? = null

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
