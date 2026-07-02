package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

import com.example.cac.R
import com.example.cac.data.InterviewItem
import com.example.cac.network.RetrofitClient
import com.example.cac.ui.adapter.ResumeAdapter
import com.example.cac.SessionManager

class AllResumeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_interview)

        val rvList = findViewById<RecyclerView>(R.id.rvInterviewList)
        rvList.layoutManager = LinearLayoutManager(this)

        // 1. 데이터 로드 (전체 리스트)
        loadAllData(rvList)

        // 2. 뒤로가기 버튼
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // 3. 하단 탭 클릭 이벤트 (필요 시 연결)
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener { /* 홈 이동 */ }
    }

    private fun loadAllData(rv: RecyclerView) {
        val token = SessionManager.getToken(this) ?: return

        lifecycleScope.launch {
            try {
                // Retrofit 호출 (.take(3) 없이 전체 호출)
                val response = RetrofitClient.api.getInterviews("Bearer $token")
                val fullList = response.interviews?.map { item ->
                    InterviewItem(
                        session_id = item.session_id ?: -1,
                        date = item.created_at ?: "",
                        meta = "${item.target_job} / ${item.session_id}번",
                        score = item.overall_score ?: 0,
                        feedback = item.feedback,
                        pdf_url = item.pdf_url
                    )
                } ?: emptyList()

                // 전체 리스트 어댑터 연결
//                rv.adapter = ResumeAdapter(fullList) { selectedItem ->
//                    val intent = Intent(this@AllResumeActivity, DetailActivity::class.java)
//                    intent.putExtra("ITEM_DATA", selectedItem)
//                    startActivity(intent)
//                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}