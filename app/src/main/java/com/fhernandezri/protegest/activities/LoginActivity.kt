package com.fhernandezri.protegest.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fhernandezri.protegest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enables return button on the action bar

        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)

        auth = FirebaseAuth.getInstance()                 // Firebase authentication instance
        forgotPassword = findViewById(R.id.forgotPassword)

        // Login Logic
        loginButton.setOnClickListener {
            val emailStr = email.text.toString()
            val passwordStr = password.text.toString()

            if (emailStr.isNotEmpty() && passwordStr.isNotEmpty()) {
                auth.signInWithEmailAndPassword(emailStr, passwordStr)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user?.isEmailVerified == true) {
                                // Check if the user exists in Firestore
                                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val groupId = document.getString("groupId")
                                            if (groupId == "") {
                                                // Show dialog to enter groupId
                                                val builder = AlertDialog.Builder(this)
                                                builder.setTitle("No tienes agrupación asignada. Para continuar debes pertenecer a una.")

                                                val input = EditText(this)
                                                input.hint = "Introduce tu groupId"
                                                builder.setView(input)

                                                builder.setPositiveButton("Aceptar") { dialog, _ ->
                                                    val groupIdStr = input.text.toString()
                                                    if (groupIdStr.isNotEmpty()) {
                                                        // Check if the groupId exists in Firestore
                                                        FirebaseFirestore.getInstance().collection("groups").document(groupIdStr)
                                                            .get()
                                                            .addOnSuccessListener { document ->
                                                                if (document.exists()) {
                                                                    // Update the user's groupId in Firestore
                                                                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                                                        .update("groupId", groupIdStr)
                                                                        .addOnSuccessListener {
                                                                            Toast.makeText(this, "GroupId actualizado correctamente", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                        .addOnFailureListener { e ->
                                                                            Log.w(TAG, "Error updating document", e)
                                                                        }
                                                                } else {
                                                                    Toast.makeText(this, "El groupId introducido no es válido", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Log.w(TAG, "Error getting document", e)
                                                            }
                                                    } else {
                                                        Toast.makeText(this, "Por favor, introduce tu groupId", Toast.LENGTH_SHORT).show()
                                                    }
                                                    dialog.dismiss()
                                                }

                                                builder.show()
                                            } else {
                                                val intent = Intent(this, HomeActivity::class.java)
                                                startActivity(intent)
                                            }
                                        } else {
                                            Toast.makeText(baseContext, "Inicio de sesión fallido. Error con la base de datos.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error getting document", e)
                                    }
                            } else {
                                Toast.makeText(baseContext, "Por favor, verifica tu correo electrónico antes de iniciar sesión", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(baseContext, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Porfavor rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Forgot password dialog
        forgotPassword.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Recuperar contraseña")

            val input = EditText(this)
            input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            input.hint = "Introduce tu correo electrónico"
            builder.setView(input)

            builder.setPositiveButton("Enviar") { dialog, _ ->
                val emailStr = input.text.toString()
                if (emailStr.isNotEmpty()) {
                    auth.sendPasswordResetEmail(emailStr)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(baseContext, "Correo de recuperación de contraseña enviado a $emailStr", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(baseContext, "Error al enviar correo de recuperación de contraseña", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(baseContext, "Por favor, introduce tu correo electrónico", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

            builder.show()
        }
    }

    // Handles the click on the return button
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}