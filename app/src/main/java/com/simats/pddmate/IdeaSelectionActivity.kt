package com.simats.pddmate

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class IdeaSelectionActivity : AppCompatActivity() {
    private lateinit var backArrow: ImageView
    private lateinit var projectTitleInput: EditText
    private lateinit var projectDescInput: EditText
    private lateinit var submitBtn: Button
    private lateinit var titleTextView: TextView
    private lateinit var apiService: ApiService
    private var projectId: Int = -1
    private var userId: String? = null
    private var milestoneType: String? = null

    interface ApiService {
        @FormUrlEncoded
        @POST("submit_idea.php")
        fun submitIdea(
            @Field("project_id") projectId: Int,
            @Field("user_id") userId: String,
            @Field("title") title: String,
            @Field("description") description: String
        ): Call<ApiResponse>

        @FormUrlEncoded
        @POST("get_idea.php")
        fun getIdea(
            @Field("project_id") projectId: Int,
            @Field("user_id") String: String
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

    data class ApiResponse(val success: Boolean, val message: String)
    data class IdeaResponse(val success: Boolean, val message: String, val data: IdeaData?)
    data class IdeaData(val title: String, val description: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idea_selection)

        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://14.139.187.229:8081/pddmate/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = retrofit.create(ApiService::class.java)

        projectId = intent.getIntExtra("PROJECT_ID", -1)
        userId = intent.getStringExtra("USER_ID")
        milestoneType = intent.getStringExtra("MILESTONE_TYPE")

        backArrow = findViewById(R.id.backArrow)
        projectTitleInput = findViewById(R.id.projectTitleInput)
        projectDescInput = findViewById(R.id.projectDescInput)
        submitBtn = findViewById(R.id.submitBtn)
        titleTextView = findViewById(R.id.title)

        backArrow.setOnClickListener { finish() }

        submitBtn.setOnClickListener {
            val title = projectTitleInput.text.toString().trim()
            val desc = projectDescInput.text.toString().trim()
            if (title.isEmpty()) {
                projectTitleInput.error = "Please enter project title"
                projectTitleInput.requestFocus()
                return@setOnClickListener
            }
            if (desc.isEmpty()) {
                projectDescInput.error = "Please enter project description"
                projectDescInput.requestFocus()
                return@setOnClickListener
            }
            if (!userId.isNullOrEmpty()) {
                submitIdea(projectId, userId!!, title, desc)
            } else {
                Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show()
            }
        }

        if (projectId != -1 && !userId.isNullOrEmpty()) {
            fetchExistingIdea(projectId, userId!!)
        }
    }

    private fun fetchExistingIdea(projectId: Int, userId: String) {
        val call = apiService.getIdea(projectId, userId)
        call.enqueue(object : Callback<IdeaResponse> {
            override fun onResponse(call: Call<IdeaResponse>, response: Response<IdeaResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        projectTitleInput.setText(body.data.title)
                        projectDescInput.setText(body.data.description)
                        Toast.makeText(
                            this@IdeaSelectionActivity,
                            "Previous idea loaded.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        projectTitleInput.setText("")
                        projectDescInput.setText("")
                    }
                } else {
                    projectTitleInput.setText("")
                    projectDescInput.setText("")
                }
            }

            override fun onFailure(call: Call<IdeaResponse>, t: Throwable) {
                Log.e("IdeaSelectionActivity", "Error fetching idea", t)
                Toast.makeText(this@IdeaSelectionActivity, "Network error fetching previous idea.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun submitIdea(projectId: Int, userId: String, title: String, description: String) {
        submitBtn.isEnabled = false
        submitBtn.text = "Submitting..."
        val call = apiService.submitIdea(projectId, userId, title, description)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                submitBtn.isEnabled = true
                submitBtn.text = "Submit for Review"
                if (response.isSuccessful && response.body()?.success == true) {
                    setMilestonePhase(projectId, userId, 0, "pending")
                } else {
                    val msg = response.body()?.message ?: "Unknown error"
                    Toast.makeText(this@IdeaSelectionActivity, "Submission failed: $msg", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                submitBtn.isEnabled = true
                submitBtn.text = "Submit for Review"
                Toast.makeText(this@IdeaSelectionActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setMilestonePhase(projectId: Int, userId: String, milestoneIndex: Int, phase: String) {
        apiService.setMilestonePhase(projectId, userId, milestoneIndex, phase)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@IdeaSelectionActivity, "Idea submitted for review!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("IdeaSelectionActivity", "Failed to update milestone phase.")
                        Toast.makeText(this@IdeaSelectionActivity, "Idea submitted, but failed to update status.", Toast.LENGTH_LONG).show()
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e("IdeaSelectionActivity", "Network error updating milestone phase: ${t.message}")
                    Toast.makeText(this@IdeaSelectionActivity, "Idea submitted, but failed to update status.", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            })
    }
}