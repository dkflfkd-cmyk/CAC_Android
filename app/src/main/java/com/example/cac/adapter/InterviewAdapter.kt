package com.example.cac.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.data.InterviewItem

class InterviewAdapter(
    private val items: List<InterviewItem>
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
        val dateOnly = item.date.substringBefore("T")
        holder.txtDate.text = dateOnly
        holder.txtDetail.text = item.meta
        holder.txtScore.text = "${item.score}점"
    }
}