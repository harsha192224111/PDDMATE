package com.example.pddmate

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pddmate.databinding.ActivitySlotDetailsBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class SlotDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlotDetailsBinding
    private var projectId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlotDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        projectId = intent.getIntExtra("project_id", 0)
        binding.backArrow.setOnClickListener { finish() }

        loadSlotDetails()
        loadStudents()
        loadStudentRequests()
    }

    private fun loadSlotDetails() {
        val client = OkHttpClient()
        val reqJson = JSONObject()
        reqJson.put("project_id", projectId)
        val requestBody = reqJson.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("http://10.213.74.64/pdd_dashboard/get_project_details.php")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val json = JSONObject(body)
                    if (json.optBoolean("success")) {
                        val slot = json.optJSONObject("data") ?: return
                        runOnUiThread {
                            binding.slotTitle.text = slot.optString("title")
                            binding.slotTypeText.text = slot.optString("type")
                            val slotsLeftText = getString(R.string.slots_left, slot.optInt("capacity"))
                            binding.slotsLeft.text = slotsLeftText
                            binding.mentorName.text = slot.optString("developer_name")
                        }
                    }
                }
            }
        })
    }

    private fun loadStudents() {
        val client = OkHttpClient()
        val url = "http://10.213.74.64/pdd_dashboard/get_project_students.php?project_id=$projectId"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                if (json.optBoolean("success")) {
                    val arr: JSONArray = json.optJSONArray("data") ?: JSONArray()
                    runOnUiThread {
                        binding.studentList.removeAllViews()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val view = LayoutInflater.from(this@SlotDetailsActivity)
                                .inflate(R.layout.student_card, binding.studentList, false)
                            view.findViewById<TextView>(R.id.studentName).text = obj.optString("name")
                            view.findViewById<TextView>(R.id.studentUserId).text = obj.optString("user_id")
                            binding.studentList.addView(view)
                        }
                    }
                }
            }
        })
    }

    private fun loadStudentRequests() {
        val client = OkHttpClient()
        val url = "http://10.213.74.64/pdd_dashboard/get_project_requests.php?project_id=$projectId"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)
                if (json.optBoolean("success")) {
                    val arr: JSONArray = json.optJSONArray("data") ?: JSONArray()
                    runOnUiThread {
                        binding.requestList.removeAllViews()
                        for (i in 0 until arr.length()) {
                            val reqObj = arr.getJSONObject(i)
                            val view = LayoutInflater.from(this@SlotDetailsActivity)
                                .inflate(R.layout.student_request_card, binding.requestList, false)
                            view.findViewById<TextView>(R.id.studentNameReq).text = reqObj.optString("name")
                            view.findViewById<TextView>(R.id.studentEmailReq).text = reqObj.optString("user_id")
                            view.findViewById<ImageView>(R.id.icAccept).setOnClickListener {
                                processRequest(reqObj.optInt("enrollment_id"), "approve")
                            }
                            view.findViewById<ImageView>(R.id.icReject).setOnClickListener {
                                processRequest(reqObj.optInt("enrollment_id"), "reject")
                            }
                            binding.requestList.addView(view)
                        }
                    }
                }
            }
        })
    }

    private fun processRequest(enrollmentId: Int, action: String) {
        val developerUserId = getSharedPreferences("login_session", MODE_PRIVATE)
            .getString("user_id", "") ?: ""
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("developer_user_id", developerUserId)
            .add("enrollment_id", enrollmentId.toString())
            .add("action", action)
            .build()
        val request = Request.Builder()
            .url("http://10.213.74.64/pdd_dashboard/approve_enrollment.php")
            .post(formBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@SlotDetailsActivity, getString(R.string.action_failed), Toast.LENGTH_SHORT).show() }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val json = JSONObject(body ?: "{}")
                runOnUiThread {
                    Toast.makeText(this@SlotDetailsActivity, json.optString("message"), Toast.LENGTH_SHORT).show()
                    loadStudents()
                    loadStudentRequests()
                }
            }
        })
    }
}