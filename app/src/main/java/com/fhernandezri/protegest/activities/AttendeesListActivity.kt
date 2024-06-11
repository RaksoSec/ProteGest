package com.fhernandezri.protegest.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.adapters.AttendeesAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.fhernandezri.protegest.models.User

class AttendeesListActivity : AppCompatActivity() {
    private lateinit var attendeesAdapter: AttendeesAdapter
    private lateinit var attendeesRecyclerView: RecyclerView
    private lateinit var attendeesNoUsersJoined: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendees_list)

        attendeesRecyclerView = findViewById(R.id.attendeesRecyclerView)
        attendeesRecyclerView.layoutManager = LinearLayoutManager(this)
        attendeesAdapter = AttendeesAdapter(listOf())
        attendeesRecyclerView.adapter = attendeesAdapter

        val eventId = intent.getStringExtra("eventId")
        Log.d("AttendeesListActivity", "Event ID: $eventId")

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        val db = FirebaseFirestore.getInstance()
        val eventDocument = db.collection("events").document(eventId ?: "")

        eventDocument.get()
            .addOnSuccessListener { document ->
                val usersJoined = document.get("usersJoined")
                if (usersJoined is ArrayList<*>) {
                    Log.d("AttendeesListActivity", "Users joined: $usersJoined")
                    val usersData = ArrayList<User>()
                    val usersCollection = db.collection("users")
                    for (userId in usersJoined) {
                        if (userId is String && userId.isNotEmpty()) {
                            usersCollection.document(userId).get()
                                .addOnSuccessListener { userDocument ->
                                    val userEmail = userDocument.getString("email")
                                    val userGroupId = userDocument.getString("groupId")
                                    val userName = userDocument.getString("name")
                                    val userRole = userDocument.getString("role")
                                    val userRank = userDocument.getString("rank")
                                    val tasksDone = userDocument.getLong("tasksDone")
                                    val experience = userDocument.getLong("experience")
                                    val eventsDone = userDocument.getLong("eventsDone")
                                    if (userEmail != null && userGroupId != null && userName != null && userRole != null && userRank != null && tasksDone != null && experience != null && eventsDone != null) {
                                        usersData.add(User(userId, userEmail, userGroupId, userName, userRole, userRank, tasksDone.toInt(), experience.toInt(), eventsDone.toInt()))
                                    }
                                }
                                .addOnCompleteListener {
                                    attendeesAdapter.updateData(usersData)
                                }
                        } else if (userId is String && userId.isEmpty()) {
                            // Ignora los valores vacíos y continúa con el siguiente elemento
                            continue
                        } else {
                            Log.e("AttendeesListActivity", "Error: userId is not a valid String")
                        }
                    }
                } else {
                    Log.e("AttendeesListActivity", "Error: usersJoined is not an ArrayList<String>")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("AttendeesListActivity", "Error getting document: $exception")
            }
    }

    fun goBack(view: View) {
        finish()
    }
}