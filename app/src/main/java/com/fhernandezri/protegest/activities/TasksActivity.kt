package com.fhernandezri.protegest.activities

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.adapters.TaskRecyclerAdapter
import com.fhernandezri.protegest.models.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TasksActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: TaskRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tasksRecyclerView = findViewById<RecyclerView>(R.id.tasks_recycler_view)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w(TAG, "No user is currently signed in.")
            return
        }

        // Get the current user's document from Firestore
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val userGroupId = document.getString("groupId")

                if (userGroupId != null) {
                    // Get the tasks that belong to the user's group
                    db.collection("tasks")
                        .whereEqualTo("groupId", userGroupId)
                        .whereEqualTo("done", false)
                        .addSnapshotListener { value, error ->
                            if (error != null) {
                                Log.w(TAG, "Listen failed.", error)
                                return@addSnapshotListener
                            }

                            val tasks = value?.toObjects(Task::class.java)
                            if (tasks != null) {
                                adapter = TaskRecyclerAdapter(tasks)
                                tasksRecyclerView.adapter = adapter
                            }
                        }
                } else {
                    Log.w(TAG, "User's groupId is null.")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting user document.", exception)
            }
    }

    fun goBack(view: View) {
        finish()
    }
}