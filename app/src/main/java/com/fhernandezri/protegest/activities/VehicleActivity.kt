package com.fhernandezri.protegest.activities

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.fhernandezri.protegest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class VehicleActivity : AppCompatActivity() {
    private lateinit var vehicleLayout: LinearLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle)

        vehicleLayout = findViewById(R.id.vehicleLayout)
        auth = FirebaseAuth.getInstance()

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val groupId = document.getString("groupId")
                    if (groupId != null) {
                        loadVehicles(groupId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting document", e)
                }
        }
    }

    private fun loadVehicles(groupId: String) {
        FirebaseFirestore.getInstance().collection("vehicles")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val vehicleId = document.id
                    val vehicleName = document.getString("name") ?: ""
                    val vehiclePlate = document.getString("plate") ?: ""

                    val vehicleView = layoutInflater.inflate(R.layout.item_vehicle, null)
                    val vehicleImage: ImageView = vehicleView.findViewById(R.id.vehicleImage)
                    val vehicleNameTextView: TextView = vehicleView.findViewById(R.id.vehicleName)
                    val vehiclePlateTextView: TextView = vehicleView.findViewById(R.id.vehiclePlate)

                    vehicleNameTextView.text = vehicleName
                    vehiclePlateTextView.text = vehiclePlate

                    val storageReference = FirebaseStorage.getInstance().getReference("vehicles/$vehicleId")
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.default_profile) // Aquí se establece la imagen de reserva
                            .into(vehicleImage)
                    }.addOnFailureListener {
                        Glide.with(this)
                            .load(R.drawable.default_profile)
                            .into(vehicleImage)
                    }

                    vehicleLayout.addView(vehicleView)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    fun goBack(view: View) {
        finish()
    }
}