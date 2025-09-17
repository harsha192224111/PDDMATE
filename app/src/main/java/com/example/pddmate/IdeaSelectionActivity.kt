package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
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

    interface ApiService {
        @FormUrlEncoded
        @POST("submit_idea.php")
        fun submitIdea(
            @Field("project_id") projectId: Int,
            @Field("title") title: String,
            @Field("description") description: String
        ): Call<ApiResponse>

        @FormUrlEncoded
        @POST("get_idea.php") // New endpoint for fetching idea
        fun getIdea(@Field("project_id") projectId: Int): Call<IdeaResponse>
    }

    data class ApiResponse(val success: Boolean, val message: String)
    data class IdeaResponse(val success: Boolean, val message: String, val data: IdeaData?)
    data class IdeaData(val title: String, val description: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_idea_selection)

        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.213.74.64/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        projectId = intent.getIntExtra("PROJECT_ID", -1)

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

            submitIdea(projectId, title, desc)
        }

        fetchExistingIdea(projectId)
    }

    private fun fetchExistingIdea(projectId: Int) {
        val call = apiService.getIdea(projectId)
        call.enqueue(object : Callback<IdeaResponse> {
            override fun onResponse(call: Call<IdeaResponse>, response: Response<IdeaResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val ideaData = response.body()?.data
                    if (ideaData != null) {
                        projectTitleInput.setText(ideaData.title)
                        projectDescInput.setText(ideaData.description)
                        Toast.makeText(this@IdeaSelectionActivity, "Previous idea loaded.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // No existing idea or failed to fetch, keep fields empty
                    projectTitleInput.setText("")
                    projectDescInput.setText("")
                }
            }

            override fun onFailure(call: Call<IdeaResponse>, t: Throwable) {
                // Log the error but don't show a toast to the user, just keep fields empty
                // You can add a Log.e here for debugging.
            }
        })
    }

    private fun submitIdea(projectId: Int, title: String, description: String) {
        submitBtn.isEnabled = false
        submitBtn.text = "Submitting..."

        val call = apiService.submitIdea(projectId, title, description)
        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                submitBtn.isEnabled = true
                submitBtn.text = "Submit for Review"

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@IdeaSelectionActivity, "Idea submitted successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to the previous activity (milestones page)
                } else {
                    Toast.makeText(this@IdeaSelectionActivity, "Submission failed: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                submitBtn.isEnabled = true
                submitBtn.text = "Submit for Review"
                Toast.makeText(this@IdeaSelectionActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}