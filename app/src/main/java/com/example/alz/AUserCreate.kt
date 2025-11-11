package com.example.alz

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot

class AUserCreate : AppCompatActivity() {

    // Data model for Firestore documents
    data class User(
        val id: String = "",
        val firstName: String = "",
        val surname: String = "",
        val age: Int = 0,
        val phone: String = "",
        val email: String = ""
    )

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerUsers: RecyclerView
    private lateinit var adapter: UserAdapter
    private val usersList = mutableListOf<User>()

    // NEW: Dashboard Button Declaration
    private lateinit var btnDashboard: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auser_create)

        db = FirebaseFirestore.getInstance()

        val inputName = findViewById<EditText>(R.id.inputName)
        val inputSurname = findViewById<EditText>(R.id.inputSurname)
        val inputAge = findViewById<EditText>(R.id.inputAge)
        val inputPhone = findViewById<EditText>(R.id.inputPhone)
        val inputEmail = findViewById<EditText>(R.id.inputEmail)
        val inputPassword = findViewById<EditText>(R.id.inputPassword)
        val btnSave = findViewById<Button>(R.id.btnSaveUser)
        val btnView = findViewById<Button>(R.id.btnViewUsers)
        recyclerUsers = findViewById(R.id.recyclerUsers)

        // NEW: Dashboard Button Initialization and Click Listener
        btnDashboard = findViewById(R.id.FourADashboard)
        btnDashboard.setOnClickListener {
            val intent = Intent(this, adashboard::class.java)
            startActivity(intent)
        }

        recyclerUsers.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter(usersList)
        recyclerUsers.adapter = adapter

        // ✅ Save user button
        btnSave.setOnClickListener {
            val firstName = inputName.text.toString().trim()
            val surname = inputSurname.text.toString().trim()
            val ageText = inputAge.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (firstName.isEmpty() || surname.isEmpty() || ageText.isEmpty() ||
                phone.isEmpty() || email.isEmpty() || password.isEmpty()
            ) {
                Toast.makeText(this, "⚠️ Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull() ?: 0

            val newUser = hashMapOf(
                "firstName" to firstName,
                "surname" to surname,
                "age" to age,
                "phone" to phone,
                "email" to email,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .add(newUser)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ User saved successfully!", Toast.LENGTH_SHORT).show()
                    inputName.text.clear()
                    inputSurname.text.clear()
                    inputAge.text.clear()
                    inputPhone.text.clear()
                    inputEmail.text.clear()
                    inputPassword.text.clear()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "❌ Error saving user: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        // ✅ View users button
        btnView.setOnClickListener {
            loadUsers()
        }
    }

    // Function to fetch and display users
    private fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                usersList.clear()
                for (doc: QueryDocumentSnapshot in result) {
                    val user = User(
                        id = doc.id,
                        firstName = doc.getString("firstName") ?: "",
                        surname = doc.getString("surname") ?: "",
                        age = (doc.getLong("age") ?: 0).toInt(),
                        phone = doc.getString("phone") ?: "",
                        email = doc.getString("email") ?: ""
                    )
                    usersList.add(user)
                }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "✅ Loaded ${usersList.size} users", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Error loading users: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // RecyclerView Adapter
    inner class UserAdapter(private val users: List<User>) :
        RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

        inner class UserViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            // Assuming item_user has these IDs
            val tvUserId: TextView = view.findViewById(R.id.tvUserId)
            val tvUserName: TextView = view.findViewById(R.id.tvUserName)
            val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
            val tvUserPhone: TextView = view.findViewById(R.id.tvUserPhone)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): UserViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)
            return UserViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
            val user = users[position]
            holder.tvUserId.text = "ID: ${user.id}"
            holder.tvUserName.text = "${user.firstName} ${user.surname}"
            holder.tvUserEmail.text = "Email: ${user.email}"
            holder.tvUserPhone.text = "Phone: ${user.phone}"
        }

        override fun getItemCount(): Int = users.size
    }
}