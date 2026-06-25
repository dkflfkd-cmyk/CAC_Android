package com.example.cac.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R

class SavedQuestionAdapter(private val questions: List<String>) :
    RecyclerView.Adapter<SavedQuestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNumber: TextView = view.findViewById(R.id.txtQuestionNumber)
        val txtText: TextView = view.findViewById(R.id.txtQuestionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.txtNumber.text = (position + 1).toString()
        holder.txtText.text = questions[position]
    }

    override fun getItemCount() = questions.size
}