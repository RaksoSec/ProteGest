package com.fhernandezri.protegest.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.models.Achievement

class AchievementAdapter(private val achievements: List<Achievement>) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.achievementName)
        val description: TextView = view.findViewById(R.id.achievementDescription)
        val xp: TextView = view.findViewById(R.id.achievementXp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.achievement_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.name.text = achievement.name
        holder.description.text = achievement.description
        holder.xp.text = "${achievement.xp} XP"
    }

    override fun getItemCount() = achievements.size
}