package com.example.cac.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.data.SavedQuestionItem

class SavedQuestionAdapter(private val questions: List<SavedQuestionItem>) :
    RecyclerView.Adapter<SavedQuestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNumber: TextView = view.findViewById(R.id.txtQuestionNumber)
        val txtText: TextView = view.findViewById(R.id.txtQuestionText)
        val txtMeta: TextView = view.findViewById(R.id.txtQuestionMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = questions[position]
        holder.txtNumber.text = (position + 1).toString()
        holder.txtText.text = item.questionText

        val meta = listOfNotNull(
            item.targetJob?.takeIf { it.isNotBlank() },
            item.questionType?.takeIf { it.isNotBlank() },
            item.createdAt?.substringBefore("T")?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        holder.txtMeta.visibility = if (meta.isBlank()) View.GONE else View.VISIBLE
        holder.txtMeta.text = meta
    }

    override fun getItemCount() = questions.size
}
