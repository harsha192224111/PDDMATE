package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var showNewPassword: ImageView
    private lateinit var showConfirmPassword: ImageView
    private lateinit var resetButton: Button

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        newPassword = findViewById(R.id.new_password)
        confirmPassword = findViewById(R.id.confirm_password)
        showNewPassword = findViewById(R.id.show_new_password)
        showConfirmPassword = findViewById(R.id.show_confirm_password)
        resetButton = findViewById(R.id.reset_password_button)

        userId = intent.getStringExtra("user_id")

        showNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            newPassword.inputType = if (isNewPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            newPassword.setSelection(newPassword.text.length)
        }

        showConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            confirmPassword.inputType = if (isConfirmPasswordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            confirmPassword.setSelection(confirmPassword.text.length)
        }

        resetButton.setOnClickListener {
            val newPwd = newPassword.text.toString()
            val confPwd = confirmPassword.text.toString()

            if (newPwd.isEmpty() || confPwd.isEmpty()) {
                Toast.makeText(this, "Both password fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPwd != confPwd) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPwd.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!newPwd.matches(Regex(".*[A-Za-z].*")) || !newPwd.matches(Regex(".*[0-9].*"))) {
                Toast.makeText(this, "Password must contain both letters and numbers.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resetPasswordOnServer(userId, newPwd, confPwd)
        }
    }

    private fun resetPasswordOnServer(userId: String?, newPwd: String, confPwd: String) {
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User identification missing!", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("user_id", userId)
            put("new_password", newPwd)
            put("confirm_password", confPwd)
        }
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.31.109/pdd_dashboard/reset_password.php")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ResetPasswordActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string()
                runOnUiThread {
                    if (!response.isSuccessful || resStr.isNullOrEmpty()) {
                        Toast.makeText(this@ResetPasswordActivity, "Server error", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    try {
                        val jsonRes = JSONObject(resStr)
                        val success = jsonRes.getBoolean("success")
                        val message = jsonRes.getString("message")
                        Toast.makeText(this@ResetPasswordActivity, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@ResetPasswordActivity, "Parsing error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}