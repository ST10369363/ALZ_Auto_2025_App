package com.example.alz

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class booking : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_booking)

        firestore = FirebaseFirestore.getInstance()

        // UI elements
        val inputName = findViewById<EditText>(R.id.inputName)
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPhone = findViewById<EditText>(R.id.inputPhone)
        val inputDate = findViewById<EditText>(R.id.inputDate)
        val spinnerServices = findViewById<Spinner>(R.id.spinnerservices)
        val inputOtherService = findViewById<EditText>(R.id.inputOtherService)
        val inputMake = findViewById<EditText>(R.id.inputMake)
        val inputModel = findViewById<EditText>(R.id.inputModel)
        val inputMileage = findViewById<EditText>(R.id.inputMileage)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitBooking)

        // Dashboard Button
        findViewById<Button>(R.id.BookingDashboard).setOnClickListener {
            startActivity(Intent(this, dashboard::class.java))
        }

        // Services list
        val services = listOf(
            "Minor Service",
            "Major Service",
            "Vehicle Inspection",
            "Mechanical Repair",
            "Electrical Repair",
            "Other"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, services)
        spinnerServices.adapter = adapter

        spinnerServices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                inputOtherService.visibility =
                    if (services[position] == "Other") android.view.View.VISIBLE else android.view.View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 📅 Date picker setup — prevents selecting past dates
        inputDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, day)

                    // Check if selected date is today or in the future
                    if (selectedDate.before(Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        })
                    ) {
                        Toast.makeText(this, "Please select today or a future date", Toast.LENGTH_SHORT).show()
                    } else {
                        val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                        inputDate.setText(formattedDate)
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Disable all past dates
            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }

        // 📤 Submit booking
        btnSubmit.setOnClickListener {
            val name = inputName.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val date = inputDate.text.toString().trim()
            val make = inputMake.text.toString().trim()
            val model = inputModel.text.toString().trim()
            val mileage = inputMileage.text.toString().trim()
            var service = spinnerServices.selectedItem?.toString() ?: ""

            if (service == "Other") {
                val otherService = inputOtherService.text.toString().trim()
                if (otherService.isEmpty()) {
                    Toast.makeText(this, "Please specify your service", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                service = otherService
            }

            // 🧾 Validation
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                date.isEmpty() || service.isEmpty() || make.isEmpty() ||
                model.isEmpty() || mileage.isEmpty()
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ⛔ Prevent manual entry of past date
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val selectedDate = sdf.parse(date)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                if (selectedDate != null && selectedDate.before(today)) {
                    Toast.makeText(this, "You cannot book a past date!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Create booking data
            val bookingData = hashMapOf(
                "createdAt" to Date(),
                "preferredDate" to date,
                "mileage" to mileage.toInt(),
                "serviceType" to service,
                "status" to "pending",
                "userEmail" to email,
                "userId" to UUID.randomUUID().toString(),
                "userName" to name,
                "userPhone" to phone,
                "vehicleMake" to make,
                "vehicleModel" to model
            )

            firestore.collection("bookings")
                .add(bookingData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Booking submitted successfully! Status: Pending", Toast.LENGTH_SHORT).show()
                    inputName.text.clear()
                    inputEmail.text.clear()
                    inputPhone.text.clear()
                    inputDate.text.clear()
                    inputMake.text.clear()
                    inputModel.text.clear()
                    inputMileage.text.clear()
                    inputOtherService.text.clear()
                    spinnerServices.setSelection(0)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to submit booking", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
