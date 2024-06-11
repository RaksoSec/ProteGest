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
import com.fhernandezri.protegest.models.User
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class DeleteVolunteerAdapter(private val context: Context,
                             private val layoutResId: Int,
                             private val data: List<User>) : ArrayAdapter<User>(context, layoutResId, data) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)

        val user = data[position]

        val volunteerNameTextView: TextView = view.findViewById(R.id.volunteerNameTextView)
        val volunteerRankTextView: TextView = view.findViewById(R.id.volunteerRankTextView)
        val deleteVolunteerButton: Button = view.findViewById(R.id.deleteVolunteerButton)

        volunteerNameTextView.text = user.name
        volunteerRankTextView.text = user.rank

        deleteVolunteerButton.setOnClickListener {
            FirebaseFirestore.getInstance().collection("users").document(user.id)
                .update("groupId", "")
                .addOnSuccessListener {
                    Toast.makeText(context, "Voluntario eliminado correctamente", Toast.LENGTH_SHORT).show()
                    this.remove(user)
                    this.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al eliminar el voluntario", Toast.LENGTH_SHORT).show()
                    Log.w("DeleteVolunteerAdapter", "Error updating document", e)
                }
        }

        return view
    }
}