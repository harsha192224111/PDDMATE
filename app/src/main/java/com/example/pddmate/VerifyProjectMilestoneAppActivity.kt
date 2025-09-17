package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class VerifyProjectMilestoneAppActivity : AppCompatActivity() {

    private lateinit var steps: List<CardView>
    private lateinit var stepIcons: List<ImageView>

    private val milestoneTitles = listOf(
        "Idea Selection",
        "UI/UX Design",
        "Frontend Development",
        "Backend Integration",
        "Testing & Debugging",
        "Deployment & Maintenance"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_project_milestone_app)

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
        stepIcons.forEach { it.setImageResource(phaseIconRes) }

        steps.forEachIndexed { index, card ->
            card.setOnClickListener {
                val title = milestoneTitles[index]
                if (title == "Idea Selection") {
                    // Launch VerifyIdeaSelectionActivity for Idea Selection
                    val intent = Intent(this, VerifyIdeaSelectionActivity::class.java)
                    intent.putExtra("MILESTONE_TYPE", "APP")
                    intent.putExtra("MILESTONE_TITLE", title)
                    startActivity(intent)
                } else {
                    // Launch VerifyFileUploadsActivity for other milestones
                    val intent = Intent(this, VerifyFileUploadsActivity::class.java)
                    intent.putExtra("MILESTONE_TYPE", "APP")
                    intent.putExtra("MILESTONE_TITLE", title)
                    intent.putExtra("STEP_INDEX", index)
                    startActivity(intent)
                }
            }
        }
    }
}
