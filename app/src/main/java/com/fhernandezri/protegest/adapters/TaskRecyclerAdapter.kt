package com.fhernandezri.protegest.adapters

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.databinding.ItemTaskBinding
import com.fhernandezri.protegest.models.Task
import java.util.Locale

class TaskRecyclerAdapter(private val tasks: List<Task>) : RecyclerView.Adapter<TaskRecyclerAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        with(holder) {
            with(tasks[position]) {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.taskName.text = name
                binding.taskDescription.text = description
                binding.taskMaxDate.text = "Fecha máxima: " + formatter.format(maxDate.toDate())
                binding.taskExperience.text = experience.toString() + " XP"
            }
        }
    }

    override fun getItemCount() = tasks.size
}