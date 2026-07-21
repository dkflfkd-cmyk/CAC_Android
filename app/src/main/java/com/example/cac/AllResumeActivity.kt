package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.ResumeRepository
import com.example.cac.network.ResumeItem
import com.example.cac.ui.adapter.ResumeAdapter
import kotlinx.coroutines.launch

class AllResumeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_resume)

        val rvList = findViewById<RecyclerView>(R.id.rvResumeList)
        rvList.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnNoticeh).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<TextView>(R.id.btnRoadmap).setOnClickListener {
            startActivity(Intent(this, RoadmapActivity::class.java))
        }

        loadAllData(rvList)
    }

    private fun loadAllData(rv: RecyclerView) {
        lifecycleScope.launch {
            try {
                rv.adapter = ResumeAdapter(ResumeRepository.loadResumeItems(this@AllResumeActivity)) { item -> openResumeDetail(item) }
            } catch (e: Exception) {
                Toast.makeText(this@AllResumeActivity, "이력서 기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openResumeDetail(item: ResumeItem) {
        val resumeId = item.resumeId
        if (resumeId == null) {
            Toast.makeText(this, "이 이력서는 상세 분석 id가 아직 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, cardResumeActivity::class.java).apply {
            putExtra("resume_id", resumeId)
            putExtra("file_name", item.fileName)
            putExtra("created_at", item.date)
            putExtra("target_job", item.targetJob)
            putExtra("pdf_url", item.pdfUrl)
        })
    }
}
