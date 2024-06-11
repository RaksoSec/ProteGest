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
import com.fhernandezri.protegest.models.Task
import com.google.firebase.firestore.FirebaseFirestore

class TaskAdapter(context: Context, private val layoutResId: Int, private val tasks: List<Task>) :
    ArrayAdapter<Task>(context, layoutResId, tasks) {

    private val db = FirebaseFirestore.getInstance()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutResId, parent, false)

        val taskNameTextView: TextView = view.findViewById(R.id.taskNameTextView)
        val deleteTaskButton: Button = view.findViewById(R.id.deleteTaskButton)

        val task = tasks[position]

        taskNameTextView.text = task.name

        deleteTaskButton.setOnClickListener {
            db.collection("tasks").document(task.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Tarea eliminada correctamente", Toast.LENGTH_SHORT).show()
                    this.remove(task)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al eliminar la tarea", Toast.LENGTH_SHORT).show()
                }
        }

        return view
    }
}