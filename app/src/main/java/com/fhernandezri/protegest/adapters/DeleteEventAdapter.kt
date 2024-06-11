package com.fhernandezri.protegest.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.models.Event
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class DeleteEventAdapter(private val context: Context,
                         private val layoutResId: Int,
                         private val data: List<Event>) : ArrayAdapter<Event>(context, layoutResId, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)

        val event = data[position]

        val eventNameTextView: TextView = view.findViewById(R.id.eventNameTextView)
        val eventDescriptionTextView: TextView = view.findViewById(R.id.eventDescriptionTextView)
        val deleteEventButton: Button = view.findViewById(R.id.deleteEventButton)

        eventNameTextView.text = event.name
        eventDescriptionTextView.text = event.description

        deleteEventButton.setOnClickListener {
            FirebaseFirestore.getInstance().collection("events").document(event.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Servicio eliminado correctamente", Toast.LENGTH_SHORT).show()
                    this.remove(event)
                    this.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al eliminar el servicio", Toast.LENGTH_SHORT).show()
                    Log.w("DeleteEventAdapter", "Error deleting document", e)
                }
        }

        return view
    }
}