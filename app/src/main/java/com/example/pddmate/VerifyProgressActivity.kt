package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

// Data models
data class VerifyProject(
    val project_id: String,
    val course_code: String,
    val title: String,
    val type: String,
    val mentor_name: String
)

data class VerifyProjectListResponse(
    val success: Boolean,
    val data: List<VerifyProject>
)

class VerifyProgressActivity : AppCompatActivity() {

    private lateinit var slotsRecyclerView: RecyclerView
    private lateinit var slotAdapter: VerifySlotsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_progress)

        slotsRecyclerView = findViewById(R.id.slotsRecyclerView)
        slotsRecyclerView.layoutManager = LinearLayoutManager(this)

        val backArrow: ImageView = findViewById(R.id.backArrow)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        fetchProjectsForVerification()
    }

    interface VerifyProgressApi {
        @FormUrlEncoded
        @POST("get_verify_slots.php")
        fun getVerifySlots(@Field("developer_user_id") developerUserId: String): Call<VerifyProjectListResponse>
    }

    private fun fetchProjectsForVerification() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.213.74.64/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
        val api = retrofit.create(VerifyProgressApi::class.java)

        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
        val developerUserId = prefs.getString("user_id", null)

        if (developerUserId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not found. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        api.getVerifySlots(developerUserId).enqueue(object : Callback<VerifyProjectListResponse> {
            override fun onResponse(
                call: Call<VerifyProjectListResponse>,
                response: Response<VerifyProjectListResponse>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val projects = apiResponse.data
                        slotAdapter = VerifySlotsAdapter(projects) { project ->
                            val intent =
                                Intent(this@VerifyProgressActivity, StudentProgressVerificationActivity::class.java)
                            intent.putExtra("project_id", project.project_id)
                            intent.putExtra("course_code", project.course_code) // ✅ already correct
                            intent.putExtra("project_title", project.title)
                            intent.putExtra("project_type", project.type)
                            intent.putExtra("leader_name", project.mentor_name)
                            startActivity(intent)
                        }
                        slotsRecyclerView.adapter = slotAdapter
                    } else {
                        Toast.makeText(this@VerifyProgressActivity, "Failed to load slots", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this@VerifyProgressActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<VerifyProjectListResponse>, t: Throwable) {
                Log.e("VerifyProgressActivity", "Network error", t)
                Toast.makeText(this@VerifyProgressActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }
}