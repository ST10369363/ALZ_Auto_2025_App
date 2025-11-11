package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

class invoice : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private val invoiceList = mutableListOf<Invoice>()

    data class Invoice(
        val invoiceId: String = "",
        val customerName: String = "",
        val createdAt: String = "",
        val itemDescription: String = "",
        val total: Double = 0.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerInvoices)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadUserInvoices()

        // 🧭 Navigation back to dashboard
        findViewById<Button>(R.id.ThreeDashboard).setOnClickListener {
            val intent = Intent(this, dashboard::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserInvoices() {
        // ✅ Retrieve the user email from SharedPreferences (from login/profile)
        val userEmail = getSharedPreferences("UserSession", MODE_PRIVATE)
            .getString("userEmail", null)

        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in. Please sign in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // ✅ Query invoices where the nested field 'customer.email' matches
        db.collection("invoices")
            .whereEqualTo("customer.email", userEmail)
            .get()
            .addOnSuccessListener { result ->
                invoiceList.clear()

                for (document in result) {
                    val invoice = parseInvoice(document)
                    invoiceList.add(invoice)
                }

                recyclerView.adapter = InvoiceAdapter(invoiceList)

                if (invoiceList.isEmpty()) {
                    Toast.makeText(this, "No invoices found for $userEmail", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load invoices: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun parseInvoice(doc: QueryDocumentSnapshot): Invoice {
        val customerMap = doc.get("customer") as? Map<*, *>
        val itemsList = doc.get("items") as? List<Map<String, Any>>

        val customerName = customerMap?.get("name")?.toString() ?: "Unknown"

        val createdAt = try {
            val timestamp = doc.getTimestamp("createdAt")?.toDate()
            if (timestamp != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                sdf.format(timestamp)
            } else {
                "No Date"
            }
        } catch (e: Exception) {
            "No Date"
        }

        val itemDescription = itemsList?.getOrNull(0)?.get("description")?.toString() ?: "No Description"
        val total = doc.getDouble("total") ?: 0.0

        return Invoice(
            invoiceId = doc.id,
            customerName = customerName,
            createdAt = createdAt,
            itemDescription = itemDescription,
            total = total
        )
    }

    inner class InvoiceAdapter(private val invoices: List<Invoice>) :
        RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

        inner class InvoiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvInvoiceTitle)
            val amount: TextView = view.findViewById(R.id.tvInvoiceAmount)
            val date: TextView = view.findViewById(R.id.tvInvoiceDate)
            val deleteBtn: Button = view.findViewById(R.id.btnDeleteInvoice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_invoice, parent, false)
            return InvoiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
            val invoice = invoices[position]
            holder.title.text = "Invoice for ${invoice.customerName}"
            holder.amount.text = "Total: ${invoice.total}"
            holder.date.text = "Created: ${invoice.createdAt}"

            // 🧾 Open details
            holder.itemView.setOnClickListener {
                val intent = Intent(this@invoice, InvoiceDetailActivity::class.java)
                intent.putExtra("invoiceId", invoice.invoiceId)
                startActivity(intent)
            }

            // 🗑️ Delete invoice
            holder.deleteBtn.setOnClickListener {
                db.collection("invoices").document(invoice.invoiceId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@invoice, "Invoice deleted", Toast.LENGTH_SHORT).show()
                        loadUserInvoices() // refresh list
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@invoice, "Failed to delete invoice", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        override fun getItemCount(): Int = invoices.size
    }
}
