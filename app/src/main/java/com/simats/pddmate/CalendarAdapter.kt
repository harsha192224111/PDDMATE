package com.simats.pddmate

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(private val items: List<CalendarCell>) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                40.dp(parent.context),
                40.dp(parent.context)
            )
            gravity = Gravity.CENTER
            textSize = 14f
            isFocusable = true
            isClickable = true
        }
        return CalendarViewHolder(textView)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val item = items[position]
        val tv = holder.textView
        tv.text = item.text
        tv.setBackgroundResource(
            if (item.isHighlighted) R.drawable.bg_current_date else android.R.color.transparent
        )
        tv.setTextColor(if (item.isDay) 0xFF222222.toInt() else 0x00000000)
    }

    class CalendarViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
