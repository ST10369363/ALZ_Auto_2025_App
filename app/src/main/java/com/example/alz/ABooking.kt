package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ABooking : AppCompatActivity() {

    private lateinit var spinnerBookings: Spinner
    private lateinit var tvBookingDetails: TextView
    private lateinit var btnAccept: Button
    private lateinit var btnReject: Button
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var statusActionsLayout: LinearLayout
    // Assuming you have a Dashboard button in your layout with this ID
    private lateinit var btnDashboard: Button

    private val db = FirebaseFirestore.getInstance()
    private val bookingIds = mutableListOf<String>()
    private var selectedBookingId: String? = null
    private var bookingsListener: ListenerRegistration? = null


    companion object {
        private const val TAG = "ABooking"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_abooking)

        spinnerBookings = findViewById(R.id.spinnerBookings)
        tvBookingDetails = findViewById(R.id.tvBookingDetails)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
        progressLoading = findViewById(R.id.progressLoading)
        tvError = findViewById(R.id.tvError)
        statusActionsLayout = findViewById(R.id.statusActionsLayout)
        // 1. Initialize the Dashboard button (using the assumed ID: ADashboard)
        btnDashboard = findViewById(R.id.ABookingDashboard)

        setLoading(true)
        listenForBookings()

        spinnerBookings.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (position < bookingIds.size) {
                    selectedBookingId = bookingIds[position]
                    loadBookingDetails(selectedBookingId!!)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAccept.setOnClickListener { confirmAction("accepted") }
        btnReject.setOnClickListener { confirmAction("rejected") }

        // 2. FIX: Move the click listener inside onCreate
        btnDashboard.setOnClickListener {
            val intent = Intent(this, adashboard::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bookingsListener?.remove()
    }

    private fun setLoading(loading: Boolean) {
        progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        spinnerBookings.isEnabled = !loading
        btnAccept.isEnabled = !loading
        btnReject.isEnabled = !loading
        statusActionsLayout.visibility = if (loading) View.GONE else View.VISIBLE
        if (loading) tvError.visibility = View.GONE
    }

    private fun listenForBookings() {
        // Listen for all bookings regardless of status
        bookingsListener = db.collection("bookings")
            .addSnapshotListener { snapshot, error ->
                setLoading(false)
                if (error != null) {
                    val msg = "Error loading bookings: ${error.message}"
                    Log.e(TAG, msg, error)
                    tvError.text = msg
                    tvError.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    tvBookingDetails.text = "No bookings found."
                    bookingIds.clear()
                    spinnerBookings.adapter = null
                    return@addSnapshotListener
                }

                bookingIds.clear()
                for (doc in snapshot.documents) {
                    bookingIds.add(doc.id)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    bookingIds
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerBookings.adapter = adapter
            }
    }

    private fun loadBookingDetails(bookingId: String) {
        setLoading(true)
        db.collection("bookings").document(bookingId).get()
            .addOnSuccessListener { doc ->
                setLoading(false)
                if (doc.exists()) {
                    val status = doc.getString("status") ?: "—"
                    val details = """
                        Booking ID: ${doc.id}
                        User: ${doc.getString("userName") ?: "—"}
                        Email: ${doc.getString("userEmail") ?: "—"}
                        Phone: ${doc.getString("userPhone") ?: "—"}
                        Vehicle: ${doc.getString("vehicleMake") ?: "—"} ${doc.getString("vehicleModel") ?: ""}
                        Service Type: ${doc.getString("serviceType") ?: "—"}
                        Preferred Date: ${doc.getString("preferredDateString") ?: "—"}
                        Mileage: ${doc.getLong("mileage") ?: "—"}
                        Status: $status
                    """.trimIndent()
                    tvBookingDetails.text = details

                    // Accept/Reject buttons only enabled for pending
                    val pending = status == "pending"
                    btnAccept.isEnabled = pending
                    btnReject.isEnabled = pending

                    // Always show all four status options for progression
                    showStatusActions()

                } else {
                    tvBookingDetails.text = "Booking not found."
                    statusActionsLayout.removeAllViews()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                val msg = "Failed to load booking details: ${e.message}"
                Log.e(TAG, msg, e)
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            }
    }

    private fun confirmAction(status: String) {
        val actionText = if (status == "accepted") "accept" else "reject"
        AlertDialog.Builder(this)
            .setTitle("Confirm $actionText")
            .setMessage("Are you sure you want to $actionText this booking?")
            .setPositiveButton("Yes") { _, _ -> updateBookingStatus(status) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBookingStatus(status: String) {
        val id = selectedBookingId ?: return
        setLoading(true)
        db.collection("bookings").document(id)
            .update("status", status)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "Booking marked as $status", Toast.LENGTH_SHORT).show()
                loadBookingDetails(id)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                val msg = "Failed to update status: ${e.message}"
                Log.e(TAG, msg, e)
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            }
    }

    private fun showStatusActions() {
        statusActionsLayout.removeAllViews()

        val stages = listOf("accepted", "received", "working in progress", "complete repair")

        for (stage in stages) {
            val button = Button(this).apply {
                text = stage.replaceFirstChar { it.uppercase() }
                setOnClickListener {
                    updateBookingStatus(stage)
                }
            }
            statusActionsLayout.addView(button)
        }
    }
}