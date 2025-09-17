package com.example.pddmate

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VerifyIdeaSelectionActivity : AppCompatActivity() {

    private lateinit var backArrow: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var projectTitleValue: TextView
    private lateinit var projectDescValue: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_idea_selection)

        backArrow = findViewById(R.id.backArrow)
        titleTextView = findViewById(R.id.title)
        projectTitleValue = findViewById(R.id.projectTitleValue)
        projectDescValue = findViewById(R.id.projectDescValue)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        // Optional: set title from Intent extra
        val milestoneTitle = intent.getStringExtra("MILESTONE_TITLE")
        if (!milestoneTitle.isNullOrBlank()) {
            titleTextView.text = milestoneTitle
        }

        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        acceptButton.setOnClickListener {
            Toast.makeText(this, "Idea Accepted", Toast.LENGTH_SHORT).show()
        }

        rejectButton.setOnClickListener {
            Toast.makeText(this, "Idea Rejected", Toast.LENGTH_SHORT).show()
        }

        // You can populate project title/desc from extras or API here as needed
    }
}
