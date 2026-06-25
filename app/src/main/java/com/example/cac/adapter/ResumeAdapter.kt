package com.example.cac.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cac.R
import com.example.cac.network.ResumeItem

class ResumeAdapter(
    private val items: List<ResumeItem>
) : RecyclerView.Adapter<ResumeAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val txtFileName: TextView = v.findViewById(R.id.txtFileName)
        val txtDate: TextView = v.findViewById(R.id.txtSub)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_resume, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        android.util.Log.d("AdapterDebug", "Position $position 데이터: $item")
        holder.txtFileName.text = item.fileName
        val rawDate = item.date ?: ""
        holder.txtDate.text = rawDate.substringBefore("T")
    }
}