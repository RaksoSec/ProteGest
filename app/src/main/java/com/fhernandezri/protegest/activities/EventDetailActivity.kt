package com.fhernandezri.protegest.activities

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import com.fhernandezri.protegest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*

class EventDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val eventName: TextView = findViewById(R.id.eventName)
        val eventDescription: TextView = findViewById(R.id.eventDescription)
        val eventLocation: TextView = findViewById(R.id.eventLocation)
        val eventDate: TextView = findViewById(R.id.eventDate)
        val eventTime: TextView = findViewById(R.id.eventTime)
        val attendButton: Button = findViewById(R.id.attendButton)
        val notAttendButton: Button = findViewById(R.id.notAttendButton)
        val eventId = intent.getStringExtra("id")
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        if (eventId.isNullOrEmpty()) {
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val eventDocument = db.collection("events").document(eventId ?: "")

        eventDocument.get()
            .addOnSuccessListener { document ->
                eventName.text = document.getString("name")

                val description = SpannableStringBuilder()
                    .bold { append("Descripción: ") }
                    .append(document.getString("description"))
                eventDescription.text = description

                val date = document.getTimestamp("date")?.toDate()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val location = SpannableStringBuilder()
                    .bold { append("Lugar: ") }
                    .append(document.getString("place"))
                eventLocation.text = location

                val eventDateText = SpannableStringBuilder()
                    .bold { append("Fecha: ") }
                    .append(dateFormat.format(date))
                eventDate.text = eventDateText

                val eventTimeText = SpannableStringBuilder()
                    .bold { append("Hora: ") }
                    .append(timeFormat.format(date))
                eventTime.text = eventTimeText

                val usersJoined = document.get("usersJoined") as ArrayList<String>
                val usersDeclined = document.get("usersDeclined") as ArrayList<String>

                if (usersJoined.contains(userId)) {
                    attendButton.isEnabled = false
                    attendButton.setBackgroundResource(R.color.gray)
                    notAttendButton.setBackgroundResource(R.color.light_red)
                }

                if (usersDeclined.contains(userId)) {
                    notAttendButton.isEnabled = false
                    notAttendButton.setBackgroundResource(R.color.gray)
                    attendButton.setBackgroundResource(R.color.light_green)
                }
            }

        // Button for attending the event
        attendButton.setOnClickListener {
            eventDocument.get().addOnSuccessListener { document ->
                val usersDeclined = document.get("usersDeclined") as ArrayList<String>
                if (usersDeclined.contains(userId)) {
                    eventDocument.update("usersDeclined", FieldValue.arrayRemove(userId))
                    notAttendButton.isEnabled = true
                    notAttendButton.setBackgroundResource(R.color.light_red)
                }
                eventDocument.update("usersJoined", FieldValue.arrayUnion(userId))
                attendButton.isEnabled = false
                attendButton.setBackgroundResource(R.color.gray)
            }
        }

        // Button for not attending the event
        notAttendButton.setOnClickListener {
            eventDocument.get().addOnSuccessListener { document ->
                val usersJoined = document.get("usersJoined") as ArrayList<String>
                if (usersJoined.contains(userId)) {
                    eventDocument.update("usersJoined", FieldValue.arrayRemove(userId))
                    attendButton.isEnabled = true
                    attendButton.setBackgroundResource(R.color.light_green)
                }
                eventDocument.update("usersDeclined", FieldValue.arrayUnion(userId))
                notAttendButton.isEnabled = false
                notAttendButton.setBackgroundResource(R.color.gray)
            }
        }

        val viewAttendeesButton: Button = findViewById(R.id.viewAttendeesButton)
        viewAttendeesButton.setOnClickListener {
            val intent = Intent(this, AttendeesListActivity::class.java)
            intent.putExtra("eventId", eventId)
            startActivity(intent)
        }
    }

    fun goBack(view: View) {
        finish()
    }
}