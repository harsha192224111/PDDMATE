package com.example.pddmate

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    // --- Data Models ---
    data class IdeaData(
        val title: String,
        val description: String
    )

    data class IdeaResponse(
        val success: Boolean,
        val message: String,
        val data: IdeaData?
    )

    // --- Retrofit API Service for this activity ---
    interface ApiService {
        @FormUrlEncoded
        @POST("verify_idea.php")
        fun getIdea(
            @Field("project_id") projectId: Int,
            @Field("user_id") userId: String
        ): Call<IdeaResponse>
    }

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_idea_selection)

        // --- Retrofit Setup ---
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // --- Get intent extras ---
        projectId = intent.getIntExtra("project_id", -1)
        studentUserId = intent.getStringExtra("student_user_id")
        studentName = intent.getStringExtra("student_name")
        milestoneTitle = intent.getStringExtra("MILESTONE_TITLE")

        // --- Bind Views ---
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

        // --- Fetch student idea using studentUserId ---
        if (projectId != -1 && !studentUserId.isNullOrEmpty()) {
            fetchExistingIdea(projectId, studentUserId!!)
        } else {
            Toast.makeText(this, "Invalid project or student data", Toast.LENGTH_SHORT).show()
        }

        acceptButton.setOnClickListener {
            // TODO: Call API to update approval status
            Toast.makeText(this, "Idea accepted!", Toast.LENGTH_SHORT).show()
        }

        rejectButton.setOnClickListener {
            // TODO: Call API to update rejection status
            Toast.makeText(this, "Idea rejected.", Toast.LENGTH_SHORT).show()
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
}