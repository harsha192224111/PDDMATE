package com.example.pddmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VerifySlotsAdapter(
    private val projects: List<VerifyProject>,
    private val onVerifyClick: (VerifyProject) -> Unit
) : RecyclerView.Adapter<VerifySlotsAdapter.VerifyProjectViewHolder>() {

    class VerifyProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseDescription: TextView = view.findViewById(R.id.courseDescription)
        val typeIcon: ImageView = view.findViewById(R.id.typeIcon)
        val slotType: TextView = view.findViewById(R.id.slotType)
        val slotLeader: TextView = view.findViewById(R.id.slotLeader)
        val verifyBtn: Button = view.findViewById(R.id.verifyBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerifyProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verify_card, parent, false)
        return VerifyProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerifyProjectViewHolder, position: Int) {
        val project = projects[position]

        // Display "course_code - title"
        holder.courseDescription.text = "${project.course_code} - ${project.title}"

        holder.slotType.text = project.type
        holder.slotLeader.text = project.mentor_name

        val iconResId = when (project.type) {
            "App" -> R.drawable.ic_app_dev
            "Product" -> R.drawable.ic_app_dev
            else -> R.drawable.ic_app_dev
        }
        holder.typeIcon.setImageResource(iconResId)

        holder.verifyBtn.setOnClickListener {
            onVerifyClick(project)
        }
    }

    override fun getItemCount() = projects.size
}