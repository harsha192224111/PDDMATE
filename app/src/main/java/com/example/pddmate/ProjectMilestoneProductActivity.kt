package com.example.pddmate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
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

class ProjectMilestoneProductActivity : AppCompatActivity() {

    private lateinit var steps: List<CardView>
    private lateinit var stepIcons: List<ImageView>
    private var userId: String? = null
    private var projectId: Int = -1
    private lateinit var apiService: ApiService

    private val milestonePhases = mutableMapOf<Int, String>()

    private val milestoneDetailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        setContentView(R.layout.activity_project_milestone_product)

        userId = intent.getStringExtra("USER_ID")
        projectId = intent.getIntExtra("PROJECT_ID", -1)

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

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
        backArrow.setOnClickListener { finish() }

        steps.forEachIndexed { index, layout ->
            layout.setOnClickListener {
                openMilestoneDetail(index)
            }
        }

        fetchMilestonePhases()
    }

    override fun onResume() {
        super.onResume()
        fetchMilestonePhases()
    }

    private fun fetchMilestonePhases() {
        if (projectId == -1 || userId.isNullOrEmpty()) {
            Toast.makeText(this, "Project or user data not available.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.getMilestonePhases(projectId, userId!!).enqueue(object : Callback<PhasesResponse> {
            override fun onResponse(call: Call<PhasesResponse>, response: Response<PhasesResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val phases = response.body()?.phases ?: emptyMap()
                    milestonePhases.clear()
                    milestonePhases.putAll(phases)
                } else {
                    Log.e("ProjectMilestoneProduct", "Failed to fetch phases: ${response.body()?.message}")
                }
                updateMilestoneIcons()
            }

            override fun onFailure(call: Call<PhasesResponse>, t: Throwable) {
                Log.e("ProjectMilestoneProduct", "Network error fetching phases", t)
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

    private fun openMilestoneDetail(stepIndex: Int) {
        if (userId == null || projectId == -1) {
            Toast.makeText(this, "User ID or project ID not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent: Intent
        if (stepIndex == 0) {
            intent = Intent(this, IdeaSelectionActivity::class.java)
            intent.putExtra("MILESTONE_TYPE", "PRODUCT")
        } else {
            intent = Intent(this, MilestoneDetailActivity::class.java)
            intent.putExtra("MILESTONE_TYPE", "PRODUCT")
            intent.putExtra("STEP_INDEX", stepIndex)
        }
        intent.putExtra("PROJECT_ID", projectId)
        intent.putExtra("USER_ID", userId)
        milestoneDetailLauncher.launch(intent)
    }
}