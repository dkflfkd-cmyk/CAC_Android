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




class InterviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interview)


        // 챗화면 전환
        val btnStart = findViewById<android.view.View>(R.id.btnStartCircle)

        btnStart.setOnClickListener {
            val intent = Intent(this, InterviewChatActivity::class.java)


            startActivity(intent)
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


        // 선택값 저장
        var selectedJob: String? = null
        var selectedType: String? = null
        var selectedCount: Int? = null







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
