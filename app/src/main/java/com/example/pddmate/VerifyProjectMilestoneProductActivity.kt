package com.example.pddmate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class VerifyProjectMilestoneProductActivity : AppCompatActivity() {

    private lateinit var steps: List<CardView>
    private lateinit var stepIcons: List<ImageView>
    private lateinit var studentNameTextView: TextView
    private lateinit var studentIdTextView: TextView
    private lateinit var pddTitleTextView: TextView
    private lateinit var apiService: ApiService

    private val milestoneTitles = listOf(
        "Idea Selection",
        "Market Research",
        "Physical Design",
        "Prototyping",
        "Testing & Validation",
        "Manufacturing & Logistics"
    )
    private val milestonePhases = mutableMapOf<Int, String>()

    private var projectId: Int = -1
    private var studentUserId: String? = null
    private var studentName: String? = null
    private var projectTitle: String? = null

    private val verifyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchMilestonePhases()
        }
    }

    interface ApiService {
        @FormUrlEncoded
        @POST("get_milestone_phases.php")
        fun getMilestonePhases(
            @Field("project_id") projectId: Int,
            @Field("user_id") userId: String
        ): Call<PhasesResponse>
    }

    data class PhasesResponse(val success: Boolean, val message: String, val phases: Map<Int, String>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_project_milestone_product)

        projectId = intent.getIntExtra("project_id", -1)
        studentUserId = intent.getStringExtra("student_user_id")
        studentName = intent.getStringExtra("student_name")
        projectTitle = intent.getStringExtra("project_title")

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        studentNameTextView = findViewById(R.id.studentNameTextView)
        studentIdTextView = findViewById(R.id.studentIdTextView)
        pddTitleTextView = findViewById(R.id.pddTitle)

        studentNameTextView.text = studentName
        studentIdTextView.text = studentUserId
        pddTitleTextView.text = projectTitle

        stepIcons = listOf(
            findViewById(R.id.step0_icon),
            findViewById(R.id.step1_icon),
            findViewById(R.id.step2_icon),
            findViewById(R.id.step3_icon),
            findViewById(R.id.step4_icon),
            findViewById(R.id.step5_icon)
        )

        steps = listOf(
            findViewById(R.id.step0_card),
            findViewById(R.id.step1_card),
            findViewById(R.id.step2_card),
            findViewById(R.id.step3_card),
            findViewById(R.id.step4_card),
            findViewById(R.id.step5_card)
        )

        val backArrow: ImageView = findViewById(R.id.backArrow)
        backArrow.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        steps.forEachIndexed { index, card ->
            card.setOnClickListener {
                val title = milestoneTitles[index]
                if (index == 0) {
                    val intent = Intent(this, VerifyIdeaSelectionActivity::class.java)
                    intent.putExtra("MILESTONE_TITLE", title)
                    intent.putExtra("STEP_INDEX", index)
                    intent.putExtra("project_id", projectId)
                    intent.putExtra("student_user_id", studentUserId)
                    intent.putExtra("student_name", studentName)
                    intent.putExtra("project_title", projectTitle)
                    verifyLauncher.launch(intent)
                } else {
                    val intent = Intent(this, VerifyFileUploadsActivity::class.java)
                    intent.putExtra("MILESTONE_TITLE", title)
                    intent.putExtra("STEP_INDEX", index)
                    intent.putExtra("project_id", projectId)
                    intent.putExtra("student_user_id", studentUserId)
                    intent.putExtra("student_name", studentName)
                    intent.putExtra("project_title", projectTitle)
                    verifyLauncher.launch(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchMilestonePhases()
    }

    private fun fetchMilestonePhases() {
        if (projectId == -1 || studentUserId.isNullOrEmpty()) {
            Toast.makeText(this, "Project or student data not available.", Toast.LENGTH_SHORT).show()
            return
        }
        apiService.getMilestonePhases(projectId, studentUserId!!).enqueue(object : Callback<PhasesResponse> {
            override fun onResponse(call: Call<PhasesResponse>, response: Response<PhasesResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val phases = response.body()?.phases ?: emptyMap()
                    milestonePhases.clear()
                    milestonePhases.putAll(phases)
                } else {
                    Log.e("VerifyMilestoneProduct", "Failed to fetch phases: ${response.body()?.message}")
                }
                updateMilestoneIcons()
            }

            override fun onFailure(call: Call<PhasesResponse>, t: Throwable) {
                Log.e("VerifyMilestoneProduct", "Network error fetching phases", t)
                updateMilestoneIcons()
            }
        })
    }

    private fun updateMilestoneIcons() {
        steps.forEachIndexed { index, _ ->
            val phase = milestonePhases[index] ?: "not_submitted"
            val iconRes = when (phase) {
                "pending" -> R.drawable.ic_pending_yellow
                "accepted" -> R.drawable.ic_accepted
                "rejected" -> R.drawable.ic_rejected
                else -> R.drawable.ic_phase
            }
            if (index < stepIcons.size) {
                stepIcons[index].setImageResource(iconRes)
            }
        }
    }
}