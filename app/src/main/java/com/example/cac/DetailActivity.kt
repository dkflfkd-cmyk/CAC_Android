package com.example.cac

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.adapter.InterviewFeedbackAdapter
import com.example.cac.data.InterviewItem
import com.example.cac.network.InterviewQuestionFeedback
import com.example.cac.network.InterviewResultResponse
import com.example.cac.network.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailActivity : AppCompatActivity() {
    private var sessionId: Int = -1
    private var questionFeedbackShown = false
    private var speechAnalysisShown = false
    private lateinit var feedbackRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val item = intent.getSerializableExtra("ITEM_DATA") as? InterviewItem
        sessionId = item?.session_id ?: intent.getIntExtra("session_id", -1)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        feedbackRecycler = findViewById(R.id.recyclerFeedback)
        feedbackRecycler.layoutManager = LinearLayoutManager(this)
        keepSpeechCardAboveQuestionList()

        bindHeader(item)
        setInsightLoading()
        showQuestionFeedbackMessage("질문별 상세 피드백을 불러오는 중입니다.")
        item?.feedback?.let { applySpeechJson(it) }
        if (sessionId != -1) {
            loadFeedback()
            loadSpeechAnalysis()
        } else {
            showFallback(item)
        }
    }

    private fun bindHeader(item: InterviewItem?) {
        findViewById<TextView>(R.id.tvDate).text = item?.date?.substringBefore("T") ?: "날짜 미정"
        findViewById<TextView>(R.id.tvMeta).text = item?.meta ?: "면접 기록"
        findViewById<TextView>(R.id.tvScore).text = (item?.score ?: 0).toString()
        findViewById<TextView>(R.id.tvFeedbackSummary).text = item?.feedback ?: "서버의 상세 분석을 불러오는 중입니다."
    }

    private fun showFallback(item: InterviewItem?) {
        findViewById<TextView>(R.id.tvFeedbackSummary).text = item?.feedback ?: "표시할 면접 상세 기록이 없습니다."
        setInsightEmpty()
        findViewById<TextView>(R.id.txtEmptyFeedback).visibility = View.VISIBLE
    }

    private fun loadFeedback() {
        loadSummaryFallback()
        loadQuestionResultFallback()
        loadSessionAnalysisQuestionFallback()
        loadStructuredFeedback()
    }

    private fun loadStructuredFeedback() {
        RetrofitClient.api.getInterviewResult(sessionId).enqueue(object : Callback<InterviewResultResponse> {
            override fun onResponse(call: Call<InterviewResultResponse>, response: Response<InterviewResultResponse>) {
                val feedback = response.body()?.feedback
                if (!response.isSuccessful || feedback == null) return

                findViewById<TextView>(R.id.tvScore).text = feedback.overall_score.toInt().toString()
                findViewById<TextView>(R.id.tvFeedbackSummary).text = buildSummary(feedback.strengths, feedback.improvements)
                showInsight(feedback.strengths.orEmpty(), feedback.improvements.orEmpty())

                val questionFeedbacks = runCatching { feedback.question_feedbacks }.getOrNull().orEmpty()
                if (questionFeedbacks.isNotEmpty()) {
                    showQuestionFeedbacks(questionFeedbacks)
                } else {
                    showQuestionFeedbackMessage("질문별 상세 피드백이 아직 준비되지 않았습니다.")
                }
            }

            override fun onFailure(call: Call<InterviewResultResponse>, t: Throwable) {
                showQuestionFeedbackMessage("질문별 상세 피드백을 불러오지 못했습니다.")
            }
        })
    }

    private fun loadSummaryFallback() {
        RetrofitClient.api.getSessionSummary(sessionId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) applySummaryJson(response.body()?.string().orEmpty())
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) = Unit
        })
    }

    private fun loadQuestionResultFallback() {
        RetrofitClient.api.getSessionResultRaw(sessionId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!response.isSuccessful) return
                val feedbacks = parseQuestionResult(response.body()?.string().orEmpty())
                if (feedbacks.isNotEmpty()) {
                    showQuestionFeedbacks(feedbacks)
                } else {
                    showQuestionFeedbackMessage("질문별 상세 피드백이 아직 준비되지 않았습니다.")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                showQuestionFeedbackMessage("질문별 상세 피드백을 불러오지 못했습니다.")
            }
        })
    }

    private fun loadSessionAnalysisQuestionFallback() {
        RetrofitClient.api.getSessionAnalysis(sessionId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!response.isSuccessful) return
                val feedbacks = parseQuestionResult(response.body()?.string().orEmpty())
                if (feedbacks.isNotEmpty()) showQuestionFeedbacks(feedbacks)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) = Unit
        })
    }

    private fun showQuestionFeedbacks(items: List<InterviewQuestionFeedback>) {
        questionFeedbackShown = true
        findViewById<TextView>(R.id.txtEmptyFeedback).visibility = View.GONE
        feedbackRecycler.adapter = InterviewFeedbackAdapter(items)
    }

    private fun showQuestionFeedbackMessage(message: String) {
        if (questionFeedbackShown) return
        findViewById<TextView>(R.id.txtEmptyFeedback).apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    private fun applySummaryJson(raw: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            val root = JSONObject(raw)
            numberOf(root, "total_score", "overall_score", "score")?.let {
                findViewById<TextView>(R.id.tvScore).text = it.toInt().toString()
            }

            val summary = stringOf(root, "summary")
            val strength = stringOf(root, "strength_summary")
            val weakness = stringOf(root, "weakness_summary")
            val recommendation = stringOf(root, "recommendation")
            val voice = stringOf(root, "voice_feedback")
            val lines = mutableListOf<String>()
            summary?.let { lines += "총평: $it" }
            recommendation?.let { lines += "추천: $it" }
            voice?.let { lines += "발화: $it" }
            if (lines.isNotEmpty()) {
                findViewById<TextView>(R.id.tvFeedbackSummary).text = lines.joinToString("\n\n")
            }
            showInsight(splitInsightText(strength), splitInsightText(weakness))

            root.optJSONObject("voice_avg")?.let { applyVoiceAverage(it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun parseQuestionResult(raw: String): List<InterviewQuestionFeedback> {
        if (raw.isBlank()) return emptyList()
        return try {
            parseFeedbackContainer(JSONTokener(raw).nextValue())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFeedbackContainer(value: Any?): List<InterviewQuestionFeedback> {
        return when (value) {
            is JSONArray -> parseFeedbackArray(value)
            is JSONObject -> {
                val arrayKeys = listOf("question_feedbacks", "question_feedback", "questions", "feedbacks", "items", "data", "results")
                for (key in arrayKeys) {
                    when (val child = value.opt(key)) {
                        is JSONArray -> return parseFeedbackArray(child)
                        is JSONObject -> parseFeedbackContainer(child).takeIf { it.isNotEmpty() }?.let { return it }
                    }
                }
                value.optJSONObject("feedback")?.let { feedback ->
                    parseFeedbackContainer(feedback).takeIf { it.isNotEmpty() }?.let { return it }
                }
                parseFeedbackItem(value, 0)?.let { listOf(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun parseFeedbackArray(array: JSONArray): List<InterviewQuestionFeedback> {
        val result = mutableListOf<InterviewQuestionFeedback>()
        for (index in 0 until array.length()) {
            when (val value = array.opt(index)) {
                is JSONObject -> parseFeedbackItem(value, index)?.let { result += it }
                is JSONArray -> result += parseFeedbackArray(value)
            }
        }
        return result
    }

    private fun parseFeedbackItem(item: JSONObject, index: Int): InterviewQuestionFeedback? {
        val questionObj = item.optJSONObject("question")
        val answer = item.optJSONObject("answer") ?: item.optJSONObject("answer_detail")
        val feedback = item.optJSONObject("feedback") ?: item.optJSONObject("analysis") ?: item
        val questionText = stringOf(item, "question_text", "question", "content", "title")
            ?: stringOf(questionObj, "question_text", "question", "content", "title")
        val answerText = stringOf(item, "answer_text", "answer_summary", "stt_text", "user_answer")
            ?: stringOf(answer, "stt_text", "answer_text", "answer_summary", "user_answer")
        val strength = stringOf(item, "strength")
            ?: stringOf(feedback, "strength", "good_point", "positive_feedback")
        val weakness = stringOf(item, "weakness")
            ?: stringOf(feedback, "weakness", "improvement", "improvement_point")
        val suggestion = stringOf(item, "suggestion", "feedback_text")
            ?: stringOf(feedback, "suggestion", "feedback", "advice", "recommendation")

        if (questionText == null && answerText == null && strength == null && weakness == null && suggestion == null) return null

        return InterviewQuestionFeedback(
            question_id = item.optInt("question_id", item.optInt("id", index + 1)),
            question_text = questionText ?: "질문 정보가 없습니다.",
            answer_text = answerText ?: "아직 답변 기록이 없습니다.",
            score = numberOf(item, "score") ?: numberOf(feedback, "score", "total_score") ?: 0.0,
            strength = strength ?: "아직 강점 분석이 없습니다.",
            weakness = weakness,
            suggestion = suggestion ?: "답변 제출 후 상세 제안이 표시됩니다."
        )
    }

    private fun setInsightLoading() {
        findViewById<TextView>(R.id.tvStrengthOne).text = "분석을 불러오는 중입니다."
        findViewById<TextView>(R.id.tvStrengthTwo).text = "서버에서 강점 데이터를 확인하고 있어요."
        findViewById<TextView>(R.id.tvImprovementOne).text = "분석을 불러오는 중입니다."
        findViewById<TextView>(R.id.tvImprovementTwo).text = "서버에서 개선 포인트를 확인하고 있어요."
        setSpeechLoading()
    }

    private fun setInsightEmpty() {
        findViewById<TextView>(R.id.tvStrengthOne).text = "아직 강점 분석이 도착하지 않았습니다."
        findViewById<TextView>(R.id.tvStrengthTwo).text = "면접 결과가 생성되면 자동으로 표시됩니다."
        findViewById<TextView>(R.id.tvImprovementOne).text = "아직 개선 분석이 도착하지 않았습니다."
        findViewById<TextView>(R.id.tvImprovementTwo).text = "면접 결과가 생성되면 자동으로 표시됩니다."
    }

    private fun showInsight(strengths: List<String>, improvements: List<String>) {
        if (strengths.isEmpty() && improvements.isEmpty()) return
        val strengthItems = strengths.filter { it.isNotBlank() }.take(2)
        val improvementItems = improvements.filter { it.isNotBlank() }.take(2)
        findViewById<TextView>(R.id.tvStrengthOne).text = formatBullet(strengthItems.getOrNull(0), "아직 강점 분석이 없습니다.")
        findViewById<TextView>(R.id.tvStrengthTwo).text = formatBullet(strengthItems.getOrNull(1), "추가 강점 데이터가 도착하면 표시됩니다.")
        findViewById<TextView>(R.id.tvImprovementOne).text = formatBullet(improvementItems.getOrNull(0), "아직 개선 분석이 없습니다.")
        findViewById<TextView>(R.id.tvImprovementTwo).text = formatBullet(improvementItems.getOrNull(1), "추가 개선 데이터가 도착하면 표시됩니다.")
    }

    private fun formatBullet(value: String?, fallback: String): String {
        return "• " + (value?.takeIf { it.isNotBlank() } ?: fallback)
    }

    private fun splitInsightText(value: String?): List<String> {
        return value.orEmpty()
            .split(Regex("(?<=[.!?。])\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { value?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList() }
            .take(2)
    }

    private fun loadSpeechAnalysis() {
        RetrofitClient.api.getSpeechAnalysisRaw(sessionId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                val raw = response.body()?.string().orEmpty()
                if (response.isSuccessful && applySpeechJson(raw)) return
                loadSpeechAnalysisFromSessionAnalysis()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                loadSpeechAnalysisFromSessionAnalysis()
            }
        })
    }

    private fun loadSpeechAnalysisFromSessionAnalysis() {
        RetrofitClient.api.getSessionAnalysis(sessionId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (speechAnalysisShown) return
                if (response.isSuccessful && applySpeechJson(response.body()?.string().orEmpty())) return
                setSpeechEmpty()
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                if (speechAnalysisShown) return
                setSpeechEmpty()
            }
        })
    }

    private fun keepSpeechCardAboveQuestionList() {
        val speechCard = findViewById<View>(R.id.cardSpeechAnalysis)
        val questionTitle = findViewById<View>(R.id.tvQuestionFeedbackTitle)
        val parent = speechCard.parent as? ViewGroup ?: return
        val targetIndex = parent.indexOfChild(questionTitle)
        if (targetIndex < 0 || parent.indexOfChild(speechCard) < targetIndex) return
        parent.removeView(speechCard)
        parent.addView(speechCard, targetIndex)
    }

    private fun applySpeechJson(raw: String): Boolean {
        if (raw.isBlank()) return false
        return try {
            val root = JSONObject(raw)
            val speech = findSpeechObject(root) ?: root
            val speed = numberOf(speech, "speaking_speed_score", "speaking_speed_wpm", "speaking_rate_avg", "avg_tempo", "tempo")
            val intonation = numberOf(speech, "intonation_score", "pronunciation_avg", "pronunciation_clarity", "clarity")
            val confidence = numberOf(speech, "confidence_score", "confidence_avg", "confidence")
            val volume = numberOf(speech, "volume_score", "volume_avg", "voice_volume", "volume")
            val filler = numberOf(speech, "filler_word_avg", "filler_word_total", "filler_word_count", "filler_words_count")
            val pause = numberOf(speech, "pause_count_avg", "pause_count", "silence_duration")

            if (listOf(speed, intonation, confidence, volume, filler, pause).all { it == null }) return false

            setMetric(R.id.txtSpeedScore, R.id.progressSpeed, normalizeScore(speed))
            setMetric(R.id.txtPronunciationScore, R.id.progressPronunciation, normalizeScore(intonation))
            setMetric(R.id.txtConfidenceScore, R.id.progressConfidence, normalizeScore(confidence))
            setMetric(R.id.txtVolumeScore, R.id.progressVolume, normalizeScore(volume))
            findViewById<TextView>(R.id.txtFillerCount).text = "${filler?.toInt() ?: 0}회"
            findViewById<TextView>(R.id.txtPauseCount).text = "${pause?.toInt() ?: 0}회"
            speechAnalysisShown = true
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun applyVoiceAverage(voiceAvg: JSONObject) {
        val speed = numberOf(voiceAvg, "speaking_rate_avg", "speaking_speed_wpm")
        val intonation = numberOf(voiceAvg, "pronunciation_avg", "intonation_score")
        val confidence = numberOf(voiceAvg, "confidence_avg")
        val volume = numberOf(voiceAvg, "volume_avg", "volume_score")
        val pauseCount = numberOf(voiceAvg, "pause_count_avg")

        if (listOf(speed, intonation, confidence, volume, pauseCount).all { it == null }) return
        setMetricIfPresent(R.id.txtSpeedScore, R.id.progressSpeed, speed)
        setMetricIfPresent(R.id.txtPronunciationScore, R.id.progressPronunciation, intonation)
        setMetricIfPresent(R.id.txtConfidenceScore, R.id.progressConfidence, confidence)
        setMetricIfPresent(R.id.txtVolumeScore, R.id.progressVolume, volume)
        pauseCount?.let { findViewById<TextView>(R.id.txtPauseCount).text = "${it.toInt()}회" }
        speechAnalysisShown = true
    }

    private fun findSpeechObject(root: JSONObject): JSONObject? {
        val keys = listOf("speech_analysis", "speech", "analysis", "data", "voice_avg")
        for (key in keys) {
            val value = root.opt(key)
            if (value is JSONObject) {
                if (hasSpeechMetric(value)) return value
                findSpeechObject(value)?.let { return it }
            }
        }
        return null
    }

    private fun hasSpeechMetric(obj: JSONObject): Boolean {
        return listOf(
            "speaking_speed_score",
            "speaking_speed_wpm",
            "speaking_rate_avg",
            "avg_tempo",
            "tempo",
            "intonation_score",
            "pronunciation_avg",
            "confidence_score",
            "confidence_avg",
            "volume_score",
            "volume_avg",
            "filler_word_avg",
            "filler_word_total",
            "pause_count_avg"
        ).any { obj.has(it) }
    }

    private fun numberOf(obj: JSONObject?, vararg keys: String): Double? {
        if (obj == null) return null
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            when (value) {
                is Number -> return value.toDouble()
                is String -> value.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun stringOf(obj: JSONObject?, vararg keys: String): String? {
        if (obj == null) return null
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.optString(key).trim()
            if (value.isNotBlank() && value != "null") return value
        }
        return null
    }

    private fun normalizeScore(value: Double?): Int {
        if (value == null) return 0
        val normalized = if (value > 100 && value <= 240) 100.0 else value
        return normalized.toInt().coerceIn(0, 100)
    }

    private fun setMetric(textId: Int, progressId: Int, value: Int) {
        findViewById<TextView>(textId).text = "$value/100"
        findViewById<ProgressBar>(progressId).progress = value.coerceIn(0, 100)
    }

    private fun setMetricIfPresent(textId: Int, progressId: Int, value: Double?) {
        if (value == null) return
        setMetric(textId, progressId, normalizeScore(value))
    }

    private fun setSpeechLoading() {
        speechAnalysisShown = false
        findViewById<TextView>(R.id.txtSpeedScore).text = "불러오는 중"
        findViewById<TextView>(R.id.txtPronunciationScore).text = "불러오는 중"
        findViewById<TextView>(R.id.txtConfidenceScore).text = "불러오는 중"
        findViewById<TextView>(R.id.txtVolumeScore).text = "불러오는 중"
        findViewById<TextView>(R.id.txtFillerCount).text = "불러오는 중"
        findViewById<TextView>(R.id.txtPauseCount).text = "불러오는 중"
    }

    private fun setSpeechEmpty() {
        if (speechAnalysisShown) return
        findViewById<TextView>(R.id.txtSpeedScore).text = "데이터 없음"
        findViewById<TextView>(R.id.txtPronunciationScore).text = "데이터 없음"
        findViewById<TextView>(R.id.txtConfidenceScore).text = "데이터 없음"
        findViewById<TextView>(R.id.txtVolumeScore).text = "데이터 없음"
        findViewById<TextView>(R.id.txtFillerCount).text = "데이터 없음"
        findViewById<TextView>(R.id.txtPauseCount).text = "데이터 없음"
        findViewById<ProgressBar>(R.id.progressSpeed).progress = 0
        findViewById<ProgressBar>(R.id.progressPronunciation).progress = 0
        findViewById<ProgressBar>(R.id.progressConfidence).progress = 0
        findViewById<ProgressBar>(R.id.progressVolume).progress = 0
    }

    private fun buildSummary(strengths: List<String>?, improvements: List<String>?): String {
        val lines = mutableListOf<String>()
        strengths.orEmpty().take(2).forEach { lines += "강점: $it" }
        improvements.orEmpty().take(2).forEach { lines += "개선: $it" }
        return lines.joinToString("\n").ifBlank { "질문별 상세 피드백을 확인해보세요." }
    }
}
