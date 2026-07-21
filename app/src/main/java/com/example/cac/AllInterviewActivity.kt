package com.example.cac

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.data.InterviewRepository
import com.example.cac.ui.adapter.InterviewAdapter
import kotlinx.coroutines.launch

class AllInterviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_interview)

        val rvList = findViewById<RecyclerView>(R.id.rvInterviewList)
        rvList.layoutManager = LinearLayoutManager(this)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        loadAllData(rvList)
    }

    private fun loadAllData(rv: RecyclerView) {
        lifecycleScope.launch {
            try {
                val fullList = InterviewRepository.loadInterviewItems(this@AllInterviewActivity)

                rv.adapter = InterviewAdapter(fullList) { selectedItem ->
                    val intent = Intent(this@AllInterviewActivity, DetailActivity::class.java)
                    intent.putExtra("ITEM_DATA", selectedItem)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AllInterviewActivity, "면접 기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
