package com.example.alz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val users: List<AUserCreate.User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUserId: TextView = view.findViewById(R.id.tvUserId)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvUserPhone: TextView = view.findViewById(R.id.tvUserPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
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
