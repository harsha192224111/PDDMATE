package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pddmate.databinding.ActivityStudentHomePageBinding
import com.google.gson.GsonBuilder
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class StudentHomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentHomePageBinding
    private var selectedCalendar: Calendar = Calendar.getInstance()
    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private val weekDays = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // New API Service and Data Model
    data class EnrollmentResponse(
        val success: Boolean,
        val project_id: Int?
    )
    interface EnrollmentApiService {
        @FormUrlEncoded
        @POST("get_student_enrollment.php")
        fun getStudentEnrollment(@Field("user_id") userId: String): Call<EnrollmentResponse>
    }
    private lateinit var enrollmentApiService: EnrollmentApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Retrofit with lenient Gson to handle potential non-standard JSON responses
        val gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        enrollmentApiService = retrofit.create(EnrollmentApiService::class.java)

        binding.titleTextView.text = getString(R.string.pdd_mate)
        binding.menuImageButton.setOnClickListener { showPopupMenu(it) }

        updateMonthYearViews()
        setupWeekDaysHeader()
        setupCalendarRecyclerView()

        binding.prevMonthButton.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, -1)
            updateMonthYearViews()
            setupCalendarRecyclerView()
        }
        binding.nextMonthButton.setOnClickListener {
            selectedCalendar.add(Calendar.MONTH, 1)
            updateMonthYearViews()
            setupCalendarRecyclerView()
        }
        binding.monthTextView.setOnClickListener { showMonthDropdown() }
        binding.yearTextView.setOnClickListener { showYearDropdown() }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.student_home_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.menu_slot_enrollment -> {
                    openSlotEnrollment()
                    true
                }
                R.id.menu_project_milestones -> {
                    openProjectMilestones()
                    true
                }
                R.id.menu_logout -> {
                    logout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openSlotEnrollment() {
        startActivity(Intent(this, SlotEnrollmentActivity::class.java))
    }

    private fun openProjectMilestones() {
        val sharedPrefs = getSharedPreferences("login_session", MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", null)

        if (userId == null) {
            Toast.makeText(this, "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch the project ID from the server
        enrollmentApiService.getStudentEnrollment(userId).enqueue(object : Callback<EnrollmentResponse> {
            override fun onResponse(call: Call<EnrollmentResponse>, response: Response<EnrollmentResponse>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.project_id != null) {
                        launchProjectMilestones(body.project_id, userId)
                    } else {
                        // Success=false, meaning no approved enrollment found. Redirect to enrollment.
                        Toast.makeText(this@StudentHomePageActivity, "No approved project found. Please enroll.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@StudentHomePageActivity, SlotEnrollmentActivity::class.java))
                    }
                } else {
                    Log.e("StudentHomePage", "Enrollment API Server Error: ${response.code()}")
                    Toast.makeText(this@StudentHomePageActivity, "Server communication error. Please check enrollment.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@StudentHomePageActivity, SlotEnrollmentActivity::class.java))
                }
            }

            override fun onFailure(call: Call<EnrollmentResponse>, t: Throwable) {
                Log.e("StudentHomePage", "Enrollment API Network Failure: ${t.message}", t)
                Toast.makeText(this@StudentHomePageActivity, "Network error: Could not fetch project details.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@StudentHomePageActivity, SlotEnrollmentActivity::class.java))
            }
        })
    }

    private fun launchProjectMilestones(projectId: Int, userId: String) {
        val sharedPref = getSharedPreferences("PDDMATE_PREFS", MODE_PRIVATE)
        val studentSlot = sharedPref.getString("student_slot", "")

        when (studentSlot) {
            "APP_DEVELOPMENT" -> {
                val intent = Intent(this, ProjectMilestoneAppActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("PROJECT_ID", projectId)
                startActivity(intent)
            }
            "PRODUCT_DEVELOPMENT" -> {
                val intent = Intent(this, ProjectMilestoneProductActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("PROJECT_ID", projectId)
                startActivity(intent)
            }
            else -> {
                // Fallback if shared preference is not set
                startActivity(Intent(this, SlotEnrollmentActivity::class.java))
            }
        }
    }

    private fun logout() {
        startActivity(Intent(this, LogoutActivity::class.java))
    }

    private fun updateMonthYearViews() {
        val month = monthNames[selectedCalendar.get(Calendar.MONTH)]
        val year = selectedCalendar.get(Calendar.YEAR).toString()
        binding.monthTextView.text = month
        binding.yearTextView.text = year
    }

    private fun showMonthDropdown() {
        val popup = PopupMenu(this, binding.monthTextView)
        monthNames.forEachIndexed { i, name ->
            popup.menu.add(0, i, i, name)
        }
        popup.setOnMenuItemClickListener { item ->
            selectedCalendar.set(Calendar.MONTH, item.itemId)
            updateMonthYearViews()
            setupCalendarRecyclerView()
            true
        }
        popup.show()
    }

    private fun showYearDropdown() {
        val currentYear = selectedCalendar.get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).toList()
        val popup = PopupMenu(this, binding.yearTextView)
        years.forEach { y -> popup.menu.add(0, y, y, y.toString()) }
        popup.setOnMenuItemClickListener { item ->
            selectedCalendar.set(Calendar.YEAR, item.itemId)
            updateMonthYearViews()
            setupCalendarRecyclerView()
            true
        }
        popup.show()
    }

    private fun setupWeekDaysHeader() {
        binding.weekDaysLinearLayout.removeAllViews()
        for (day in weekDays) {
            val tv = TextView(this).apply {
                text = day
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.black, theme))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            binding.weekDaysLinearLayout.addView(tv)
        }
    }

    private fun setupCalendarRecyclerView() {
        val calendar = selectedCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekCalendar = calendar.get(Calendar.DAY_OF_WEEK)
        val firstDayOfWeek = if (firstDayOfWeekCalendar == Calendar.SUNDAY) 7 else firstDayOfWeekCalendar - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()
        val items = mutableListOf<CalendarCell>()
        for (i in 1 until firstDayOfWeek) {
            items.add(CalendarCell("", false, false))
        }
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val isCurrentDate =
                day == today.get(Calendar.DAY_OF_MONTH) &&
                        selectedCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        selectedCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
            items.add(CalendarCell(day.toString(), true, isCurrentDate))
        }
        val adapter = CalendarAdapter(items)
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        binding.calendarRecyclerView.adapter = adapter
    }
}