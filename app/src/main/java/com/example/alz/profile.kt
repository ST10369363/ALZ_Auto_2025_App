package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class profile : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()

        // --- UI Components ---
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvSurname = findViewById<TextView>(R.id.tvSurname)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // ✅ If your XML layout includes a Dashboard button, link it here:
        // (Otherwise, remove these two lines)
        val btnDashboard = findViewById<Button?>(R.id.btnDashboard)

        // ✅ Try to get user email from Intent or SharedPreferences
        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val userEmail = intent.getStringExtra("userEmail")
            ?: sharedPrefs.getString("userEmail", null)

        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "No user session found. Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // ✅ Fetch user details from Firestore
        db.collection("users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val userDoc = querySnapshot.documents[0]

                    val firstName = userDoc.getString("firstName") ?: "Unknown"
                    val surname = userDoc.getString("surname") ?: "Unknown"
                    val email = userDoc.getString("email") ?: userEmail

                    tvName.text = "Name: $firstName"
                    tvSurname.text = "Surname: $surname"
                    tvEmail.text = "Email: $email"

                    // ✅ Save user info locally (for future access)
                    with(sharedPrefs.edit()) {
                        putString("firstName", firstName)
                        putString("surname", surname)
                        putString("userEmail", email)
                        apply()
                    }

                } else {
                    Toast.makeText(this, "User not found in Firestore.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // 🚪 Logout button clears SharedPreferences
        btnLogout.setOnClickListener {
            sharedPrefs.edit().clear().apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // 🏠 Go back to dashboard (if button exists)
        btnDashboard?.setOnClickListener {
            val intent = Intent(this, dashboard::class.java)
            startActivity(intent)
        }
    }
}
