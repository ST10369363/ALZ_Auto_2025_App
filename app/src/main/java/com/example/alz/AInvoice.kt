package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class AInvoice : AppCompatActivity() {

    // Declaration of the Dashboard button property
    private lateinit var btnDashboard: Button

    private lateinit var inputName: EditText
    private lateinit var inputSurname: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPhone: EditText
    private lateinit var inputBookingID: EditText
    private lateinit var inputVehicle: EditText
    private lateinit var inputDate: EditText
    private lateinit var lineItemsContainer: LinearLayout
    private lateinit var btnAddLine: Button
    private lateinit var tvTotalAmount: TextView
    private lateinit var inputSubtotal: EditText
    private lateinit var inputTaxPercentage: EditText
    private lateinit var inputTaxAmount: EditText
    private lateinit var inputNotes: EditText
    private lateinit var btnCreateInvoice: Button
    private lateinit var btnClear: Button
    private lateinit var btnPreview: Button

    private val db = FirebaseFirestore.getInstance()

    // Prevent recursive updates from TextWatchers when we programmatically setText()
    private var isUpdatingFields = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The line below should be called first to load the layout XML
        // Make sure this matches your actual XML filename in res/layout/
        setContentView(R.layout.activity_ainvoice)

        // FIX: Initialize the Dashboard button *after* setContentView
        // I am assuming the ID is ABookingDashboard from your previous XML,
        // but using TwoAInvoiceDashboard as per your code's broken reference.
        // I'll stick to ABookingDashboard as it appears in your XML.
        btnDashboard = findViewById(R.id.TwoAInvoiceDashboard)

        btnDashboard.setOnClickListener {
            val intent = Intent(this, adashboard::class.java)
            startActivity(intent)
        }

        // Initialize other views
        inputName = findViewById(R.id.inv_inputName)
        inputSurname = findViewById(R.id.inv_inputSurname)
        inputEmail = findViewById(R.id.inv_inputEmail)
        inputPhone = findViewById(R.id.inv_inputPhone)
        inputBookingID = findViewById(R.id.inv_inputBookingID)
        inputVehicle = findViewById(R.id.inv_inputVehicle)
        inputDate = findViewById(R.id.inv_inputDate)
        lineItemsContainer = findViewById(R.id.inv_lineItemsContainer)
        btnAddLine = findViewById(R.id.inv_btnAddLine)
        tvTotalAmount = findViewById(R.id.inv_tvTotalAmount)
        inputSubtotal = findViewById(R.id.inv_inputSubtotal)
        inputTaxPercentage = findViewById(R.id.inv_inputTaxPercentage)
        inputTaxAmount = findViewById(R.id.inv_inputTaxAmount)
        inputNotes = findViewById(R.id.inv_inputNotes)
        btnCreateInvoice = findViewById(R.id.inv_btnCreateInvoice)
        btnClear = findViewById(R.id.inv_btnClear)


        // Add listeners
        btnAddLine.setOnClickListener {
            addLineItem()
            calculateTotal()
        }

        btnClear.setOnClickListener { clearForm() }

        btnCreateInvoice.setOnClickListener { createInvoice() }

        // Add watcher to tax percentage so changing it recalculates totals
        inputTaxPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { calculateTotal() }
        })

        // If user edits subtotal or tax amount manually, recalc grand total display
        inputSubtotal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { if (!isUpdatingFields) calculateTotal() }
        })
        inputTaxAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { if (!isUpdatingFields) calculateTotal() }
        })

        // Start with one empty line item
        addLineItem()
        calculateTotal()
    }

    private fun addLineItem() {
        // Horizontal row with Description (3), Qty (1), Unit (2)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }

        val description = EditText(this).apply {
            hint = "Description"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f).apply {
                marginEnd = 8
            }
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val qty = EditText(this).apply {
            hint = "Qty"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val unit = EditText(this).apply {
            hint = "Unit Price"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        // Recalculate whenever qty or unit change
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { calculateTotal() }
        }
        qty.addTextChangedListener(watcher)
        unit.addTextChangedListener(watcher)

        row.addView(description)
        row.addView(qty)
        row.addView(unit)

        lineItemsContainer.addView(row)
    }

    private fun calculateTotal() {
        // avoid recursive calls when we programmatically setText()
        if (isUpdatingFields) return
        isUpdatingFields = true

        try {
            var computedSubtotal = 0.0
            for (i in 0 until lineItemsContainer.childCount) {
                val row = lineItemsContainer.getChildAt(i)
                if (row !is LinearLayout) continue
                // Expect child order: 0=desc,1=qty,2=unit
                val qtyView = row.getChildAt(1) as? EditText
                val unitView = row.getChildAt(2) as? EditText
                val qty = qtyView?.text.toString().trim().toIntOrNull() ?: 0
                val unit = unitView?.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (qty > 0 && unit > 0.0) {
                    computedSubtotal += qty * unit
                }
            }

            // Tax percentage from field (if user filled) or zero
            val taxRate = inputTaxPercentage.text.toString().trim().toDoubleOrNull() ?: 0.0

            // Set subtotal and tax amount programmatically (rounded to 2 decimals)
            val roundedSubtotal = String.format(Locale.US, "%.2f", computedSubtotal)
            if (inputSubtotal.text.toString() != roundedSubtotal) {
                inputSubtotal.setText(roundedSubtotal)
            }

            val taxAmount = (computedSubtotal * taxRate) / 100.0
            val roundedTaxAmount = String.format(Locale.US, "%.2f", taxAmount)
            if (inputTaxAmount.text.toString() != roundedTaxAmount) {
                inputTaxAmount.setText(roundedTaxAmount)
            }

            val grand = computedSubtotal + taxAmount
            tvTotalAmount.text = "Total Amount: R${String.format(Locale.US, "%.2f", grand)}"
        } catch (e: Exception) {
            // Log but don't crash the app
            Log.e("AInvoice", "Error calculating totals", e)
        } finally {
            isUpdatingFields = false
        }
    }

    private fun clearForm() {
        inputName.text.clear()
        inputSurname.text.clear()
        inputEmail.text.clear()
        inputPhone.text.clear()
        inputBookingID.text.clear()
        inputVehicle.text.clear()
        inputDate.text.clear()
        inputSubtotal.text.clear()
        inputTaxPercentage.text.clear()
        inputTaxAmount.text.clear()
        inputNotes.text.clear()
        lineItemsContainer.removeAllViews()
        tvTotalAmount.text = "Total Amount: R0.00"
        addLineItem()
        calculateTotal()
    }

    private fun createInvoice() {
        // Validate user & booking fields
        val name = inputName.text.toString().trim()
        val surname = inputSurname.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val phone = inputPhone.text.toString().trim()
        val bookingId = inputBookingID.text.toString().trim()
        val vehicle = inputVehicle.text.toString().trim()
        val date = inputDate.text.toString().trim()
        val notes = inputNotes.text.toString().trim()

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() ||
            phone.isEmpty() || bookingId.isEmpty() || vehicle.isEmpty() || date.isEmpty()
        ) {
            Toast.makeText(this, "Please fill in all user and booking fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure totals are up-to-date before saving
        calculateTotal()

        val subtotal = inputSubtotal.text.toString().trim().toDoubleOrNull()
        val taxRate = inputTaxPercentage.text.toString().trim().toDoubleOrNull() ?: 0.0
        val taxAmount = inputTaxAmount.text.toString().trim().toDoubleOrNull()

        if (subtotal == null || taxAmount == null) {
            Toast.makeText(this, "Please ensure subtotal and tax amount are valid numbers.", Toast.LENGTH_SHORT).show()
            return
        }

        // Gather line items
        val items = mutableListOf<Map<String, Any>>()
        for (i in 0 until lineItemsContainer.childCount) {
            val row = lineItemsContainer.getChildAt(i)
            if (row !is LinearLayout) continue
            val descView = row.getChildAt(0) as? EditText
            val qtyView = row.getChildAt(1) as? EditText
            val unitView = row.getChildAt(2) as? EditText
            val description = descView?.text.toString().trim()
            val qty = qtyView?.text.toString().trim().toIntOrNull() ?: 0
            val unit = unitView?.text.toString().trim().toDoubleOrNull() ?: 0.0
            if (description.isEmpty() || qty <= 0 || unit <= 0.0) {
                Toast.makeText(this, "Please fill all line items correctly (Description, Qty > 0, Unit Price > 0).", Toast.LENGTH_SHORT).show()
                return
            }
            items.add(mapOf(
                "description" to description,
                "qty" to qty,
                "unit" to unit,
                "lineTotal" to (qty * unit)
            ))
        }

        if (items.isEmpty()) {
            Toast.makeText(this, "Please add at least one valid line item.", Toast.LENGTH_SHORT).show()
            return
        }

        val total = subtotal + taxAmount

        val invoice = hashMapOf<String, Any>(
            "bookingId" to bookingId,
            "customer" to hashMapOf(
                "name" to "$name $surname",
                "email" to email,
                "phone" to phone
            ),
            "vehicle" to vehicle,
            "date" to date,
            "items" to items,
            "subtotal" to subtotal,
            "taxRate" to taxRate,
            "taxAmount" to taxAmount,
            "total" to total,
            "notes" to notes,
            "createdAt" to Timestamp.now()
        )

        // Save to Firestore
        btnCreateInvoice.isEnabled = false
        db.collection("invoices")
            .add(invoice)
            .addOnSuccessListener {
                Toast.makeText(this, "Invoice created successfully!", Toast.LENGTH_SHORT).show()
                clearForm()
                btnCreateInvoice.isEnabled = true
            }
            .addOnFailureListener { e ->
                btnCreateInvoice.isEnabled = true
                Toast.makeText(this, "Failed to create invoice: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("AInvoice", "Firestore save error", e)
            }
    }
}