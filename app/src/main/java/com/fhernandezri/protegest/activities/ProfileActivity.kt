package com.fhernandezri.protegest.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.fhernandezri.protegest.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {
    private lateinit var nameTextView: TextView
    private lateinit var rankTextView: TextView
    private lateinit var roleTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var uploadPhotoButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val user = auth.currentUser
        if (user != null && uri != null) {
            val filename = user.uid
            val ref = FirebaseStorage.getInstance().getReference("/profilePics/$filename")

            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        db.collection("users").document(user.uid)
                            .update("profileImageUrl", it.toString())
                    }
                }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        nameTextView = findViewById(R.id.nameTextView)
        rankTextView = findViewById(R.id.rankTextView)
        roleTextView = findViewById(R.id.roleTextView)
        profileImageView = findViewById(R.id.profileImageView)
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton)

        val currentUser = auth.currentUser

        // Show the photo right after the user uploads it
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        nameTextView.text = snapshot.getString("name")
                        rankTextView.text = snapshot.getString("rank")
                        roleTextView.text = snapshot.getString("role")
                        val profileImageUrl = snapshot.getString("profileImageUrl")
                        if (profileImageUrl != null) {
                            Glide.with(this).load(profileImageUrl).into(profileImageView)
                        } else {
                            // If the user has not uploaded a profile image, load the placeholder
                            Glide.with(this).load(R.drawable.default_profile).into(profileImageView)
                        }
                    }
                }
        }

        // RESET PASSWORD LOGIC
        val changePasswordButton: Button = findViewById(R.id.changePasswordButton)
        changePasswordButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
            val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.currentPasswordEditText)
            val newPasswordEditText = dialogView.findViewById<EditText>(R.id.newPasswordEditText)
            val confirmNewPasswordEditText = dialogView.findViewById<EditText>(R.id.confirmNewPasswordEditText)

            AlertDialog.Builder(this)
                .setTitle("Cambiar contraseña")
                .setView(dialogView)
                .setPositiveButton("Confirmar") { _, _ ->
                    val currentPassword = currentPasswordEditText.text.toString()
                    val newPassword = newPasswordEditText.text.toString()
                    val confirmNewPassword = confirmNewPasswordEditText.text.toString()

                    if (newPassword == confirmNewPassword) {
                        val user = auth.currentUser
                        if (user != null && user.email != null) {
                            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                            user.reauthenticate(credential).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    user.updatePassword(newPassword).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(this, "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this, "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        uploadPhotoButton.setOnClickListener {
            getContent.launch("image/*")
        }
    }

    fun goBack(view: View) {
        finish()
    }
}