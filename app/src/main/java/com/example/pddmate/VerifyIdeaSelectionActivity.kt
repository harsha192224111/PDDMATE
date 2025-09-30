package com.example.pddmate

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class VerifyIdeaSelectionActivity : AppCompatActivity() {

    private lateinit var backArrow: ImageView
    private lateinit var projectTitleValue: TextView
    private lateinit var projectDescValue: TextView
    private lateinit var milestoneTitleTextView: TextView
    private lateinit var studentNameTextView: TextView
    private lateinit var studentIdTextView: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    private var projectId: Int = -1
    private var studentName: String? = null
    private var milestoneTitle: String? = null
    private var studentUserId: String? = null
    private var stepIndex: Int = -1

    data class IdeaData(
        val title: String,
        val description: String
    )

    data class IdeaResponse(
        val success: Boolean,
        val message: String,
        val data: IdeaData?
    )

    data class ApiResponse(val success: Boolean, val message: String)

    interface ApiService {
        @FormUrlEncoded
        @POST("get_idea.php")
        fun getIdea(
            @Field("project_id") projectId: Int,
            @Field("user_id") userId: String
        ): Call<IdeaResponse>

        @FormUrlEncoded
        @POST("set_milestone_phase.php")
        fun setMilestonePhase(
            @Field("project_id") projectId: Int,
            @Field("user_id") userId: String,
            @Field("milestone_index") milestoneIndex: Int,
            @Field("phase") phase: String
        ): Call<ApiResponse>
    }

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_idea_selection)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        projectId = intent.getIntExtra("project_id", -1)
        studentUserId = intent.getStringExtra("student_user_id")
        studentName = intent.getStringExtra("student_name")
        milestoneTitle = intent.getStringExtra("MILESTONE_TITLE")
        stepIndex = intent.getIntExtra("STEP_INDEX", -1)

        backArrow = findViewById(R.id.backArrow)
        projectTitleValue = findViewById(R.id.projectTitleValue)
        projectDescValue = findViewById(R.id.projectDescValue)
        milestoneTitleTextView = findViewById(R.id.title)
        studentNameTextView = findViewById(R.id.studentNameTextView)
        studentIdTextView = findViewById(R.id.studentIdTextView)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        backArrow.setOnClickListener { finish() }

        milestoneTitleTextView.text = milestoneTitle
        studentNameTextView.text = studentName
        studentIdTextView.text = studentUserId

        if (projectId != -1 && !studentUserId.isNullOrEmpty()) {
            fetchExistingIdea(projectId, studentUserId!!)
        } else {
            Toast.makeText(this, "Invalid project or student data", Toast.LENGTH_SHORT).show()
        }

        acceptButton.setOnClickListener {
            updateMilestoneStatus("accepted")
        }

        rejectButton.setOnClickListener {
            updateMilestoneStatus("rejected")
        }
    }

    private fun fetchExistingIdea(projectId: Int, userId: String) {
        val call = apiService.getIdea(projectId, userId)
        call.enqueue(object : Callback<IdeaResponse> {
            override fun onResponse(call: Call<IdeaResponse>, response: Response<IdeaResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        projectTitleValue.text = body.data.title
                        projectDescValue.text = body.data.description
                        Toast.makeText(this@VerifyIdeaSelectionActivity, "Idea loaded.", Toast.LENGTH_SHORT).show()
                    } else {
                        projectTitleValue.text = "No idea submitted yet."
                        projectDescValue.text = "No description available."
                        Toast.makeText(this@VerifyIdeaSelectionActivity, body?.message ?: "No idea found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    projectTitleValue.text = "Error fetching idea."
                    projectDescValue.text = ""
                    Toast.makeText(this@VerifyIdeaSelectionActivity, "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<IdeaResponse>, t: Throwable) {
                Log.e("VerifyIdeaSelection", "Network error: ${t.message}")
                Toast.makeText(this@VerifyIdeaSelectionActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun updateMilestoneStatus(phase: String) {
        if (projectId == -1 || studentUserId.isNullOrEmpty() || stepIndex == -1) {
            Toast.makeText(this, "Cannot update status. Missing data.", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.setMilestonePhase(projectId, studentUserId!!, stepIndex, phase).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@VerifyIdeaSelectionActivity, "Milestone updated to $phase!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@VerifyIdeaSelectionActivity, "Failed to update status. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@VerifyIdeaSelectionActivity, "Network error updating status.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}