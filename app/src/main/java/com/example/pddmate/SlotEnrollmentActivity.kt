package com.example.pddmate

import android.os.Bundle
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pddmate.databinding.ActivitySlotEnrollmentBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.google.gson.GsonBuilder

class SlotEnrollmentActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlotEnrollmentBinding
    private lateinit var adapter: EnrollmentAdapter
    private val slots = mutableListOf<EnrollmentSlot>()

    private lateinit var currentStudentUserId: String
    private var alreadyEnrolled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlotEnrollmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
        currentStudentUserId = prefs.getString("user_id", "") ?: ""

        binding.backArrow.setOnClickListener { finish() }

        adapter = EnrollmentAdapter(
            slots,
            onEnrollClick = { slot ->
                if (alreadyEnrolled) {
                    Toast.makeText(this, "You can only enroll for one slot.", Toast.LENGTH_SHORT).show()
                } else {
                    showConfirmDialog(slot)
                }
            }
        )
        binding.recyclerViewEnrollments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewEnrollments.adapter = adapter

        loadSlotsForEnrollment()
    }

    private fun loadSlotsForEnrollment() {
        // Use GsonBuilder to allow lenient JSON parsing
        val gson = GsonBuilder().setLenient().create()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()

        val url = "http://10.249.231.64/pdd_dashboard/get_enrollments.php?student_user_id=$currentStudentUserId"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SlotEnrollmentActivity, "Failed to load slots: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string() ?: return

                    // Attempt to parse JSON response string
                    try {
                        val json = JSONObject(responseBody)
                        if (json.optBoolean("success")) {
                            val data = json.optJSONArray("data") ?: return
                            val newList = mutableListOf<EnrollmentSlot>()
                            alreadyEnrolled = false
                            for (i in 0 until data.length()) {
                                val obj = data.getJSONObject(i)
                                val status = obj.optString("status", "")
                                if (
                                    status.equals("Pending", ignoreCase = true) ||
                                    status.equals("Approved", ignoreCase = true) ||
                                    status.equals("Accepted", ignoreCase = true)
                                ) {
                                    alreadyEnrolled = true
                                }
                                newList.add(
                                    EnrollmentSlot(
                                        projectId = obj.optInt("project_id"),
                                        courseCode = obj.optString("course_code"),
                                        title = obj.optString("title"),
                                        type = obj.optString("type"),
                                        capacity = obj.optInt("capacity"),
                                        slotsLeft = obj.optInt("slots_left"),
                                        mentorName = obj.optString("mentor_name"),
                                        status = status
                                    )
                                )
                            }
                            runOnUiThread {
                                slots.clear()
                                slots.addAll(newList)
                                adapter.setSingleEnrollment(alreadyEnrolled)
                                adapter.notifyDataSetChanged()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@SlotEnrollmentActivity, json.optString("message"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@SlotEnrollmentActivity, "Error reading slot data: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("SlotEnrollment", "JSON Parsing Error: ${e.message}. Response: $responseBody")
                        }
                    }
                }
            }
        })
    }

    private fun showConfirmDialog(slot: EnrollmentSlot) {
        val dialogView = layoutInflater.inflate(R.layout.activity_confirm_enrollment_popup, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialogView.findViewById<android.widget.Button>(R.id.btn_confirm).setOnClickListener {
            requestEnrollment(slot)
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.ImageView>(R.id.close_icon).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun requestEnrollment(slot: EnrollmentSlot) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("student_user_id", currentStudentUserId)
        jsonObject.put("project_id", slot.projectId)
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("http://10.249.231.64/pdd_dashboard/enroll_project.php")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SlotEnrollmentActivity, "Request failed", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val responseBody = res.body?.string() ?: return
                    val json = JSONObject(responseBody)
                    runOnUiThread {
                        Toast.makeText(this@SlotEnrollmentActivity, json.optString("message"), Toast.LENGTH_SHORT).show()
                        if (json.optBoolean("success")) {
                            slot.status = "Pending"
                            adapter.notifyDataSetChanged()
                            alreadyEnrolled = true
                            adapter.setSingleEnrollment(true)
                            val sharedPref = getSharedPreferences("PDDMATE_PREFS", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                if (slot.type.equals("App", ignoreCase = true)) {
                                    putString("student_slot", "APP_DEVELOPMENT")
                                } else if (slot.type.equals("Product", ignoreCase = true)) {
                                    putString("student_slot", "PRODUCT_DEVELOPMENT")
                                }
                                commit()
                            }
                        }
                    }
                }
            }
        })
    }
}
