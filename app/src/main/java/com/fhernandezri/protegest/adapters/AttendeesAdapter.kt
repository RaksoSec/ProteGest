package com.fhernandezri.protegest.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.models.User

class AttendeesAdapter(private var data: List<User>) : RecyclerView.Adapter<AttendeesAdapter.AttendeeViewHolder>() {

    inner class AttendeeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val attendeeName: TextView = view.findViewById(R.id.attendeeName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendeeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.attendee_card, parent, false)
        return AttendeeViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendeeViewHolder, position: Int) {
        val attendee = data[position]
        holder.attendeeName.text = attendee.name
    }

    override fun getItemCount() = data.size

    fun updateData(newData: List<User>) {
        Log.d("AttendeesAdapter", "updateData called with data: $newData")
        data = newData
        notifyDataSetChanged()
    }
}