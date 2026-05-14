package com.example.cac

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.network.InterviewQuestionFeedback

// ⭐️ 생성자에 strengths: List<String>? 매개변수 추가!
class InterviewFeedbackAdapter(
    private val feedbacks: List<InterviewQuestionFeedback>,
    private val strengths: List<String>?
) : RecyclerView.Adapter<InterviewFeedbackAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtQuestionNumber: TextView = view.findViewById(R.id.txtQuestionNumber)
        val txtQuestionScore: TextView = view.findViewById(R.id.txtQuestionScore)
        val txtQuestionText: TextView = view.findViewById(R.id.txtQuestionText)
        val txtAnswerText: TextView = view.findViewById(R.id.txtAnswerText)

        val txtSpeedScore: TextView = view.findViewById(R.id.txtSpeedScore)
        val progressSpeed: ProgressBar = view.findViewById(R.id.progressSpeed)
        val txtPronunciationScore: TextView = view.findViewById(R.id.txtPronunciationScore)
        val progressPronunciation: ProgressBar = view.findViewById(R.id.progressPronunciation)

        val txtConfidenceScore: TextView = view.findViewById(R.id.txtConfidenceScore)
        val progressConfidence: ProgressBar = view.findViewById(R.id.progressConfidence)
        val txtVolumeScore: TextView = view.findViewById(R.id.txtVolumeScore)
        val progressVolume: ProgressBar = view.findViewById(R.id.progressVolume)

        val txtFillerCountBadge: TextView = view.findViewById(R.id.txtFillerCountBadge)
        val txtFillerDetail: TextView = view.findViewById(R.id.txtFillerDetail)
        val txtFillerStatus: TextView = view.findViewById(R.id.txtFillerStatus)

        val txtPauseCountBadge: TextView = view.findViewById(R.id.txtPauseCountBadge)
        val txtPauseDetail: TextView = view.findViewById(R.id.txtPauseDetail)
        val txtPauseStatus: TextView = view.findViewById(R.id.txtPauseStatus)

        val txtStrengthPoints: TextView = view.findViewById(R.id.txtStrengthPoints)
        val txtImprovementPoints: TextView = view.findViewById(R.id.txtImprovementPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interview_feedback, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = feedbacks[position]
        val analysis = item.analysis

        holder.txtQuestionNumber.text = "질문 ${position + 1}"
        holder.txtQuestionScore.text = "${item.score}점"
        holder.txtQuestionText.text = item.question
        holder.txtAnswerText.text = item.answer_summary ?: "답변 데이터가 존재하지 않습니다."

        val clarityScore = if ((analysis?.clarity ?: 0.0) <= 1.0) {
            ((analysis?.clarity ?: 0.0) * 100).toInt()
        } else {
            (analysis?.clarity ?: 0.0).toInt()
        }
        holder.txtPronunciationScore.text = "$clarityScore/100"
        holder.progressPronunciation.progress = clarityScore.coerceIn(0, 100)

        val speedValue = (analysis?.speech_rate ?: 0.0).toInt()
        holder.txtSpeedScore.text = "$speedValue/100"
        holder.progressSpeed.progress = speedValue.coerceIn(0, 100)

        val confidenceValue = (analysis?.confidence ?: 0.0).toInt()
        holder.txtConfidenceScore.text = "$confidenceValue/100"
        holder.progressConfidence.progress = confidenceValue.coerceIn(0, 100)

        val volumeValue = (analysis?.volume ?: 0.0).toInt()
        holder.txtVolumeScore.text = "$volumeValue/100"
        holder.progressVolume.progress = volumeValue.coerceIn(0, 100)

        val fillerCount = analysis?.filler_words_count ?: 0
        holder.txtFillerCountBadge.text = "${fillerCount}회"
        holder.txtFillerDetail.text = "\"음\" / \"어\" / \"그\" 등 사용"
        holder.txtFillerStatus.text = if (fillerCount <= 3) "✔ 양호" else "⚠ 주의"

        val silenceDuration = analysis?.silence_duration ?: 0.0
        holder.txtPauseCountBadge.text = "침묵"
        holder.txtPauseDetail.text = "총 정적 시간: ${silenceDuration}초"
        holder.txtPauseStatus.text = if (silenceDuration <= 3.0) "✔ 적절" else "⚠ 주의"


        if (!strengths.isNullOrEmpty()) {
            val strengthsText = strengths.joinToString("\n") { "• $it" }
            holder.txtStrengthPoints.text = strengthsText
        } else {
            holder.txtStrengthPoints.text = "• 분석된 종합 강점이 없습니다."
        }

        holder.txtImprovementPoints.text = if (!item.feedback.isNullOrBlank()) {
            "• ${item.feedback}"
        } else {
            "• 분석된 피드백 내용이 없습니다."
        }
    }

    override fun getItemCount(): Int = feedbacks.size
}