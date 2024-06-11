package com.fhernandezri.protegest.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.adapters.UserAdapter
import com.fhernandezri.protegest.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VolunteersActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("VolunteersActivity", "onCreate called")
        setContentView(R.layout.activity_volunteers)

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        // Enables return button on the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val currentGroupId = document.getString("groupId") ?: ""
                    db.collection("users")
                        .whereEqualTo("groupId", currentGroupId)
                        .get()
                        .addOnSuccessListener { documents ->
                            Log.d("Firestore", "Documents fetched successfully")
                            val users = documents.map { document ->
                                User(
                                    id = document.getString("id") ?: "",
                                    email = document.getString("email") ?: "",
                                    groupId = document.getString("groupId") ?: "",
                                    name = document.getString("name") ?: "",
                                    role = document.getString("role") ?: "Standard",
                                    rank = document.getString("rank") ?: "unknown",
                                    tasksDone = document.getLong("tasksDone")?.toInt() ?: 0,
                                    experience = document.getLong("experience")?.toInt() ?: 0,
                                    eventsDone = document.getLong("eventsDone")?.toInt() ?: 0
                                )
                            }
                            Log.d("Firestore", "Users: $users")
                            val adapter = UserAdapter(users)
                            val volunteersRecyclerView: RecyclerView = findViewById(R.id.volunteersRecyclerView)
                            volunteersRecyclerView.layoutManager = LinearLayoutManager(this)
                            volunteersRecyclerView.adapter = adapter
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Error fetching documents: ", exception)
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error fetching current user document: ", exception)
                }
        } else {
            Log.d("VolunteersActivity", "Current user is null")
        }
    }

    // Manage return button on the action bar
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