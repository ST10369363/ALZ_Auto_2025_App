package com.example.alz

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class InvoiceDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var itemsAdapter: InvoiceItemsAdapter
    private val itemsList = mutableListOf<InvoiceItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_detail)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Invoice Details"

        db = FirebaseFirestore.getInstance()

        // Initialize RecyclerView for items
        val recyclerItems = findViewById<RecyclerView>(R.id.recyclerItems)
        recyclerItems.layoutManager = LinearLayoutManager(this)
        itemsAdapter = InvoiceItemsAdapter(itemsList)
        recyclerItems.adapter = itemsAdapter

        // Get invoice ID from intent
        val invoiceId = intent.getStringExtra("invoiceId")
        if (invoiceId != null) {
            loadInvoiceDetails(invoiceId)
        } else {
            Toast.makeText(this, "Invoice ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadInvoiceDetails(invoiceId: String) {
        db.collection("invoices").document(invoiceId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    displayInvoiceData(document)
                } else {
                    Toast.makeText(this, "Invoice not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading invoice: ${exception.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayInvoiceData(document: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            // Basic invoice info
            findViewById<TextView>(R.id.tvBookingId).text = document.getString("bookingId") ?: "N/A"
            findViewById<TextView>(R.id.tvBookingRef).text = document.getString("bookingRef") ?: "N/A"

            // Format timestamp
            val timestamp = document.getTimestamp("createdAt")
            val createdAt = if (timestamp != null) {
                val dateFormat = SimpleDateFormat("dd MMMM yyyy 'at' HH:mm:ss", Locale.getDefault())
                dateFormat.format(timestamp.toDate())
            } else {
                "N/A"
            }
            findViewById<TextView>(R.id.tvCreatedAt).text = createdAt

            findViewById<TextView>(R.id.tvCreatedBy).text = document.getString("createdBy") ?: "N/A"

            // Customer info
            val customer = document.get("customer") as? Map<*, *>
            findViewById<TextView>(R.id.tvCustomerName).text = customer?.get("name") as? String ?: "N/A"
            findViewById<TextView>(R.id.tvCustomerEmail).text = customer?.get("email") as? String ?: "N/A"
            findViewById<TextView>(R.id.tvCustomerPhone).text = customer?.get("phone") as? String ?: "N/A"

            // Financial info
            findViewById<TextView>(R.id.tvSubtotal).text = formatCurrency(document.getDouble("subtotal"))
            findViewById<TextView>(R.id.tvTaxAmount).text = formatCurrency(document.getDouble("taxAmount"))
            findViewById<TextView>(R.id.tvTaxRate).text = "${document.getDouble("taxRate") ?: 0.0}%"
            findViewById<TextView>(R.id.tvTotal).text = formatCurrency(document.getDouble("total"))

            // Notes
            val notes = document.getString("notes")
            findViewById<TextView>(R.id.tvNotes).text = if (notes.isNullOrBlank()) "No notes" else notes

            // Load items
            val items = document.get("items") as? List<*>
            itemsList.clear()
            items?.forEach { item ->
                val itemMap = item as? Map<*, *>
                if (itemMap != null) {
                    itemsList.add(
                        InvoiceItem(
                            description = itemMap["description"] as? String ?: "",
                            qty = (itemMap["qty"] as? Number)?.toInt() ?: 0,
                            unit = (itemMap["unit"] as? Number)?.toDouble() ?: 0.0,
                            lineTotal = (itemMap["lineTotal"] as? Number)?.toDouble() ?: 0.0
                        )
                    )
                }
            }
            itemsAdapter.notifyDataSetChanged()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error displaying invoice data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatCurrency(amount: Double?): String {
        return if (amount != null) {
            String.format("R%.2f", amount)
        } else {
            "R0.00"
        }
    }

    data class InvoiceItem(
        val description: String,
        val qty: Int,
        val unit: Double,
        val lineTotal: Double
    )

    inner class InvoiceItemsAdapter(private val items: List<InvoiceItem>) :
        RecyclerView.Adapter<InvoiceItemsAdapter.ItemViewHolder>() {

        inner class ItemViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val tvDescription: TextView = itemView.findViewById(R.id.tvItemDescription)
            val tvQty: TextView = itemView.findViewById(R.id.tvItemQty)
            val tvUnit: TextView = itemView.findViewById(R.id.tvItemUnit)
            val tvLineTotal: TextView = itemView.findViewById(R.id.tvItemLineTotal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = layoutInflater.inflate(R.layout.item_invoice_detail, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = items[position]
            holder.tvDescription.text = item.description
            holder.tvQty.text = item.qty.toString()
            holder.tvUnit.text = formatCurrency(item.unit)
            holder.tvLineTotal.text = formatCurrency(item.lineTotal)
        }

        override fun getItemCount(): Int = items.size
    }
}