package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnCreateProfile = findViewById<Button>(R.id.btnCreateProfile)


        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 Step 1: Check Admin Collection First
            db.collection("admins")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        val adminDoc = snapshot.documents[0]
                        val storedPassword = adminDoc.getString("password")

                        if (storedPassword == password ) {
                            Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show()

                            // Save admin email in session
                            val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                            sharedPrefs.edit().putString("userEmail", email).apply()

                            // Go to Admin Dashboard
                            val intent = Intent(this, adashboard::class.java)
                            startActivity(intent)
                            finish()
                            return@addOnSuccessListener
                        } else {
                            Toast.makeText(this, "Incorrect password for admin.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                    }

                    // 🔹 Step 2: Not admin, check Users Collection
                    checkRegularUser(email, password)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // ➕ Go to Create Profile
        btnCreateProfile.setOnClickListener {
            val intent = Intent(this, signin::class.java)
            startActivity(intent)
        }

    }

    private fun checkRegularUser(email: String, password: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val userDoc = snapshot.documents[0]
                    val storedPassword = userDoc.getString("password")
                    val name = userDoc.getString("firstname")

                    if (storedPassword == password) {
                        Toast.makeText(this, "Welcome, $name!", Toast.LENGTH_SHORT).show()

                        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                        sharedPrefs.edit().putString("userEmail", email).apply()

                        val intent = Intent(this, profile::class.java)
                        intent.putExtra("userEmail", email)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User not found. Please create a profile.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
