package com.example.pddmate

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pddmate.databinding.ActivityCreateSlotBinding
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateSlotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateSlotBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateSlotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener { finish() }
        binding.projectTypeAppButton.isChecked = true

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            updateDateField()
        }

        binding.dateInputLayout.setEndIconOnClickListener { showDatePicker(dateSetListener) }
        binding.dateEditText.setOnClickListener { showDatePicker(dateSetListener) }

        binding.launchSlotButton.setOnClickListener { launchSlot() }
    }

    private fun showDatePicker(listener: DatePickerDialog.OnDateSetListener) {
        DatePickerDialog(
            this, listener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateField() {
        binding.dateEditText.setText(dateFormat.format(calendar.time))
    }

    private fun getUserId(): String {
        val sharedPref = getSharedPreferences("login_session", MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    private fun launchSlot() {
        val courseCode = binding.courseCodeEditText.text.toString().trim()
        val title = binding.courseTitleEditText.text.toString().trim()
        val maxStudentsText = binding.maxStudentsEditText.text.toString().trim()
        val startDateText = binding.dateEditText.text.toString().trim()

        if (courseCode.isEmpty() || title.isEmpty() || maxStudentsText.isEmpty() || startDateText.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val capacity = maxStudentsText.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "Please enter valid capacity", Toast.LENGTH_SHORT).show()
            return
        }

        val projectType = if (binding.projectTypeProductButton.isChecked) "Product" else "App"

        val startDate = try {
            dateFormat.parse(startDateText)
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid start date", Toast.LENGTH_SHORT).show()
            return
        } ?: return

        val endCalendar = Calendar.getInstance().apply {
            time = startDate
            add(Calendar.MONTH, 5)
        }
        val endDateText = dateFormat.format(endCalendar.time)

        val userId = getUserId()
        if (userId.isEmpty()) {
            Toast.makeText(this, "Developer not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val formBody = FormBody.Builder()
            .add("developer_user_id", userId)
            .add("course_code", courseCode)
            .add("title", title)
            .add("type", projectType)
            .add("capacity", capacity.toString())
            .add("start_date", startDateText)
            .add("end_date", endDateText)
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://10.213.74.64/pdd_dashboard/create_project.php")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CreateSlotActivity, "Network error", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && body?.trim()?.startsWith("{") == true) {
                        try {
                            val json = JSONObject(body)
                            if (json.optBoolean("success")) {
                                Toast.makeText(this@CreateSlotActivity, "Slot launched successfully", Toast.LENGTH_SHORT).show()
                                val intent = Intent().apply {
                                    putExtra("newSlot", json.optJSONObject("slot")?.toString())
                                }
                                setResult(RESULT_OK, intent)
                                finish()
                            } else {
                                Toast.makeText(this@CreateSlotActivity, json.optString("message"), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@CreateSlotActivity, "Invalid response from server", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CreateSlotActivity, "Failed to launch slot", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}