package com.fhernandezri.protegest.activities

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.adapters.AchievementAdapter
import com.fhernandezri.protegest.models.Achievement
import com.google.firebase.firestore.FirebaseFirestore

class AchievementsActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var achievementsList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)       // Return button

        db = FirebaseFirestore.getInstance()
        achievementsList = findViewById(R.id.achievementsList)
        achievementsList.layoutManager = LinearLayoutManager(this)

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        // Get all achievements from Firestore
        db.collection("achievements")
            .get()
            .addOnSuccessListener { documents ->
                val achievements = documents.map { doc ->
                    Achievement(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        xp = doc.getLong("xp")?.toInt() ?: 0
                    )
                }
                achievementsList.adapter = AchievementAdapter(achievements)
            }
    }

    // Logic to return to the previous activity when the back button is pressed
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun goBack(view: View) {
        finish()
    }
}