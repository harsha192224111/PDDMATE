package com.simats.pddmate

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.simats.pddmate.databinding.ActivityCreateAccountBinding
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class SignupResponse(val success: Boolean, val message: String)

interface ApiSignup {
    @Headers("Content-Type: application/json")
    @POST("signup.php")
    fun signup(@Body body: Map<String, String>): Call<SignupResponse>
}

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAccountBinding
    private lateinit var api: ApiSignup
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val roles = listOf("Student", "Developer", "Supervisor")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.roleSpinner.adapter = adapter
        binding.roleSpinner.setSelection(0)

        setPasswordFieldInitialState()

        // Toggle visibility icons
        binding.passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (binding.passwordEditText.right -
                        (binding.passwordEditText.compoundDrawables[2]?.bounds?.width() ?: 0) -
                        binding.passwordEditText.paddingEnd)
            ) {
                togglePasswordVisibility()
                return@setOnTouchListener true
            }
            false
        }

        binding.confirmPasswordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (binding.confirmPasswordEditText.right -
                        (binding.confirmPasswordEditText.compoundDrawables[2]?.bounds?.width() ?: 0) -
                        binding.confirmPasswordEditText.paddingEnd)
            ) {
                toggleConfirmPasswordVisibility()
                return@setOnTouchListener true
            }
            false
        }

        // Retrofit setup
        val gson = GsonBuilder().setLenient().create()
        api = Retrofit.Builder()
            .baseUrl("http://14.139.187.229:8081/pddmate/") // ✅ Replace with your backend IP
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiSignup::class.java)

        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val phone = binding.phoneNumberEditText.text.toString().trim()
            val userId = binding.regNoEditText.text.toString().trim()
            val role = binding.roleSpinner.selectedItem.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                userId.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val body = mapOf(
                "name" to name,
                "email" to email,
                "user_id" to userId,
                "role" to role,
                "password" to password,
                "confirm_password" to confirmPassword,
                "phone_number" to phone
            )

            binding.signUpButton.isEnabled = false

            api.signup(body).enqueue(object : Callback<SignupResponse> {
                override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                    binding.signUpButton.isEnabled = true
                    val resp = response.body()
                    if (response.isSuccessful && resp != null) {
                        Toast.makeText(this@CreateAccountActivity, resp.message, Toast.LENGTH_LONG).show()
                        if (resp.success) {
                            startActivity(Intent(this@CreateAccountActivity, LoginActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(this@CreateAccountActivity, "Server returned no data.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                    binding.signUpButton.isEnabled = true
                    Toast.makeText(this@CreateAccountActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            })
        }

        binding.loginHereText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setPasswordFieldInitialState() {
        binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun togglePasswordVisibility() {
        val icon = if (isPasswordVisible) R.drawable.ic_hide_password else R.drawable.ic_show_password
        binding.passwordEditText.inputType = if (isPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
        binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, icon), null)
        isPasswordVisible = !isPasswordVisible
    }

    private fun toggleConfirmPasswordVisibility() {
        val icon = if (isConfirmPasswordVisible) R.drawable.ic_hide_password else R.drawable.ic_show_password
        binding.confirmPasswordEditText.inputType = if (isConfirmPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        binding.confirmPasswordEditText.setSelection(binding.confirmPasswordEditText.text.length)
        binding.confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, icon), null)
        isConfirmPasswordVisible = !isConfirmPasswordVisible
    }
}
