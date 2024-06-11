package com.fhernandezri.protegest.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.activities.EventDetailActivity
import com.fhernandezri.protegest.models.Event
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(context: Context, resource: Int, events: List<Event>) : ArrayAdapter<Event>(context, resource, events) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_event, parent, false)

        val eventName: TextView = view.findViewById(R.id.eventName)
        val eventDate: TextView = view.findViewById(R.id.eventDate)

        val event = getItem(position)

        eventName.text = event?.name

        // Format the date to a more readable format
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        eventDate.text = event?.date?.toDate()?.let { sdf.format(it) }

        view.setOnClickListener {
            val intent = Intent(context, EventDetailActivity::class.java)
            intent.putExtra("id", event?.id)
            context.startActivity(intent)
        }

        return view
    }
}