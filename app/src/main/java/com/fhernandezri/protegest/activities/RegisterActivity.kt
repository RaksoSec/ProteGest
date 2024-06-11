package com.fhernandezri.protegest.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fhernandezri.protegest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.fhernandezri.protegest.utils.UserInputValidator


class RegisterActivity : AppCompatActivity() {
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var confirmEmail: EditText
    private lateinit var password: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var code: EditText
    private lateinit var signUpButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enables return button on the action bar

        // Get all values from the form
        name = findViewById(R.id.name)
        email = findViewById(R.id.email)
        confirmEmail = findViewById(R.id.confirmEmail)
        password = findViewById(R.id.password)
        confirmPassword = findViewById(R.id.confirmPassword)
        code = findViewById(R.id.code)
        signUpButton = findViewById(R.id.signUpButton)

        auth = FirebaseAuth.getInstance()       // Firebase Authentication
        db = FirebaseFirestore.getInstance()    // Firebase Firestore

        // Register logic
        signUpButton.setOnClickListener {
            val emailStr = email.text.toString()
            val confirmEmailStr = confirmEmail.text.toString()
            val passwordStr = password.text.toString()
            val confirmPasswordStr = confirmPassword.text.toString()
            val codeStr = code.text.toString()
            val nameStr = name.text.toString()
            val userInputValidator = UserInputValidator()

            if (emailStr.isNotEmpty() && passwordStr.isNotEmpty() && codeStr.isNotEmpty() && nameStr.isNotEmpty()) {
                if (userInputValidator.areEmailsMatching(emailStr, confirmEmailStr) && userInputValidator.arePasswordsMatching(passwordStr, confirmPasswordStr)) {
                    // Check if groupId exists in the database
                    db.collection("groups").document(codeStr)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                // groupId exists, proceed with registration
                                auth.createUserWithEmailAndPassword(emailStr, passwordStr)
                                    .addOnCompleteListener(this) { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser     // Get the user from Firebase Authentication
                                            val userId = user?.uid          // Get the user id from Firebase Authentication

                                            // Create a new document in Firestore
                                            val userDocument = hashMapOf(
                                                "id" to userId,
                                                "email" to emailStr,
                                                "groupId" to codeStr,
                                                "name" to nameStr,
                                                "role" to "Standard",   // Default value for role
                                                "rank" to "Sin rango",
                                                "experience" to 0,
                                                "tasksDone" to 0,
                                                "eventsDone" to 0
                                            )

                                            db.collection("users")
                                                .document(userId!!)
                                                .set(userDocument)
                                                .addOnSuccessListener {
                                                    Log.d(TAG, "DocumentSnapshot successfully written!")
                                                    user?.sendEmailVerification()
                                                        ?.addOnCompleteListener { emailVerificationTask ->
                                                            if (emailVerificationTask.isSuccessful) {
                                                                val intent = Intent(this, EmailVerificationActivity::class.java)
                                                                startActivity(intent)
                                                            } else {
                                                                Toast.makeText(baseContext, "No se pudo enviar el correo de verificación", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(TAG, "Error writing document", e)
                                                }
                                        } else {
                                            Toast.makeText(baseContext, "Registro fallido", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(baseContext, "El código de grupo no existe",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error getting documents: ", exception)
                        }
                } else {
                    Toast.makeText(baseContext, "Los correos o contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(baseContext, "Porfavor rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
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