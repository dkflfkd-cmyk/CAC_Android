package com.example.cac.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val txtStrengthPoints: TextView = view.findViewById(R.id.txtStrengthPoints)
        val txtImprovementPoints: TextView = view.findViewById(R.id.txtImprovementPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_interview_feedback, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = feedbacks[position]
        holder.txtQuestionNumber.text = "질문 ${position + 1}"
        holder.txtQuestionScore.text = "${item.score.toInt()}점"
        holder.txtQuestionText.text = item.question_text.ifBlank { "질문 정보가 없습니다." }
        holder.itemAnswerText.text = item.answer_text.ifBlank { "답변 기록이 없습니다." }

        val parsed = parseFeedback(item.suggestion.orEmpty())
        val strength = item.strength.orEmpty()
            .ifBlank { parsed.first }
            .ifBlank { "답변의 장점이 아직 분석되지 않았습니다." }
        val improvement = buildList {
            if (!item.weakness.isNullOrBlank()) add("보완점: ${item.weakness}")
            if (parsed.second.isNotBlank()) add(parsed.second)
            else if (!item.suggestion.isNullOrBlank() && item.strength.isNullOrBlank()) add(item.suggestion)
        }.joinToString("\n").trim().ifBlank { "추가 개선 제안이 없습니다." }

        holder.txtStrengthPoints.text = "• $strength"
        holder.txtImprovementPoints.text = "• $improvement"
    }

    override fun getItemCount(): Int = feedbacks.size

    private fun parseFeedback(feedback: String): Pair<String, String> {
        val text = feedback.trim()
        if (text.isBlank()) return "" to ""

        val splitWords = listOf(" 그러나 ", " 하지만 ", " 다만 ", " 아쉬운 점은 ", " 다음엔 ")
        val splitIndex = splitWords.mapNotNull { word ->
            text.indexOf(word).takeIf { it >= 0 }
        }.minOrNull()

        if (splitIndex == null) return text to ""

        val strength = text.substring(0, splitIndex).trim()
        val improvement = text.substring(splitIndex).trim()
            .replace(Regex("^그러나\\s*"), "")
            .replace(Regex("^하지만\\s*"), "")
            .replace(Regex("^다만\\s*"), "")

        return strength to improvement
    }
}
