package com.example.alz

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore

class signin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signin)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating account...")
        progressDialog.setCancelable(false)

        val name = findViewById<EditText>(R.id.SignInName)
        val surname = findViewById<EditText>(R.id.SignInSurname)
        val email = findViewById<EditText>(R.id.SignInEmail)
        val phone = findViewById<EditText>(R.id.SignInPhone)
        val password = findViewById<EditText>(R.id.SignInPassword)
        val confirmPassword = findViewById<EditText>(R.id.SignInConfirmPassword)
        val btnSignIn = findViewById<Button>(R.id.BtnSignIn)
        val btnBack = findViewById<Button>(R.id.BtnBack)

        btnSignIn.setOnClickListener {
            val userName = name.text.toString().trim()
            val userSurname = surname.text.toString().trim()
            val userEmail = email.text.toString().trim().lowercase()
            val userPhone = phone.text.toString().trim()
            val userPassword = password.text.toString().trim()
            val userConfirmPassword = confirmPassword.text.toString().trim()

            // === VALIDATION ===
            if (userName.isEmpty() || userSurname.isEmpty() || userEmail.isEmpty() ||
                userPhone.isEmpty() || userPassword.isEmpty() || userConfirmPassword.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordRegex = Regex("^(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$")
            if (!passwordRegex.matches(userPassword)) {
                Toast.makeText(
                    this,
                    "Password must be at least 6 characters and include one number and one special character.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (userPassword != userConfirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneDigits = userPhone.replace("-", "").replace(" ", "")
            if (phoneDigits.length < 7) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog.show()
            btnSignIn.isEnabled = false

            auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val userId = firebaseUser.uid

                        val userMap = hashMapOf(
                            "uid" to userId,
                            "firstName" to userName,
                            "surname" to userSurname,
                            "email" to userEmail,
                            "phone" to userPhone,
                            "password" to userPassword, // ✅ Plain text password (not hashed)
                            "role" to "user",
                            "createdAt" to Timestamp.now()
                        )

                        firestore.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                name.text.clear()
                                surname.text.clear()
                                email.text.clear()
                                phone.text.clear()
                                password.text.clear()
                                confirmPassword.text.clear()

                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progressDialog.dismiss()
                                btnSignIn.isEnabled = true
                                Toast.makeText(
                                    this,
                                    "Failed to save user: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        progressDialog.dismiss()
                        btnSignIn.isEnabled = true
                        Toast.makeText(this, "User creation failed. Try again.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    btnSignIn.isEnabled = true

                    val msg = when (e) {
                        is FirebaseAuthException -> when (e.errorCode) {
                            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use. Try logging in."
                            "ERROR_INVALID_EMAIL" -> "Invalid email address."
                            "ERROR_WEAK_PASSWORD" -> "Weak password. Use at least 6 characters, a number, and a special character."
                            else -> "Authentication failed: ${e.message}"
                        }
                        else -> when {
                            e.message?.contains("network", true) == true -> "Network error. Check your connection."
                            else -> "Failed to create account: ${e.message}"
                        }
                    }

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
        }

        btnBack.setOnClickListener { finish() }
    }
}
