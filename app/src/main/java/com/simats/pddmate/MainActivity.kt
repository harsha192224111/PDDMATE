package com.simats.pddmate

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is your splash screen's layout file
        setContentView(R.layout.activity_main)

        // Delay of 2 seconds (2000 ms) before navigating to the next activity.
        Handler(Looper.getMainLooper()).postDelayed({
            // Navigate to the SubscriptionActivity after the delay
            val intent = Intent(this, SubscriptionActivity::class.java)
            startActivity(intent)

            // Finish MainActivity so the user cannot return to the splash screen
            // by pressing the back button.
            finish()
        }, 2000)
    }
}