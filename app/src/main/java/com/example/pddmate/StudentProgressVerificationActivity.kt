package com.example.pddmate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- Data classes ---
data class Student(
    @SerializedName("user_id") val userId: String,
    val name: String,
    @SerializedName("slot_type") val slotType: String // "App" or "Product"
)

data class StudentListResponse(
    val success: Boolean,
    val data: List<Student>
)

// --- Retrofit API interface ---
interface StudentProgressApi {
    @GET("get_project_students_with_slot.php")
    fun getProjectStudents(@Query("project_id") projectId: String): Call<StudentListResponse>
}

// --- Main Activity ---
class StudentProgressVerificationActivity : AppCompatActivity() {

    private lateinit var projectTitleTextView: TextView
    private lateinit var projectTypeTextView: TextView
    private lateinit var leaderNameTextView: TextView
    private lateinit var studentsRecyclerView: RecyclerView
    private lateinit var studentsAdapter: VerifyStudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_progress_verification)

        projectTitleTextView = findViewById(R.id.projectTitle)
        projectTypeTextView = findViewById(R.id.projectType)
        leaderNameTextView = findViewById(R.id.leaderName)
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView)
        val backArrow: ImageView = findViewById(R.id.backArrow)

        studentsRecyclerView.layoutManager = LinearLayoutManager(this)
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val projectId = intent.getStringExtra("project_id")
        val courseCode = intent.getStringExtra("course_code")
        val projectTitle = intent.getStringExtra("project_title")
        val projectType = intent.getStringExtra("project_type")
        val leaderName = intent.getStringExtra("leader_name")

        projectTitleTextView.text = "$courseCode - $projectTitle"
        projectTypeTextView.text = projectType
        leaderNameTextView.text = leaderName

        if (projectId != null) {
            fetchStudents(projectId)
        } else {
            Toast.makeText(this, "Project ID not found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchStudents(projectId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.213.74.64/pdd_dashboard/")  // Update base URL as needed
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        val api = retrofit.create(StudentProgressApi::class.java)
        api.getProjectStudents(projectId).enqueue(object : Callback<StudentListResponse> {
            override fun onResponse(call: Call<StudentListResponse>, response: Response<StudentListResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val students = apiResponse.data
                        studentsAdapter = VerifyStudentAdapter(this@StudentProgressVerificationActivity, students) { student ->
                            openProjectMilestones(student)
                        }
                        studentsRecyclerView.adapter = studentsAdapter
                    } else {
                        Toast.makeText(this@StudentProgressVerificationActivity, "Failed to load students", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@StudentProgressVerificationActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<StudentListResponse>, t: Throwable) {
                Log.e("StudentVerification", "Network error", t)
                Toast.makeText(this@StudentProgressVerificationActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openProjectMilestones(student: Student) {
        when (student.slotType.uppercase()) {
            "APP" -> {
                val intent = Intent(this, VerifyProjectMilestoneAppActivity::class.java)
                intent.putExtra("student_id", student.userId)
                intent.putExtra("student_name", student.name)
                intent.putExtra("student_slot", student.slotType)
                startActivity(intent)
            }
            "PRODUCT" -> {
                val intent = Intent(this, VerifyProjectMilestoneProductActivity::class.java)
                intent.putExtra("student_id", student.userId)
                intent.putExtra("student_name", student.name)
                intent.putExtra("student_slot", student.slotType)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "No slot enrolled for this student", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
