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
    private val onStartClick: (Int) -> Unit
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
        holder.txtNumber.text = (position + 1).toString()
        holder.txtQuestion.text = item.question
        holder.btnStart.setOnClickListener {
            onStartClick(position)
        }
    }

    override fun getItemCount(): Int = items.size
}