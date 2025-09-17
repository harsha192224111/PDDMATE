package com.example.pddmate

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.pddmate.databinding.ActivityCreateAccountBinding
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class SignupResponse(val success: Boolean, val message: String)

interface ApiSignup {
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

        binding.roleSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                // Role-specific UI logic if any.
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        setPasswordFieldInitialState()

        binding.passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (binding.passwordEditText.right - (binding.passwordEditText.compoundDrawables[2]?.bounds?.width()
                    ?: 0) - binding.passwordEditText.paddingEnd)) {
                togglePasswordVisibility()
                return@setOnTouchListener true
            }
            false
        }

        binding.confirmPasswordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (binding.confirmPasswordEditText.right - (binding.confirmPasswordEditText.compoundDrawables[1]?.bounds?.width()
                    ?: 0) - binding.confirmPasswordEditText.paddingEnd)) {
                toggleConfirmPasswordVisibility()
                return@setOnTouchListener true
            }
            false
        }

        val gson = GsonBuilder().setLenient().create()
        api = Retrofit.Builder()
            .baseUrl("http://10.213.74.64/pdd_dashboard/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiSignup::class.java)

        binding.signUpButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val phoneNumber = binding.phoneNumberEditText.text.toString().trim()
            val userId = binding.regNoEditText.text.toString().trim() // renamed - regNoEditText actually takes user_id
            val role = binding.roleSpinner.selectedItem.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (name.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() ||
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
                "phone_number" to phoneNumber,
                "user_id" to userId, // changed key from reg_no to user_id
                "role" to role,
                "password" to password,
                "confirm_password" to confirmPassword
            )

            binding.signUpButton.isEnabled = false

            api.signup(body).enqueue(object : Callback<SignupResponse> {
                override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                    binding.signUpButton.isEnabled = true
                    if (response.isSuccessful) {
                        val resp = response.body()
                        if (resp?.success == true) {
                            Toast.makeText(this@CreateAccountActivity, "Signup successful! Please login.", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@CreateAccountActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@CreateAccountActivity, resp?.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@CreateAccountActivity, "Server error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                    binding.signUpButton.isEnabled = true
                    Toast.makeText(this@CreateAccountActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
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
        binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, R.drawable.ic_hide_password), null)

        binding.confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        binding.confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, R.drawable.ic_hide_password), null)
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, R.drawable.ic_hide_password), null)
        } else {
            binding.passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passwordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, R.drawable.ic_show_password), null)
        }
        binding.passwordEditText.setSelection(binding.passwordEditText.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            binding.confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, R.drawable.ic_hide_password), null)
        } else {
            binding.confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.confirmPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, ContextCompat.getDrawable(this, R.drawable.ic_show_password), null)
        }
        binding.confirmPasswordEditText.setSelection(binding.confirmPasswordEditText.text.length)
        isConfirmPasswordVisible = !isConfirmPasswordVisible
    }
}
