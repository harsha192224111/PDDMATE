package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class ProjectMilestoneProductActivity : AppCompatActivity() {

    private lateinit var steps: List<ConstraintLayout>
    private lateinit var stepIcons: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_milestone_product)

        // Initialize step containers and icons all with ic_phase initially
        stepIcons = listOf(
            findViewById(R.id.step1_icon),
            findViewById(R.id.step2_icon),
            findViewById(R.id.step3_icon),
            findViewById(R.id.step4_icon),
            findViewById(R.id.step5_icon),
            findViewById(R.id.step6_icon)
        )

        steps = listOf(
            findViewById(R.id.step1_card),
            findViewById(R.id.step2_card),
            findViewById(R.id.step3_card),
            findViewById(R.id.step4_card),
            findViewById(R.id.step5_card),
            findViewById(R.id.step6_card)
        )

        val phaseIconRes = R.drawable.ic_phase
        stepIcons.forEach { imageView ->
            imageView.setImageResource(phaseIconRes)
        }

        // Set click listeners on steps
        steps.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                openMilestoneDetail(index)
            }
        }
    }

    private fun openMilestoneDetail(stepIndex: Int) {
        val intent: Intent
        if (stepIndex == 0) {
            // "Idea Selection" is the first step (index 0)
            intent = Intent(this, IdeaSelectionActivity::class.java)
        } else {
            intent = Intent(this, MilestoneDetailActivity::class.java)
            intent.putExtra("MILESTONE_TYPE", "PRODUCT")
            intent.putExtra("STEP_INDEX", stepIndex)
        }
        intent.putExtra("PROJECT_ID", 1) // Pass a placeholder project ID
        startActivity(intent)
    }
}