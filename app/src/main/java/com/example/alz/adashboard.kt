package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class adashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_adashboard)

        // ✅ Navigation buttons
        findViewById<Button>(R.id.OneABooking).setOnClickListener {
            val intent = Intent(this, ABooking::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.OneAInvoices).setOnClickListener {
            val intent = Intent(this, AInvoice::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.OneACreate).setOnClickListener {
            val intent = Intent(this, AUserCreate::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.ALogout).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


  }


}