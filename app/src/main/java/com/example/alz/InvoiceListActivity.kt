package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class InvoiceListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerInvoices: RecyclerView
    private var invoiceList = mutableListOf<Invoice>()

    data class Invoice(
        val id: String = "",
        val createdAt: String? = null,
        val total: Double? = null,
        val customerName: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice_list)

        recyclerInvoices = findViewById(R.id.recyclerInvoices)
        recyclerInvoices.layoutManager = LinearLayoutManager(this)

        db = FirebaseFirestore.getInstance()

        val userId = intent.getStringExtra("userId")
        if (userId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadInvoices(userId)
    }

    private fun loadInvoices(userId: String) {
        db.collection("invoices")
            .whereEqualTo("createdBy", userId)
            .get()
            .addOnSuccessListener { result ->
                invoiceList.clear()
                for (doc in result) {
                    val customer = doc.get("customer") as? Map<*, *>
                    val invoice = Invoice(
                        id = doc.id,
                        createdAt = doc.getString("createdAt"),
                        total = doc.getDouble("total"),
                        customerName = customer?.get("name") as? String
                    )
                    invoiceList.add(invoice)
                }
                recyclerInvoices.adapter = InvoiceAdapter(invoiceList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error loading invoices", Toast.LENGTH_SHORT).show()
            }
    }

    inner class InvoiceAdapter(private val invoices: List<Invoice>) :
        RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

        inner class InvoiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvInvoiceId: TextView = view.findViewById(R.id.tvInvoiceId)
            val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
            val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_invoice, parent, false)
            return InvoiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
            val invoice = invoices[position]
            holder.tvInvoiceId.text = "Invoice ID: ${invoice.id}"
            holder.tvCustomerName.text = "Customer: ${invoice.customerName ?: "N/A"}"
            holder.tvTotal.text = "Total: ${invoice.total ?: 0.0}"

            holder.itemView.setOnClickListener {
                val intent = Intent(this@InvoiceListActivity, InvoiceDetailActivity::class.java)
                intent.putExtra("invoiceId", invoice.id)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int = invoices.size
    }
}
