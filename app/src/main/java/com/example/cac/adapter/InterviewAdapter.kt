package com.example.cac.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.data.InterviewItem

class InterviewAdapter(
    private val items: List<InterviewItem>,
    private val onClick: (InterviewItem) -> Unit
) : RecyclerView.Adapter<InterviewAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtDate: TextView = v.findViewById(R.id.txtDate)
        val txtDetail: TextView = v.findViewById(R.id.txtMeta)
        val txtScore: TextView = v.findViewById(R.id.badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_interview, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.txtDate.text = item.date.substringBefore("T").ifBlank { "날짜 미정" }
        holder.txtDetail.text = item.meta
        holder.txtScore.text = "${item.score}점"
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
