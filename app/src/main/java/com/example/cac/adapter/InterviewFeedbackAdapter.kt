package com.example.cac

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar // [해결] Unresolved reference 'ProgressBar' 방지
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.network.InterviewQuestionFeedback

class InterviewFeedbackAdapter(private val feedbacks: List<InterviewQuestionFeedback>) :
    RecyclerView.Adapter<InterviewFeedbackAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtQuestionNumber: TextView = view.findViewById(R.id.txtQuestionNumber)
        val txtQuestionScore: TextView = view.findViewById(R.id.txtQuestionScore)
        val txtQuestionText: TextView = view.findViewById(R.id.txtQuestionText)
        val txtAnswerText: TextView = view.findViewById(R.id.txtAnswerText)

        // 발화 분석 (진행바 및 점수)
        val txtSpeedScore: TextView = view.findViewById(R.id.txtSpeedScore)
        val progressSpeed: ProgressBar = view.findViewById(R.id.progressSpeed)
        val txtPronunciationScore: TextView = view.findViewById(R.id.txtPronunciationScore)
        val progressPronunciation: ProgressBar = view.findViewById(R.id.progressPronunciation)

        // 필러워드 관련 (이 부분을 추가해야 오류가 사라집니다)
        val txtFillerCountBadge: TextView = view.findViewById(R.id.txtFillerCountBadge)
        val txtFillerDetail: TextView = view.findViewById(R.id.txtFillerDetail)
        val txtFillerStatus: TextView = view.findViewById(R.id.txtFillerStatus)

        // 쉼(Pause) 관련
        val txtPauseCountBadge: TextView = view.findViewById(R.id.txtPauseCountBadge)
        val txtPauseDetail: TextView = view.findViewById(R.id.txtPauseDetail)
        val txtPauseStatus: TextView = view.findViewById(R.id.txtPauseStatus)

        // 강점 및 개선점
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
        val analysis = item.analysis // 발화 분석 데이터 추출

        holder.txtQuestionNumber.text = "질문 ${position + 1}"
        holder.txtQuestionText.text = item.question

        // 1. 발음 명료도 (Clarity) 연결
        val clarityScore = ((analysis?.clarity ?: 0.0) * 100).toInt()
        holder.txtPronunciationScore.text = "$clarityScore/100"
        holder.progressPronunciation.progress = clarityScore

        // 2. 말하기 속도 (Speech Rate) 연결
        val speedValue = (analysis?.speech_rate ?: 0.0).toInt()
        holder.txtSpeedScore.text = "$speedValue/100"
        holder.progressSpeed.progress = speedValue

        // 3. 필러워드 (Filler Words) 연결
        val fillerCount = analysis?.filler_words_count ?: 0
        holder.txtFillerCountBadge.text = "${fillerCount}회"

        // 필러워드 개수에 따른 상태 메시지 (예시)
        holder.txtFillerStatus.text = if (fillerCount <= 3) "✔ 양호" else "⚠ 주의"

        // 4. 강점 및 개선점
        // 현재 item.feedback에 통째로 온다면 그대로
        holder.txtStrengthPoints.text = item.feedback
        holder.txtImprovementPoints.text = "분석된 침묵 시간은 ${analysis?.silence_duration ?: 0.0}초입니다."
    }

    override fun getItemCount(): Int = feedbacks.size
}