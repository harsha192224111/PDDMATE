package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Student(
    val user_id: String,
    val name: String,
    val slot_type: String // "App" or "Product"
)

data class StudentListResponse(
    val success: Boolean,
    val data: List<Student>
)

interface StudentProgressApi {
    @GET("get_project_students_with_slot.php")
    fun getProjectStudents(@Query("project_id") projectId: String): Call<StudentListResponse>
}

class StudentProgressVerificationActivity : AppCompatActivity() {

    private lateinit var studentsRecyclerView: RecyclerView
    private lateinit var studentsAdapter: VerifyStudentAdapter
    private var projectId: String? = null
    private var projectTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_progress_verification)

        studentsRecyclerView = findViewById(R.id.studentsRecyclerView)
        val backArrow: ImageView = findViewById(R.id.backArrow)

        studentsRecyclerView.layoutManager = LinearLayoutManager(this)
        backArrow.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        projectId = intent.getStringExtra("project_id")
        projectTitle = intent.getStringExtra("project_title")

        if (projectId != null) {
            fetchStudents(projectId!!)
        } else {
            Toast.makeText(this, "Project ID not found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchStudents(projectId: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(StudentProgressApi::class.java)
        api.getProjectStudents(projectId).enqueue(object : Callback<StudentListResponse> {
            override fun onResponse(call: Call<StudentListResponse>, response: Response<StudentListResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse?.success == true) {
                        val students = apiResponse.data
                        studentsAdapter = VerifyStudentAdapter(
                            this@StudentProgressVerificationActivity,
                            students,
                            ::openProjectMilestones
                        )
                        studentsRecyclerView.adapter = studentsAdapter
                    } else {
                        Toast.makeText(this@StudentProgressVerificationActivity, "Failed to load students", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@StudentProgressVerificationActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<StudentListResponse>, t: Throwable) {
                Toast.makeText(this@StudentProgressVerificationActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openProjectMilestones(student: Student) {
        val projectIdInt = projectId?.toIntOrNull() ?: -1
        if (projectIdInt == -1) {
            Toast.makeText(this, "Invalid Project ID", Toast.LENGTH_SHORT).show()
            return
        }
        when (student.slot_type.uppercase()) {
            "APP" -> {
                val intent = Intent(this, VerifyProjectMilestoneAppActivity::class.java)
                intent.putExtra("student_user_id", student.user_id) // pass actual user_id of student
                intent.putExtra("student_name", student.name)
                intent.putExtra("project_id", projectIdInt)
                intent.putExtra("project_title", projectTitle)
                startActivity(intent)
            }
            "PRODUCT" -> {
                val intent = Intent(this, VerifyProjectMilestoneProductActivity::class.java)
                intent.putExtra("student_user_id", student.user_id)
                intent.putExtra("student_name", student.name)
                intent.putExtra("project_id", projectIdInt)
                intent.putExtra("project_title", projectTitle)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "No slot enrolled for this student", Toast.LENGTH_SHORT).show()
            }
        }
    }
}