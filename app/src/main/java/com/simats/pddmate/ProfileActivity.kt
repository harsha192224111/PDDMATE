package com.simats.pddmate

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simats.pddmate.databinding.ActivityProfileBinding
import com.google.gson.annotations.SerializedName
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// --- Data Models ---
data class ProfileResponse(
    val success: Boolean,
    val message: String,
    val profile: ProfileData?
)

data class ProfileData(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("user_id") val userId: String,
    val role: String,
    @SerializedName("phone_number") val phoneNumber: String?
)

// --- Retrofit API Interface ---
interface ProfileApiService {
    @FormUrlEncoded
    @POST("get_profile.php")
    fun fetchUserProfile(@Field("user_id") userId: String): Call<ProfileResponse>
}

// --- Profile Activity ---
class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var api: ProfileApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrofit initialization
        api = Retrofit.Builder()
            .baseUrl("http://14.139.187.229:8081/pddmate/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProfileApiService::class.java)

        // Get stored user_id from SharedPreferences
        val prefs = getSharedPreferences("login_session", MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)
        if (userId != null) {
            loadProfile(userId)
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfile(userId: String) {
        api.fetchUserProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.success) {
                    val profile = body.profile
                    if (profile != null) {
                        binding.fullNameEditText.setText(profile.name)
                        binding.emailEditText.setText(profile.email)
                        binding.phoneEditText.setText(profile.phoneNumber ?: "")
                        binding.registrationEditText.setText(profile.userId)
                        binding.roleEditText.setText(profile.role)
                    } else {
                        Toast.makeText(this@ProfileActivity, "Profile data missing", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, body?.message ?: "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
