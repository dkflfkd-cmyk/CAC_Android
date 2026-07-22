package com.example.cac

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cac.data.InterviewItem
import com.example.cac.data.InterviewRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var recentItems: List<InterviewItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupSideMenu()
        setupTitle()
        setupClickListeners()
        BottomNavHelper.apply(this, BottomNavHelper.Tab.HOME)
        loadRecentInterviews()
    }

    private fun setupClickListeners() {
        val aptitudeUrl = "http://52.79.211.82:8000/static/aptitude-test.html"
        findViewById<View>(R.id.btnDoAptitudeTest).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aptitudeUrl)))
        }

        findViewById<TextView>(R.id.btnInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        findViewById<TextView>(R.id.btnNoticev2).setOnClickListener {
            startActivity(Intent(this, MYActivity::class.java))
        }

        findViewById<View>(R.id.cardInterview).setOnClickListener {
            startActivity(Intent(this, InterviewActivity::class.java))
        }
        findViewById<View>(R.id.cardResume).setOnClickListener {
            startActivity(Intent(this, cardResumeActivity::class.java))
        }
        findViewById<View>(R.id.cardlist).setOnClickListener {
            startActivity(Intent(this, QuestionSetupActivity::class.java))
        }

        findViewById<View>(R.id.cardRecent).setOnClickListener {
            startActivity(Intent(this, AllInterviewActivity::class.java))
        }
        findViewById<TextView>(R.id.txtRecent).setOnClickListener {
            startActivity(Intent(this, AllInterviewActivity::class.java))
        }
    }

    private fun loadRecentInterviews() {
        lifecycleScope.launch {
            try {
                recentItems = InterviewRepository.loadInterviewItems(this@MainActivity).take(2)
                bindRecentPreview()
            } catch (e: Exception) {
                hideRecentPreview()
            }
        }
    }

    private fun bindRecentPreview() {
        val hasRecent = recentItems.isNotEmpty()
        findViewById<View>(R.id.txtRecentEmpty).visibility = if (hasRecent) View.GONE else View.VISIBLE
        findViewById<View>(R.id.recentSpacer).visibility = if (recentItems.size >= 2) View.VISIBLE else View.GONE
        bindRecentRow(0, R.id.recentRow1, R.id.date1, R.id.desc1, R.id.score1)
        bindRecentRow(1, R.id.recentRow2, R.id.date2, R.id.desc2, R.id.score2)
    }

    private fun bindRecentRow(index: Int, rowId: Int, dateId: Int, descId: Int, scoreId: Int) {
        val row = findViewById<View>(rowId)
        val item = recentItems.getOrNull(index)
        if (item == null) {
            row.visibility = View.GONE
            return
        }

        row.visibility = View.VISIBLE
        findViewById<TextView>(dateId).text = item.date.substringBefore("T").ifBlank { "날짜 미정" }
        findViewById<TextView>(descId).text = item.meta
        findViewById<TextView>(scoreId).text = "${item.score}점"
        row.setOnClickListener {
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("ITEM_DATA", item)
            startActivity(intent)
        }
    }

    private fun hideRecentPreview() {
        findViewById<View>(R.id.txtRecentEmpty).visibility = View.VISIBLE
        findViewById<View>(R.id.recentRow1).visibility = View.GONE
        findViewById<View>(R.id.recentSpacer).visibility = View.GONE
        findViewById<View>(R.id.recentRow2).visibility = View.GONE
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
