package com.fhernandezri.protegest.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CalendarView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fhernandezri.protegest.models.Event
import com.fhernandezri.protegest.adapters.EventAdapter
import com.fhernandezri.protegest.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CalendarActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val db = FirebaseFirestore.getInstance()
        val eventsCollection = db.collection("events")
        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val eventListView: ListView = findViewById(R.id.eventListView)
        val user = FirebaseAuth.getInstance().currentUser

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        db.collection("users").document(user?.uid ?: "")
            .get()
            .addOnSuccessListener { document ->
                val groupId = document.getString("groupId")
                Log.d("CalendarActivity", "groupId: $groupId")

                calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    selectedDate.set(year, month, dayOfMonth)

                    val startOfDay = getStartOfDay(selectedDate.time)
                    val startOfNextDay = getStartOfNextDay(selectedDate.time)

                    Log.d("CalendarActivity", "startOfDay: $startOfDay")
                    Log.d("CalendarActivity", "startOfNextDay: $startOfNextDay")

                    eventsCollection
                        .whereEqualTo("groupId", groupId)
                        .whereGreaterThanOrEqualTo("date", startOfDay)
                        .whereLessThan("date", startOfNextDay)
                        .get()
                        .addOnSuccessListener { documents ->
                            Log.d("CalendarActivity", "documents: $documents")
                            val events = documents.map { document ->
                                Event(
                                    id = document.id,
                                    groupId = document.getString("groupId") ?: "",
                                    description = document.getString("description") ?: "",
                                    name = document.getString("name") ?: "",
                                    date = document.getTimestamp("date") ?: Timestamp.now(),
                                    usersJoined = document.get("usersJoined") as ArrayList<String>,
                                    usersDeclined = document.get("usersDeclined") as ArrayList<String>
                                )
                            }
                            val adapter = EventAdapter(this, R.layout.list_item_event, events)
                            eventListView.adapter = adapter
                            if (events.isNotEmpty()) {
                                Log.d("CalendarActivity", "ID documento: ${events[0].id}")
                            } else {
                                Log.d("CalendarActivity", "No events for this date")
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("CalendarActivity", "Error getting documents: ", exception)
                        }

                    eventListView.setOnItemClickListener { parent, _, position, _ ->
                        val selectedEvent = parent.getItemAtPosition(position) as Event
                        Log.d("CalendarActivity", "Id selectedEvent: ${selectedEvent.id}")
                        val intent = Intent(this, EventDetailActivity::class.java)
                        intent.putExtra("id", selectedEvent.id)
                        startActivity(intent)
                    }
                }

            }
    }

    // When the user clicks the return button on the action bar, finish the activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getStartOfDay(date: Date): Timestamp {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    private fun getStartOfNextDay(date: Date): Timestamp {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return Timestamp(calendar.time)
    }

    fun goBack(view: View) {
        finish()
    }
}
