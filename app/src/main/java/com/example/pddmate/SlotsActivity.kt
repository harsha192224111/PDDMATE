package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pddmate.databinding.ActivitySlotsBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SlotsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySlotsBinding
    private lateinit var adapter: SlotsAdapter
    private val slotsList = mutableListOf<ProjectSlot>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySlotsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SlotsAdapter(slotsList) { slot ->
            val intent = Intent(this, SlotDetailsActivity::class.java).apply {
                putExtra("project_id", slot.projectId)
                putExtra("course_code", slot.courseCode)
                putExtra("title", slot.title)
                putExtra("type", slot.type)
                putExtra("capacity", slot.capacity)
                putExtra("start_date", slot.startDate?.time)
                putExtra("end_date", slot.endDate?.time)
                putExtra("developer_name", slot.developerName)
            }
            startActivity(intent)
        }
        binding.recyclerViewSlots.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSlots.adapter = adapter

        binding.fab.setOnClickListener {
            startActivity(Intent(this, CreateSlotActivity::class.java))
        }
        binding.backArrow.setOnClickListener { finish() }

        loadActiveSlots()
    }

    override fun onResume() {
        super.onResume()
        loadActiveSlots()
    }

    private fun getDeveloperUserId(): String {
        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    private fun loadActiveSlots() {
        val developerUserId = getDeveloperUserId()
        if (developerUserId.isEmpty()) {
            Log.e("SlotsActivity", "Developer user ID not found in SharedPreferences!")
            return
        }

        slotsList.clear()
        val client = OkHttpClient()
        val jsonBody = JSONObject()
        jsonBody.put("developer_user_id", developerUserId)

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("http://192.168.31.109/pdd_dashboard/get_slots.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SlotsActivity", "Fetch failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                try {
                    if (!bodyString.isNullOrEmpty()) {
                        val json = JSONObject(bodyString)
                        val success = json.optBoolean("success", false)
                        if (success) {
                            val dataArray = json.optJSONArray("data") ?: JSONArray()
                            for (i in 0 until dataArray.length()) {
                                val obj = dataArray.getJSONObject(i)
                                val startDate = obj.optString("start_date")
                                    .takeIf { it.isNotBlank() }
                                    ?.let { dateFormat.parse(it) }
                                val endDate = obj.optString("end_date")
                                    .takeIf { it.isNotBlank() }
                                    ?.let { dateFormat.parse(it) }
                                val slot = ProjectSlot(
                                    projectId = obj.optInt("project_id"),
                                    courseCode = obj.optString("course_code"),
                                    title = obj.optString("title"),
                                    type = obj.optString("type"),
                                    capacity = obj.optInt("capacity"),
                                    startDate = startDate,
                                    endDate = endDate,
                                    developerName = obj.optString("developer_name")
                                )
                                slotsList.add(slot)
                            }
                            runOnUiThread { adapter.notifyDataSetChanged() }
                        } else {
                            Log.e("SlotsActivity", "API success=false or no data returned")
                        }
                    } else {
                        Log.e("SlotsActivity", "Empty response from server")
                    }
                } catch (e: Exception) {
                    Log.e("SlotsActivity", "JSON parsing failed", e)
                }
            }
        })
    }
}