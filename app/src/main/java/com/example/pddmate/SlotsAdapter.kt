package com.example.pddmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class SlotsAdapter(
    private val slots: List<ProjectSlot>,
    private val onViewClick: (ProjectSlot) -> Unit
) : RecyclerView.Adapter<SlotsAdapter.SlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slot_card, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        holder.bind(slot)
    }

    override fun getItemCount(): Int = slots.size

    inner class SlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<CardView>(R.id.slotCard)
        private val title = view.findViewById<TextView>(R.id.cardTitle)
        private val category = view.findViewById<TextView>(R.id.cardCategory)
        private val mentor = view.findViewById<TextView>(R.id.cardMentor)
        private val slotsLeft = view.findViewById<TextView>(R.id.cardSlotsLeft)
        private val viewButton = view.findViewById<Button>(R.id.viewButton)

        fun bind(slot: ProjectSlot) {
            title.text = "${slot.courseCode} - ${slot.title}"
            category.text = slot.type
            mentor.text = slot.developerName
            slotsLeft.text = "${slot.capacity} slots left"

            // Set the click listener for the view button
            viewButton.setOnClickListener { onViewClick(slot) }
        }
    }
}