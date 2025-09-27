package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pddmate.databinding.ActivityLoginBinding
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

// Login response data class -- use userId (not regNo) for universal role support
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    @SerializedName("user_id") val userId: String? = null, // userId to persist
    val role: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null
)

// Retrofit API service
interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("login.php")
    fun login(@Body body: Map<String, String>): Call<LoginResponse>
}

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var api: ApiService
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Session check: if within 5 days and logged in, skip login
        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val loginTime = prefs.getLong("loginTime", 0L)
        val userRole = prefs.getString("role", null)
        val fiveDaysMillis = 5 * 24 * 60 * 60 * 1000L

        if (isLoggedIn && System.currentTimeMillis() - loginTime < fiveDaysMillis) {
            when (userRole) {
                "Student" -> startActivity(Intent(this, StudentHomePageActivity::class.java))
                "Developer" -> startActivity(Intent(this, DeveloperHomePageActivity::class.java))
                "Supervisor" -> startActivity(Intent(this, SupervisorHomePageActivity::class.java))
                else -> {}
            }
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        api = Retrofit.Builder()
            .baseUrl("http://192.168.31.109/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.passwordEditText.right - binding.passwordEditText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginUser(email, password)
        }

        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, VerificationActivity::class.java))
        }

        binding.signupLink.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_show_password), null
            )
        } else {
            binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_hide_password), null
            )
        }
        binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun loginUser(email: String, password: String) {
        val body = mapOf("email" to email, "password" to password)
        api.login(body).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val resp = response.body()
                    if (resp?.success == true) {
                        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("isLoggedIn", true)
                            .putLong("loginTime", System.currentTimeMillis())
                            .putString("role", resp.role)
                            .putString("user_id", resp.userId ?: "") // <-- corrected key
                            .apply()

                        when (resp.role) {
                            "Student" -> {
                                val intent = Intent(this@LoginActivity, StudentHomePageActivity::class.java).apply {
                                    putExtra("id", resp.id)
                                    putExtra("name", resp.name)
                                    putExtra("email", resp.email)
                                    putExtra("userId", resp.userId)
                                    putExtra("phoneNumber", resp.phoneNumber)
                                }
                                startActivity(intent)
                                finish()
                            }
                            "Developer" -> {
                                startActivity(Intent(this@LoginActivity, DeveloperHomePageActivity::class.java))
                                finish()
                            }
                            "Supervisor" -> {
                                startActivity(Intent(this@LoginActivity, SupervisorHomePageActivity::class.java))
                                finish()
                            }
                            else -> redirectToInvalidCredentials()
                        }
                    } else {
                        redirectToInvalidCredentials()
                    }
                } else {
                    redirectToInvalidCredentials()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Unable to connect to server: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                redirectToInvalidCredentials()
            }
        })
    }

    private fun redirectToInvalidCredentials() {
        startActivity(Intent(this, InvalidCredentialsActivity::class.java))
        finish()
    }

    companion object {
        fun performLogout(activity: AppCompatActivity) {
            val prefs = activity.getSharedPreferences("login_session", MODE_PRIVATE)
            prefs.edit().clear().apply()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            activity.finish()
        }
    }
}