package com.example.pddmate

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class ProfileActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var registrationEditText: EditText
    private lateinit var roleEditText: EditText

    interface ApiService {
        @FormUrlEncoded
        @POST("get_profile.php")
        fun getProfile(@Field("user_id") userId: String): Call<ProfileResponse>
    }

    data class ProfileResponse(
        val success: Boolean,
        val message: String,
        val profile: UserProfile?
    )

    data class UserProfile(
        val name: String,
        val email: String,
        @SerializedName("user_id") val userId: String,
        val role: String,
        @SerializedName("phone_number") val phoneNumber: String?,
        @SerializedName("registration_number") val registrationNumber: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        fullNameEditText = findViewById(R.id.fullNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        registrationEditText = findViewById(R.id.registrationEditText)
        roleEditText = findViewById(R.id.roleEditText)

        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)

        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        fetchUserProfile(userId)
    }

    private fun fetchUserProfile(userId: String) {
        val gson = GsonBuilder().setLenient().create()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.213.74.64/pdd_dashboard/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.getProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(
                call: Call<ProfileResponse>,
                response: Response<ProfileResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        val profile = body.profile
                        if (profile != null) {
                            fullNameEditText.setText(profile.name)
                            emailEditText.setText(profile.email)
                            phoneEditText.setText(profile.phoneNumber ?: "")
                            registrationEditText.setText(profile.registrationNumber ?: "")
                            roleEditText.setText(profile.role)
                        } else {
                            Toast.makeText(this@ProfileActivity, "Profile not found", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Try to log error body to understand failure
                        val errorBodyStr = response.errorBody()?.string()
                        Log.e("ProfileActivity", "API response unsuccessful: $errorBodyStr")
                        Toast.makeText(this@ProfileActivity, body?.message ?: "Failed to load profile", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    Log.e("ProfileActivity", "Error response: $errorBodyStr")
                    Toast.makeText(this@ProfileActivity, "Server error: $errorBodyStr", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Log.e("ProfileActivity", "Network request failed", t)
                Toast.makeText(this@ProfileActivity, "Network error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
