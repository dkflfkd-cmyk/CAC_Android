package com.example.cac.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.data.QuestionItem

class QuestionListAdapter(
    private val items: List<QuestionItem>,
    private val onStartClick: (QuestionItem) -> Unit
) : RecyclerView.Adapter<QuestionListAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNumber: TextView = itemView.findViewById(R.id.txtQuestionNumber)
        val txtQuestion: TextView = itemView.findViewById(R.id.txtQuestionText)
        val btnStart: ImageButton = itemView.findViewById(R.id.btnStartQuestion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val item = items[position]
        //의 뷰 구조 유지
        holder.txtNumber.text = item.number.toString()
        holder.txtQuestion.text = item.question

        // 버튼 클릭 시 아이템 객체 전달
        holder.btnStart.setOnClickListener {
            onStartClick(item)
        }
        // 항목 전체 클릭 시에도 이동하도록 설정
        holder.itemView.setOnClickListener {
            onStartClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}