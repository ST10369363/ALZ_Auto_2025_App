package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        // ✅ Navigation buttons
        findViewById<Button>(R.id.OneBooking).setOnClickListener {
            val intent = Intent(this, booking::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.OneTrack).setOnClickListener {
            val intent = Intent(this, tracking::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.OneInvoice).setOnClickListener {
            val intent = Intent(this, invoice::class.java)
           startActivity(intent)
        }

        findViewById<Button>(R.id.OneProfile).setOnClickListener {
            val intent = Intent(this, profile::class.java)
           startActivity(intent)
        }



    }
}
