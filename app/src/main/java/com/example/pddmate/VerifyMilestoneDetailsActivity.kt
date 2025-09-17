package com.example.pddmate

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class VerifyMilestoneDetailActivity : AppCompatActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    // Define variables only for views guaranteed to exist in the layouts
    private lateinit var fileName1: TextView
    private lateinit var fileName2: TextView
    private lateinit var fileName3: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val milestoneType = intent.getStringExtra("MILESTONE_TYPE") ?: ""
        val stepIndex = intent.getIntExtra("STEP_INDEX", -1)

        if (stepIndex == 0) {
            // Show the idea selection milestone layout
            setContentView(R.layout.activity_verify_idea_selection)
            titleTextView = findViewById(R.id.title)
            titleTextView.text = getString(R.string.idea_selection)
            // If you need to assign descriptions, add more code as needed
        } else {
            // Show file uploads layout for other milestones
            setContentView(R.layout.activity_verify_file_uploads)

            titleTextView = findViewById(R.id.title)
            acceptButton = findViewById(R.id.acceptButton)
            rejectButton = findViewById(R.id.rejectButton)

            // These are included for easy extension (if you want click behavior or display file names)
            fileName1 = findViewById(R.id.fileName1)
            fileName2 = findViewById(R.id.fileName2)
            fileName3 = findViewById(R.id.fileName3)

            // Set titles based on milestone type and step index
            val (mainTitle, description) = when (milestoneType.uppercase()) {
                "APP" -> when (stepIndex) {
                    1 -> getString(R.string.uiux_title) to getString(R.string.uiux_desc)
                    2 -> getString(R.string.frontend_title) to getString(R.string.frontend_desc)
                    3 -> getString(R.string.backend_title) to getString(R.string.backend_desc)
                    4 -> getString(R.string.testing_title) to getString(R.string.testing_desc)
                    5 -> getString(R.string.documentation_title) to getString(R.string.documentation_desc)
                    else -> "" to ""
                }
                "PRODUCT" -> when (stepIndex) {
                    1 -> getString(R.string.modelling_title) to getString(R.string.modelling_desc)
                    2 -> getString(R.string.prototype_title) to getString(R.string.prototype_desc)
                    3 -> getString(R.string.validation_title) to getString(R.string.validation_desc)
                    4 -> getString(R.string.integration_title) to getString(R.string.integration_desc)
                    5 -> getString(R.string.documentation_title) to getString(R.string.documentation_desc)
                    else -> "" to ""
                }
                else -> "" to ""
            }

            titleTextView.text = mainTitle

            // You may assign the description to another field if present, otherwise you can skip or show it in a Toast
            if (description.isNotBlank()) {
                Toast.makeText(this, description, Toast.LENGTH_LONG).show()
            }

            acceptButton.setOnClickListener {
                Toast.makeText(this, getString(R.string.files_submitted_message), Toast.LENGTH_SHORT).show()
            }
            rejectButton.setOnClickListener {
                Toast.makeText(this, "Files rejected", Toast.LENGTH_SHORT).show()
            }

            // Optional: click listeners to preview files
            fileName1.setOnClickListener {
                Toast.makeText(this, "Preview ${fileName1.text}", Toast.LENGTH_SHORT).show()
            }
            fileName2.setOnClickListener {
                Toast.makeText(this, "Preview ${fileName2.text}", Toast.LENGTH_SHORT).show()
            }
            fileName3.setOnClickListener {
                Toast.makeText(this, "Preview ${fileName3.text}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
