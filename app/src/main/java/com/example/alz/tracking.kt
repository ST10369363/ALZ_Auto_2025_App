package com.example.alz

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class tracking : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var bookingDropdown: AutoCompleteTextView
    private lateinit var resultArea: TextView
    private lateinit var trackButton: MaterialButton
    private lateinit var clearButton: MaterialButton
    private lateinit var sharedPrefs: SharedPreferences

    private val bookingIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        // 🔧 Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // 🔧 Initialize UI elements
        bookingDropdown = findViewById(R.id.tracking_id_edit_text)
        resultArea = findViewById(R.id.result_area)
        trackButton = findViewById(R.id.track_button)
        clearButton = findViewById(R.id.clear_btn)
        sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)

        // 🧠 Retrieve logged-in user's email
        val userEmail = sharedPrefs.getString("userEmail", null)

        if (userEmail == null) {
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 🔥 Fetch bookings that belong to this user
        fetchUserBookings(userEmail)

        // 🟢 Track button logic
        trackButton.setOnClickListener {
            val selectedBookingId = bookingDropdown.text.toString().trim()

            if (selectedBookingId.isEmpty()) {
                Toast.makeText(this, "Please select a booking ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fetchBookingStatus(selectedBookingId)
        }

        // 🧹 Clear button logic
        clearButton.setOnClickListener {
            bookingDropdown.setText("")
            resultArea.text = ""
            resultArea.visibility = TextView.GONE
        }
    }

    // 🔥 Get user's bookings from Firestore
    private fun fetchUserBookings(email: String) {
        db.collection("bookings")
            .whereEqualTo("userEmail", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "No bookings found for $email", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                bookingIds.clear()
                for (document in querySnapshot.documents) {
                    bookingIds.add(document.id)
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bookingIds)
                bookingDropdown.setAdapter(adapter)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading bookings: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 🔍 Fetch and display the status of a specific booking
    private fun fetchBookingStatus(bookingId: String) {
        db.collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val serviceType = document.getString("serviceType") ?: "N/A"
                    val status = document.getString("status") ?: "N/A"
                    val preferredDate = document.getString("preferredDate") ?: "N/A"
                    val vehicleMake = document.getString("vehicleMake") ?: "N/A"
                    val vehicleModel = document.getString("vehicleModel") ?: "N/A"
                    val mileage = document.getLong("mileage")?.toString() ?: "N/A"

                    val displayText = """
                        🚗 Booking ID: $bookingId
                        🧰 Service Type: $serviceType
                        📅 Preferred Date: $preferredDate
                        🏎️ Vehicle: $vehicleMake $vehicleModel
                        🔢 Mileage: $mileage km
                        📊 Status: $status
                    """.trimIndent()

                    resultArea.text = displayText
                    resultArea.visibility = TextView.VISIBLE
                } else {
                    Toast.makeText(this, "Booking not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving booking: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
