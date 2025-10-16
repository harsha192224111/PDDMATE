package com.simats.pddmate

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simats.pddmate.databinding.ActivityVerificationBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class VerificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerificationBinding
    private var generatedOtp: String? = null
    private var email: String = ""
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener { finish() }

        binding.getOtpButton.setOnClickListener {
            email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendOtpRequest(email)
        }

        binding.verifyButton.setOnClickListener {
            val enteredOtp = binding.otpEditText.text.toString().trim()
            if (enteredOtp.isEmpty()) {
                Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (generatedOtp == null) {
                Toast.makeText(this, "Please request OTP first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (enteredOtp == generatedOtp) {
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                fetchUserIdThenNavigate(email)
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtpRequest(email: String) {
        val client = OkHttpClient()
        val json = JSONObject().apply { put("email", email) }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://14.139.187.229:8081/pddmate/forgot_password.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@VerificationActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string()
                if (!response.isSuccessful || resStr.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@VerificationActivity, "Server error", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                try {
                    val jsonRes = JSONObject(resStr)
                    val success = jsonRes.getBoolean("success")
                    val message = jsonRes.getString("message")
                    if (success) {
                        if (jsonRes.has("otp")) {
                            generatedOtp = jsonRes.getString("otp")
                            userId = jsonRes.optString("user_id", null)
                            runOnUiThread {
                                Toast.makeText(this@VerificationActivity, "OTP sent! OTP: $generatedOtp", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@VerificationActivity, "OTP sent! Please check inbox.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@VerificationActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@VerificationActivity, "Parsing error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchUserIdThenNavigate(email: String) {
        if (userId != null) {
            val intent = Intent(this@VerificationActivity, ResetPasswordActivity::class.java)
            intent.putExtra("email", email)
            intent.putExtra("user_id", userId)
            startActivity(intent)
            finish()
            return
        }

        val client = OkHttpClient()
        val json = JSONObject().apply { put("email", email) }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://14.139.187.229:8081/pddmate/get_user_id.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    val intent = Intent(this@VerificationActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string()
                runOnUiThread {
                    try {
                        if (response.isSuccessful && !resStr.isNullOrEmpty()) {
                            val jsonRes = JSONObject(resStr)
                            userId = jsonRes.optString("user_id", null)
                            val intent = Intent(this@VerificationActivity, ResetPasswordActivity::class.java)
                            intent.putExtra("email", email)
                            intent.putExtra("user_id", userId)
                            startActivity(intent)
                            finish()
                        } else {
                            val intent = Intent(this@VerificationActivity, ResetPasswordActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        val intent = Intent(this@VerificationActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })
    }
}