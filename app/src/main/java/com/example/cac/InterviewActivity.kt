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

class InterviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interview)


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

        val btnDS = findViewById<TextView>(R.id.btnNoticev)
        btnDS.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }


        //목록 펼치기
        val boxJob = findViewById<View>(R.id.boxJob)
        val boxType = findViewById<View>(R.id.boxType)
        val boxCount = findViewById<View>(R.id.boxCount)

        val txtJob = findViewById<TextView>(R.id.txtJobHint)
        val txtType = findViewById<TextView>(R.id.txtTypeHint)
        val txtCount = findViewById<TextView>(R.id.txtCountHint)

        fun showPicker(title: String, items: Array<String>, onPick: (String) -> Unit) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(items) { _, which ->
                    onPick(items[which])
                }
                .show()
        }


        // 선택값 저장
        var selectedJob: String? = null
        var selectedType: String? = null
        var selectedCount: Int? = null


        boxJob.setOnClickListener {
            val items = arrayOf("백엔드", "프론트엔드", "모바일", "데이터/AI")
            AlertDialog.Builder(this)
                .setTitle("직무 선택")
                .setItems(items) { _, which ->
                    selectedJob = items[which]
                    txtJob.text = selectedJob
                    txtJob.setTextColor(android.graphics.Color.parseColor("#111111"))
                }
                .show()
        }

// 질문 유형
        boxType.setOnClickListener {
            val items = arrayOf("기술", "인성", "프로젝트", "CS")
            AlertDialog.Builder(this)
                .setTitle("질문 유형")
                .setItems(items) { _, which ->
                    selectedType = items[which]
                    txtType.text = selectedType
                    txtType.setTextColor(android.graphics.Color.parseColor("#111111"))
                }
                .show()
        }

// 질문 개수
        boxCount.setOnClickListener {
            val items = arrayOf("3", "5", "10")
            AlertDialog.Builder(this)
                .setTitle("질문 개수")
                .setItems(items) { _, which ->
                    selectedCount = items[which].toInt()
                    txtCount.text = "${selectedCount}개"
                    txtCount.setTextColor(android.graphics.Color.parseColor("#111111"))
                }
                .show()
        }


//면접 시작 버튼
        val btnStartCircle = findViewById<View>(R.id.btnStartCircle)

        btnStartCircle.setOnClickListener {
            if (selectedJob == null || selectedType == null || selectedCount == null) {
                Toast.makeText(this, "직무/유형/개수를 모두 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = ResumeRequest(
                job = selectedJob!!,
                questionType = selectedType!!,
                questionCount = selectedCount!!
            )

            RetrofitClient.api.startInterview(request)
                .enqueue(object : retrofit2.Callback<com.example.cac.data.ResumeResponse> {

                    override fun onResponse(
                        call: retrofit2.Call<com.example.cac.data.ResumeResponse>,
                        response: retrofit2.Response<com.example.cac.data.ResumeResponse>
                    ) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            android.widget.Toast.makeText(
                                this@InterviewActivity,
                                "전송 성공: ${body?.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                this@InterviewActivity,
                                "서버 오류: ${response.code()}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(
                        call: retrofit2.Call<com.example.cac.data.ResumeResponse>,
                        t: Throwable
                    ) {
                        android.widget.Toast.makeText(
                            this@InterviewActivity,
                            "통신 실패: ${t.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                })
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
        }
    }
