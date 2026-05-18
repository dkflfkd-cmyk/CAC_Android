package com.example.cac.adapter

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.network.InterviewQuestionFeedback

class InterviewFeedbackAdapter(
    private val feedbacks: List<InterviewQuestionFeedback>
) : RecyclerView.Adapter<InterviewFeedbackAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtQuestionNumber: TextView = view.findViewById(R.id.txtQuestionNumber)
        val txtQuestionScore: TextView = view.findViewById(R.id.txtQuestionScore)
        val txtQuestionText: TextView = view.findViewById(R.id.txtQuestionText)
        val itemAnswerText: TextView = view.findViewById(R.id.txtAnswerText)

        // 강점과 개선점을 보여줄 텍스트뷰
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

        // 1. 기본 정보(질문 번호, 점수, 질문, 내 답변) 바인딩
        holder.txtQuestionNumber.text = "질문 ${position + 1}"
        holder.txtQuestionScore.text = "${item.score.toInt()}점"
        holder.txtQuestionText.text = item.question_text ?: "질문 정보가 없습니다."
        holder.itemAnswerText.text = item.answer_text ?: "답변 데이터가 존재하지 않습니다."

        // 💡 2. 서버가 합쳐서 보낸 피드백 문장을 '강점'과 '개선점'으로 분리합니다.
        // 현재 ApiService에서 서버의 "feedback" 필드가 "suggestion" 변수로 매핑되어 들어옵니다.
        val fullFeedback = item.suggestion ?: ""
        var strengthText = item.strength ?: ""
        var improvementText = ""

        // 만약 강점이 비어있고, 피드백 문장이 있다면 직접 쪼개줍니다.
        if (strengthText.isBlank() && fullFeedback.isNotBlank()) {
            // 서버 응답 패턴: "~~ 좋았습니다. 그러나 ~~ 아쉽습니다."
            if (fullFeedback.contains("좋았습니다.")) {
                val splitIndex = fullFeedback.indexOf("좋았습니다.") + "좋았습니다.".length
                strengthText = fullFeedback.substring(0, splitIndex).trim()

                // 뒤에 이어지는 "그러나 " 등의 불필요한 접속사를 제거하여 깔끔하게 다듬기
                improvementText = fullFeedback.substring(splitIndex)
                    .replace("그러나", "")
                    .trim()
            } else {
                // "좋았습니다" 패턴이 없으면 전체를 개선점에 넣습니다.
                improvementText = fullFeedback
            }
        } else {
            // 구버전 데이터나 약점/제안이 따로 분리되어 있을 경우의 처리
            improvementText = buildString {
                if (!item.weakness.isNullOrBlank()) append("• 아쉬운 점: ${item.weakness}\n")
                if (!item.suggestion.isNullOrBlank()) append("• 피드백 제안: ${item.suggestion}")
            }.trim()
        }

        // 3. 강점(잘한 점) 화면 적용
        if (strengthText.isNotBlank()) {
            holder.txtStrengthPoints.text = "• $strengthText"
        } else {
            holder.txtStrengthPoints.text = "• 특별한 강점이 분석되지 않았습니다."
        }

        // 4. 개선점(아쉬운 점/제안) 화면 적용
        if (improvementText.isNotBlank()) {
            holder.txtImprovementPoints.text = "• $improvementText"
        } else {
            holder.txtImprovementPoints.text = "• 개선할 점이 없습니다. 훌륭한 답변입니다!"
        }
    }

    override fun getItemCount(): Int = feedbacks.size
}