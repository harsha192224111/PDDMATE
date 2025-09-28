package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class ProjectMilestoneProductActivity : AppCompatActivity() {

    private lateinit var steps: List<ConstraintLayout>
    private lateinit var stepIcons: List<ImageView>
    private var userId: String? = null
    private var projectId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_milestone_product)

        userId = intent.getStringExtra("USER_ID")
        projectId = intent.getIntExtra("PROJECT_ID", -1)

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

        steps.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                openMilestoneDetail(index)
            }
        }
    }

    private fun openMilestoneDetail(stepIndex: Int) {
        if (userId == null || projectId == -1) {
            // Handle case where user ID or project ID is not available
            return
        }

        val intent: Intent
        if (stepIndex == 0) {
            intent = Intent(this, IdeaSelectionActivity::class.java)
        } else {
            intent = Intent(this, MilestoneDetailActivity::class.java)
            intent.putExtra("MILESTONE_TYPE", "PRODUCT")
            intent.putExtra("STEP_INDEX", stepIndex)
        }
        intent.putExtra("PROJECT_ID", projectId) // Pass the correct project ID
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }
}
