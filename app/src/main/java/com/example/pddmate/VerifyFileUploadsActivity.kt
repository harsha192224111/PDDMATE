package com.example.pddmate

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class VerifyFileUploadsActivity : AppCompatActivity() {

    private lateinit var backArrow: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    private lateinit var fileCard1: CardView
    private lateinit var fileCard2: CardView
    private lateinit var fileCard3: CardView
    private lateinit var fileName1: TextView
    private lateinit var fileName2: TextView
    private lateinit var fileName3: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_file_uploads)

        backArrow = findViewById(R.id.backArrow)
        titleTextView = findViewById(R.id.title)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        fileCard1 = findViewById(R.id.fileCard1)
        fileCard2 = findViewById(R.id.fileCard2)
        fileCard3 = findViewById(R.id.fileCard3)

        fileName1 = findViewById(R.id.fileName1)
        fileName2 = findViewById(R.id.fileName2)
        fileName3 = findViewById(R.id.fileName3)

        // Set dynamic title from intent extra
        val milestoneTitle = intent.getStringExtra("MILESTONE_TITLE")
        if (!milestoneTitle.isNullOrBlank()) {
            titleTextView.text = milestoneTitle
        }

        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        acceptButton.setOnClickListener {
            Toast.makeText(this, "Files Accepted", Toast.LENGTH_SHORT).show()
        }

        rejectButton.setOnClickListener {
            Toast.makeText(this, "Files Rejected", Toast.LENGTH_SHORT).show()
        }

        fileCard1.setOnClickListener {
            Toast.makeText(this, "Opening ${fileName1.text}", Toast.LENGTH_SHORT).show()
        }
        fileCard2.setOnClickListener {
            Toast.makeText(this, "Opening ${fileName2.text}", Toast.LENGTH_SHORT).show()
        }
        fileCard3.setOnClickListener {
            Toast.makeText(this, "Opening ${fileName3.text}", Toast.LENGTH_SHORT).show()
        }
    }
}
