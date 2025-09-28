package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class VerifyProjectMilestoneProductActivity : AppCompatActivity() {

    private lateinit var steps: List<CardView>
    private lateinit var stepIcons: List<ImageView>
    private lateinit var studentNameTextView: TextView
    private lateinit var studentIdTextView: TextView
    private lateinit var pddTitleTextView: TextView

    private val milestoneTitles = listOf(
        "Idea Selection",
        "Market Research",
        "Physical Design",
        "Prototyping",
        "Testing & Validation",
        "Manufacturing & Logistics"
    )

    private var projectId: Int = -1
    private var studentUserId: String? = null
    private var studentName: String? = null
    private var projectTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_project_milestone_product)

        projectId = intent.getIntExtra("project_id", -1)
        studentUserId = intent.getStringExtra("student_user_id")
        studentName = intent.getStringExtra("student_name")
        projectTitle = intent.getStringExtra("project_title")

        studentNameTextView = findViewById(R.id.studentNameTextView)
        studentIdTextView = findViewById(R.id.studentIdTextView)
        pddTitleTextView = findViewById(R.id.pddTitle)

        studentNameTextView.text = studentName
        studentIdTextView.text = studentUserId
        pddTitleTextView.text = projectTitle

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

        val backArrow: ImageView = findViewById(R.id.backArrow)
        backArrow.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val phaseIconRes = R.drawable.ic_phase
        stepIcons.forEach { it.setImageResource(phaseIconRes) }

        steps.forEachIndexed { index, card ->
            card.setOnClickListener {
                val title = milestoneTitles[index]
                if (index == 0) {
                    val intent = Intent(this, VerifyIdeaSelectionActivity::class.java)
                    intent.putExtra("MILESTONE_TYPE", "PRODUCT")
                    intent.putExtra("MILESTONE_TITLE", title)
                    intent.putExtra("project_id", projectId)
                    intent.putExtra("student_user_id", studentUserId)
                    intent.putExtra("student_name", studentName)
                    intent.putExtra("project_title", projectTitle)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, VerifyFileUploadsActivity::class.java)
                    intent.putExtra("MILESTONE_TYPE", "PRODUCT")
                    intent.putExtra("MILESTONE_TITLE", title)
                    intent.putExtra("STEP_INDEX", index)
                    intent.putExtra("project_id", projectId)
                    intent.putExtra("student_user_id", studentUserId)
                    intent.putExtra("student_name", studentName)
                    intent.putExtra("project_title", projectTitle)
                    startActivity(intent)
                }
            }
        }
    }
}
