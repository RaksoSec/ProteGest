package com.fhernandezri.protegest.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fhernandezri.protegest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    private lateinit var welcomeText: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        // Init processes
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)      // Selects the layout
        welcomeText = findViewById(R.id.welcomeText) // Asegúrate de tener un TextView con este id en tu layout
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set the "hello" text
        val user = auth.currentUser
        if (user != null) {
            val docRef = db.collection("users").document(user.uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val name = document.getString("name")
                        welcomeText.text = "¡Hola, $name!"
                    } else {
                        welcomeText.text = "¡Hola!"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting user document: ", exception)
                }
        } else {
            welcomeText.text = "¡Hola!"
        }

        // Button to redirect to Calendar
        val calendarButton: Button = findViewById(R.id.calendarButton)
        calendarButton.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // Button to redirect to Volunteers
        val volunteersButton: Button = findViewById(R.id.volunteersButton)
        volunteersButton.setOnClickListener {
            val intent = Intent(this, VolunteersActivity::class.java)
            startActivity(intent)
        }

        // Button to redirect to Tasks
        val tasksButton: Button = findViewById(R.id.tasksButton)
        tasksButton.setOnClickListener {
            val intent = Intent(this, TasksActivity::class.java)
            startActivity(intent)
        }

        // Button to redirect to Achievements
        val achievementsButton: Button = findViewById(R.id.achievementsButton)
        achievementsButton.setOnClickListener {
            val intent = Intent(this, AchievementsActivity::class.java)
            startActivity(intent)
        }

        // Button to redirect to AdminPanel
        val adminPanelButton = findViewById<Button>(R.id.adminPanelButton)
        adminPanelButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val userRole = document.getString("role") ?: "Standard"
                        Log.d("Firestore", "User role: $userRole")
                        if (userRole == "Coordinator" || userRole == "Admin") {
                            val intent = Intent(this, AdminPanelActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "No tienes permiso para acceder a esta pantalla", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Firestore", "Error fetching current user document: ", exception)
                    }
            }
        }

        // Button to logout and redirect to MainActivity
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val vehicleButton: Button = findViewById(R.id.vehicleButton)
        vehicleButton.setOnClickListener {
            val intent = Intent(this, VehicleActivity::class.java)
            startActivity(intent)
        }

        val viewProfileButton: Button = findViewById(R.id.viewProfileButton)
        viewProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }
}