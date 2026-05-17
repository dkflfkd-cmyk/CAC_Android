package com.example.cac.adapter

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.network.InterviewQuestionFeedback
import com.example.cac.network.SpeechDetailResponse

class InterviewFeedbackAdapter(
    private val feedbacks: List<InterviewQuestionFeedback>,
    private val strengths: List<String>?,
    private val speechAnalysis: com.example.cac.network.SpeechAnalysisResponse?
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
        val fullFeedback = item.feedback ?: ""

        holder.txtQuestionNumber.text = "질문 ${position + 1}"
        holder.txtQuestionScore.text = "${(item.score ?: 0).toInt()}점"
        holder.txtQuestionText.text = item.question ?: "질문 정보가 없습니다."
        holder.txtAnswerText.text = item.answer_summary ?: "답변 데이터가 존재하지 않습니다."


        val audioScores = item.audio_scores
        val individualFillerCount = item.filler_word_count ?: 0

        // 1. 발음 명료도 (질문별 개별 점수 반영)
        val clarityScore = audioScores?.clarity ?: 0.0
        val clarityStr = if (clarityScore >= 80) "명료함" else if (clarityScore >= 50) "보통" else "흐림"
        holder.txtPronunciationScore.text = clarityStr
        holder.progressPronunciation.progress = clarityScore.toInt().coerceIn(0, 100)

        // 2. 말하기 속도
        val speedValue = audioScores?.speech_rate ?: 0.0
        val speedEval = if (speedValue in 80.0..130.0) "적절" else if (speedValue < 80.0) "느림" else "빠름"
        holder.txtSpeedScore.text = "${speedValue.toInt()} WPM ($speedEval)"
        holder.progressSpeed.progress = speedValue.toInt().coerceIn(0, 100)

        // 3. 자신감
        val confidenceValue = audioScores?.confidence ?: 0.0
        val confidenceStr = if (confidenceValue >= 75) "높음" else if (confidenceValue >= 50) "보통" else "낮음"
        holder.txtConfidenceScore.text = confidenceStr
        holder.progressConfidence.progress = confidenceValue.toInt().coerceIn(0, 100)

        // 4. 목소리 크기
        val volumeValue = audioScores?.volume ?: 0.0
        val volumeStr = if (volumeValue >= 70) "적절" else if (volumeValue >= 40) "보통" else "작음"
        holder.txtVolumeScore.text = volumeStr
        holder.progressVolume.progress = volumeValue.toInt().coerceIn(0, 100)

        // 5. 불필요한 추임새
        holder.txtFillerCountBadge.text = "${individualFillerCount}회"
        holder.txtFillerDetail.text = if (individualFillerCount == 0) "불필요한 습관어 사용 없음" else "\"음\" / \"어\" / \"그\" 등 사용"
        holder.txtFillerStatus.text = if (individualFillerCount <= 2) "✔ 양호" else "⚠ 주의"

        // 6. 침묵 구간
        val silenceSec = audioScores?.silence_duration ?: 0.0
        holder.txtPauseCountBadge.text = "침묵"
        holder.txtPauseDetail.text = "총 정적 시간: ${silenceSec.toInt()}초"
        holder.txtPauseStatus.text = if (silenceSec <= 3.0) "✔ 적절" else "⚠ 주의"

        // 7. 강점 및 개선점 피드백 분기 처리
        val delimiter = if (fullFeedback.contains("그러나")) "그러나" else "하지만"

        if (fullFeedback.contains(delimiter)) {
            val parts = fullFeedback.split(delimiter)
            holder.txtStrengthPoints.text = "• ${parts[0].trim()}"
            holder.txtStrengthPoints.visibility = View.VISIBLE

            holder.txtImprovementPoints.text = "• $delimiter ${parts[1].trim()}"
            holder.txtImprovementPoints.visibility = View.VISIBLE
        } else {
            holder.txtStrengthPoints.visibility = View.GONE
            if (fullFeedback.isNotBlank()) {
                holder.txtImprovementPoints.text = "• $fullFeedback"
                holder.txtImprovementPoints.visibility = View.VISIBLE
            } else {
                holder.txtImprovementPoints.text = "• 분석된 피드백 내용이 없습니다."
                holder.txtImprovementPoints.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = feedbacks.size
}