package com.fhernandezri.protegest.activities

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fhernandezri.protegest.R
import com.fhernandezri.protegest.adapters.DeleteEventAdapter
import com.fhernandezri.protegest.adapters.DeleteVolunteerAdapter
import com.fhernandezri.protegest.adapters.EventAdapter
import com.fhernandezri.protegest.adapters.TaskAdapter
import com.fhernandezri.protegest.models.Event
import com.fhernandezri.protegest.models.Task
import com.fhernandezri.protegest.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import android.widget.ScrollView
import android.widget.Spinner
import com.fhernandezri.protegest.models.Achievement
import com.fhernandezri.protegest.utils.ExperienceCalculator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var groupId: String
    private var selectedPhotoUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // Allows the back button to appear with an icon
        val backButton = findViewById<Button>(R.id.backButton)
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        backButton.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)

        // Check that we have an user ID
        if (currentUser != null) {
            userId = currentUser.uid
        } else {
            finish()
            return
        }

        // Get the groupId of the user
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    groupId = document.getString("groupId") ?: ""
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

        // Button to get the code of the group
        val groupCodeButton: Button = findViewById(R.id.groupCodeButton)
        groupCodeButton.setOnClickListener {
            // Create an AlertDialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Código de grupo")
            builder.setMessage("El código de tu grupo es: $groupId")

            // Add an "Aceptar" button that closes the dialog when clicked
            builder.setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }

            // Show the AlertDialog
            builder.show()
        }

        // Button to add a new service
        val addServiceButton: Button = findViewById(R.id.addServiceButton)
        addServiceButton.setOnClickListener {
            // Create dialog
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_service, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Añadir servicio")
            builder.show()

            // Set all entry fields
            val dateEditText: EditText = dialogView.findViewById(R.id.dateEditText)
            val nameEditText: EditText = dialogView.findViewById(R.id.nameEditText)
            val descriptionEditText: EditText = dialogView.findViewById(R.id.descriptionEditText)
            val placeEditText: EditText = dialogView.findViewById(R.id.placeEditText)
            val addButton: Button = dialogView.findViewById(R.id.addButton)

            addButton.setOnClickListener {
                // Change the date to a timestamp
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = sdf.parse(dateEditText.text.toString())
                val timestamp = Timestamp(date)

                // Create the document
                val document = hashMapOf(
                    "date" to timestamp,
                    "name" to nameEditText.text.toString(),
                    "description" to descriptionEditText.text.toString(),
                    "groupId" to groupId,
                    "place" to placeEditText.text.toString(),
                    "usersDeclined" to arrayListOf(""),
                    "usersJoined" to arrayListOf("")
                )

                // Add the document to the database
                db.collection("events")
                    .add(document)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Servicio añadido correctamente", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al añadir el servicio", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        val serviceSignInButton: Button = findViewById(R.id.serviceSignInButton)
        serviceSignInButton.setOnClickListener {
            val db = FirebaseFirestore.getInstance()

            // Get all events and users from Firestore
            db.collection("events").whereEqualTo("groupId", groupId).get().addOnSuccessListener { eventsSnapshot ->
                val allEvents = eventsSnapshot.documents.map { doc ->
                    Event(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        groupId = doc.getString("groupId") ?: "",
                        date = doc.getTimestamp("date") ?: Timestamp.now(),
                        usersJoined = doc.get("usersJoined") as ArrayList<String>,
                        usersDeclined = doc.get("usersDeclined") as ArrayList<String>
                    )
                }

                // Create AlertDialog with Spinners
                val eventNames = allEvents.map { it.name }.toTypedArray()

                val eventSpinner = Spinner(this)
                eventSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, eventNames)

                val layout = LinearLayout(this)
                layout.orientation = LinearLayout.VERTICAL
                layout.addView(eventSpinner)

                AlertDialog.Builder(this)
                    .setTitle("Fichaje de servicio")
                    .setView(layout)
                    .setPositiveButton("OK") { _, _ ->
                        val selectedEvent = allEvents[eventSpinner.selectedItemPosition]

                        // Get all users who joined the selected event
                        db.collection("users").whereIn("id", selectedEvent.usersJoined).get().addOnSuccessListener { usersSnapshot ->
                            val allUsers = usersSnapshot.documents.map { doc ->
                                User(
                                    id = doc.getString("id") ?: "",
                                    email = doc.getString("email") ?: "",
                                    groupId = doc.getString("groupId") ?: "",
                                    name = doc.getString("name") ?: "",
                                    role = doc.getString("role") ?: "",
                                    rank = doc.getString("rank") ?: "",
                                    tasksDone = doc.getLong("tasksDone")?.toInt() ?: 0,
                                    experience = doc.getLong("experience")?.toInt() ?: 0,
                                    eventsDone = doc.getLong("eventsDone")?.toInt() ?: 0
                                )
                            }

                            // Create a multi-choice dialog for users
                            val userNames = allUsers.map { it.name }.toTypedArray()
                            val checkedItems = BooleanArray(userNames.size)

                            AlertDialog.Builder(this)
                                .setTitle("Selecciona los usuarios")
                                .setMultiChoiceItems(userNames, checkedItems) { _, _, _ -> }
                                .setPositiveButton("OK") { _, _ ->
                                    // Update the eventsDone and experience of each selected user
                                    for (i in checkedItems.indices) {
                                        if (checkedItems[i]) {
                                            val selectedUser = allUsers[i]
                                            val newEventsDone = selectedUser.eventsDone + 1
                                            val newExperience = selectedUser.experience + 100

                                            db.collection("users").document(selectedUser.id)
                                                .update(mapOf("eventsDone" to newEventsDone, "experience" to newExperience))
                                                .addOnSuccessListener {
                                                    Toast.makeText(this, "Fichaje realizado correctamente", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w(TAG, "Error updating document", e)
                                                }
                                        }
                                    }
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        val deleteServiceButton: Button = findViewById(R.id.deleteServiceButton)
        deleteServiceButton.setOnClickListener {
            // Pop up dialog
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_service, null)
            AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Eliminar servicio")
                .show()

            // Get events list
            val eventListView: ListView = dialogView.findViewById(R.id.eventListView)
            db.collection("events")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    val events = documents.map { document ->
                        Event(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            description = document.getString("description") ?: "",
                            groupId = document.getString("groupId") ?: "",
                            date = document.getTimestamp("date") ?: Timestamp.now(),
                            usersJoined = document.get("usersJoined") as ArrayList<String>,
                            usersDeclined = document.get("usersDeclined") as ArrayList<String>
                        )
                    }

                    // Adapter configuration
                    val adapter = DeleteEventAdapter(this, R.layout.delete_event_list, events)
                    eventListView.adapter = adapter

                    // Delete button listener
                    eventListView.setOnItemClickListener { parent, _, position, _ ->
                        val selectedEvent = parent.getItemAtPosition(position) as Event
                        db.collection("events").document(selectedEvent.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Servicio eliminado correctamente", Toast.LENGTH_SHORT).show()
                                (eventListView.adapter as EventAdapter).remove(selectedEvent)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al eliminar el servicio", Toast.LENGTH_SHORT).show()
                                Log.w(TAG, "Error deleting document", e)
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }

        // Button to delete a service
        val addTaskButton: Button = findViewById(R.id.addTaskButton)
        addTaskButton.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Añadir tarea")
            val alertDialog = builder.show()

            val addTaskDialogButton: Button = dialogView.findViewById(R.id.addTaskDialogButton)
            addTaskDialogButton.setOnClickListener {
                val taskNameEditText: EditText = dialogView.findViewById(R.id.taskNameEditText)
                val taskDescriptionEditText: EditText = dialogView.findViewById(R.id.taskDescriptionEditText)
                val taskMaxDatePicker: DatePicker = dialogView.findViewById(R.id.taskMaxDatePicker)

                val name = taskNameEditText.text.toString()
                val description = taskDescriptionEditText.text.toString()
                val calendar = Calendar.getInstance()
                calendar.set(taskMaxDatePicker.year, taskMaxDatePicker.month, taskMaxDatePicker.dayOfMonth)
                val maxDate = Timestamp(calendar.time)

                if (name.isNotEmpty() && description.isNotEmpty()) {
                    val task = hashMapOf(
                        "name" to name,
                        "description" to description,
                        "maxDate" to maxDate,
                        "groupId" to groupId,
                        "done" to false,
                        "doneBy" to "",
                        "createdBy" to userId,
                        "experience" to 100         // All tasks give 100 XP
                    )

                    db.collection("tasks")
                        .add(task)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Tarea creada correctamente", Toast.LENGTH_SHORT).show()
                            alertDialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al crear la tarea", Toast.LENGTH_SHORT).show()
                            Log.w(TAG, "Error adding document", e)
                        }
                } else {
                    Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val deleteTaskButton: Button = findViewById(R.id.deleteTaskButton)
        deleteTaskButton.setOnClickListener {
            // Pop up dialog
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_task, null)
            AlertDialog.Builder(this)
                .setView(dialogView)
                .setTitle("Eliminar tarea")
                .show()

            // Get tasks list
            val taskListView: ListView = dialogView.findViewById(R.id.taskListView)
            db.collection("tasks")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    val tasks = documents.map { document ->
                        Task(
                            id = document.id,
                            name = document.getString("name") ?: "",
                            description = document.getString("description") ?: "",
                            done = document.getBoolean("done") ?: false,
                            maxDate = document.getTimestamp("maxDate") ?: Timestamp.now(),
                            groupId = document.getString("groupId") ?: "",
                            experience = document.getLong("experience")?.toInt() ?: 0
                        )
                    }

                    // Adapter configuration
                    val adapter = TaskAdapter(this, R.layout.task_item, tasks)
                    taskListView.adapter = adapter

                    // Delete button listener
                    taskListView.setOnItemClickListener { parent, _, position, _ ->
                        val selectedTask = parent.getItemAtPosition(position) as Task
                        db.collection("tasks").document(selectedTask.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Tarea eliminada correctamente", Toast.LENGTH_SHORT).show()
                                (taskListView.adapter as TaskAdapter).remove(selectedTask)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al eliminar la tarea", Toast.LENGTH_SHORT).show()
                                Log.w(TAG, "Error deleting document", e)
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }

        val completeTaskButton: Button = findViewById(R.id.completeTaskButton)
        completeTaskButton.setOnClickListener {
            val db = FirebaseFirestore.getInstance()

            // Get all users and tasks from Firestore
            db.collection("users").whereEqualTo("groupId", groupId).get().addOnSuccessListener { usersSnapshot ->
                val allUsers = usersSnapshot.documents.map { doc ->
                    User(
                        id = doc.getString("id") ?: "",
                        email = doc.getString("email") ?: "",
                        groupId = doc.getString("groupId") ?: "",
                        name = doc.getString("name") ?: "",
                        role = doc.getString("role") ?: "",
                        rank = doc.getString("rank") ?: "",
                        tasksDone = doc.getLong("tasksDone")?.toInt() ?: 0,
                        experience = doc.getLong("experience")?.toInt() ?: 0,
                        eventsDone = doc.getLong("eventsDone")?.toInt() ?: 0
                    )
                }

                db.collection("tasks").whereEqualTo("groupId", groupId).whereEqualTo("done", false).get().addOnSuccessListener { tasksSnapshot ->
                    val allTasks = tasksSnapshot.documents.map { doc ->
                        Task(
                            id = doc.id,
                            createdBy = doc.getString("createdBy") ?: "",
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            done = doc.getBoolean("done") ?: false,
                            doneBy = doc.getString("doneBy") ?: "",
                            maxDate = doc.getTimestamp("maxDate") ?: Timestamp.now(),
                            groupId = doc.getString("groupId") ?: "",
                            experience = doc.getLong("experience")?.toInt() ?: 0
                        )
                    }

                    // Inflate the custom layout
                    val layout = LayoutInflater.from(this).inflate(R.layout.dialog_complete_task, null)

                    // Create AlertDialog with Spinners
                    val userNames = allUsers.map { it.name }.toTypedArray()
                    val taskNames = allTasks.map { it.name }.toTypedArray()

                    val userSpinner = layout.findViewById<Spinner>(R.id.userSpinner)
                    userSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)

                    val taskSpinner = layout.findViewById<Spinner>(R.id.taskSpinner)
                    taskSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taskNames)

                    AlertDialog.Builder(this)
                        .setTitle("Completar tarea")
                        .setView(layout)
                        .setPositiveButton("OK") { _, _ ->
                            val selectedUser = allUsers[userSpinner.selectedItemPosition]
                            val selectedTask = allTasks[taskSpinner.selectedItemPosition]

                            // Update the user's experience and tasksDone
                            val newExperience = selectedUser.experience + 100
                            val newTasksDone = selectedUser.tasksDone + 1

                            db.collection("users").document(selectedUser.id)
                                .update(mapOf("experience" to newExperience, "tasksDone" to newTasksDone))
                                .addOnSuccessListener {
                                    // Mark the task as done and update the "doneBy" field
                                    db.collection("tasks").document(selectedTask.id)
                                        .update(mapOf("done" to true, "doneBy" to selectedUser.id))
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Tarea completada correctamente", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(TAG, "Error updating document", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error updating document", e)
                                }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }

        val grantAchievementButton: Button = findViewById(R.id.grantAchievementButton)
        grantAchievementButton.setOnClickListener {
            val db = FirebaseFirestore.getInstance()

            // Get all achievements and users from Firestore
            db.collection("achievements").get().addOnSuccessListener { achievementsSnapshot ->
                val allAchievements = achievementsSnapshot.documents.map { doc ->
                    Achievement(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        xp = doc.getLong("xp")?.toInt() ?: 0
                    )
                }

                db.collection("users").whereEqualTo("groupId", groupId).get().addOnSuccessListener { usersSnapshot ->
                    val allUsers = usersSnapshot.documents.map { doc ->
                        User(
                            id = doc.getString("id") ?: "",
                            email = doc.getString("email") ?: "",
                            groupId = doc.getString("groupId") ?: "",
                            name = doc.getString("name") ?: "",
                            role = doc.getString("role") ?: "",
                            rank = doc.getString("rank") ?: "",
                            tasksDone = doc.getLong("tasksDone")?.toInt() ?: 0,
                            experience = doc.getLong("experience")?.toInt() ?: 0,
                            eventsDone = doc.getLong("eventsDone")?.toInt() ?: 0
                        )
                    }

                    // Create AlertDialog with Spinners
                    val achievementNames = allAchievements.map { it.name }.toTypedArray()
                    val userNames = allUsers.map { it.name }.toTypedArray()

                    val achievementSpinner = Spinner(this)
                    achievementSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, achievementNames)

                    val userSpinner = Spinner(this)
                    userSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)

                    val layout = LinearLayout(this)
                    layout.orientation = LinearLayout.VERTICAL
                    layout.addView(achievementSpinner)
                    layout.addView(userSpinner)

                    AlertDialog.Builder(this)
                        .setTitle("Otorgar logro")
                        .setView(layout)
                        .setPositiveButton("OK") { _, _ ->
                            val selectedAchievement = allAchievements[achievementSpinner.selectedItemPosition]
                            val selectedUser = allUsers[userSpinner.selectedItemPosition]

                            // Add the achievement to the user
                            db.collection("users").document(selectedUser.id)
                                .update("achievementsEarned", FieldValue.arrayUnion(selectedAchievement.id))
                                .addOnSuccessListener {
                                    // Get the updated list of achievement IDs
                                    db.collection("users").document(selectedUser.id)
                                        .get()
                                        .addOnSuccessListener { document ->
                                            val achievementsEarnedIds = (document.get("achievementsEarned") as List<*>).map { it.toString() }

                                            // Get the details of each achievement
                                            val achievementsEarned = mutableListOf<Achievement>()
                                            val achievementsCount = achievementsEarnedIds.size
                                            var achievementsFetched = 0

                                            for (achievementId in achievementsEarnedIds) {
                                                db.collection("achievements").document(achievementId)
                                                    .get()
                                                    .addOnSuccessListener { achievementDocument ->
                                                        val achievement = Achievement(
                                                            id = achievementDocument.id,
                                                            name = achievementDocument.getString("name") ?: "",
                                                            description = achievementDocument.getString("description") ?: "",
                                                            xp = achievementDocument.getLong("xp")?.toInt() ?: 0
                                                        )
                                                        achievementsEarned.add(achievement)

                                                        achievementsFetched++
                                                        if (achievementsFetched == achievementsCount) {
                                                            // Calculate the total experience
                                                            val experienceCalculator = ExperienceCalculator()
                                                            val totalExperience = experienceCalculator.calculateExperience(achievementsEarned)

                                                            // Update the user's experience in the database
                                                            db.collection("users").document(selectedUser.id)
                                                                .update("experience", totalExperience)
                                                                .addOnSuccessListener {
                                                                    Toast.makeText(this, "Logro otorgado correctamente", Toast.LENGTH_SHORT).show()
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    Log.w(TAG, "Error updating document", e)
                                                                }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.w(TAG, "Error getting document", e)
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(TAG, "Error getting document", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al otorgar el logro", Toast.LENGTH_SHORT).show()
                                    Log.w(TAG, "Error updating document", e)
                                }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
        }

        val deleteVolunteerButton: Button = findViewById(R.id.deleteVolunteerButton)
        deleteVolunteerButton.setOnClickListener {
            // Check if the user is an admin
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role")
                        if (role == "Admin") {
                            // Pop up dialog
                            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_volunteer, null)
                            AlertDialog.Builder(this)
                                .setView(dialogView)
                                .setTitle("Eliminar voluntario")
                                .show()

                            // Get volunteers list
                            val volunteerListView: ListView = dialogView.findViewById(R.id.volunteerListView)
                            db.collection("users")
                                .whereEqualTo("groupId", groupId)
                                .get()
                                .addOnSuccessListener { documents ->
                                    val volunteers = documents.map { document ->
                                        User(
                                            id = document.id,
                                            email = document.getString("email") ?: "",
                                            groupId = document.getString("groupId") ?: "",
                                            name = document.getString("name") ?: "",
                                            role = document.getString("role") ?: "",
                                            rank = document.getString("rank") ?: "",
                                            tasksDone = document.getLong("tasksDone")?.toInt() ?: 0,
                                            experience = document.getLong("experience")?.toInt() ?: 0,
                                            eventsDone = document.getLong("eventsDone")?.toInt() ?: 0
                                        )
                                    }.filter { it.id != userId } // Filter the list to exclude actual user

                                    // Adapter configuration
                                    val adapter = DeleteVolunteerAdapter(this, R.layout.item_delete_volunteer, volunteers)
                                    volunteerListView.adapter = adapter

                                    // Delete button listener
                                    volunteerListView.setOnItemClickListener { parent, _, position, _ ->
                                        val selectedVolunteer = parent.getItemAtPosition(position) as User
                                        (volunteerListView.adapter as DeleteVolunteerAdapter).remove(selectedVolunteer)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.w(TAG, "Error getting documents: ", exception)
                                }
                        } else {
                            Toast.makeText(this, "No tienes permisos para realizar esta acción", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error getting document", e)
                    }
            }
        }

        val modifyRoleVolunteerButton: Button = findViewById(R.id.modifyRoleVolunteerButton)
        modifyRoleVolunteerButton.setOnClickListener {
            // Get volunteers list
            db.collection("users")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    val volunteers = documents.mapNotNull { document ->
                        val role = document.getString("role") ?: ""
                        if (role != "Admin") {
                            User(
                                id = document.id,
                                email = document.getString("email") ?: "",
                                groupId = document.getString("groupId") ?: "",
                                name = document.getString("name") ?: "",
                                role = role,
                                rank = document.getString("rank") ?: "",
                                tasksDone = document.getLong("tasksDone")?.toInt() ?: 0,
                                experience = document.getLong("experience")?.toInt() ?: 0,
                                eventsDone = document.getLong("eventsDone")?.toInt() ?: 0
                            )
                        } else null
                    }

                    // For each volunteer, add a button to change their role
                    for (volunteer in volunteers) {
                        val inflater = LayoutInflater.from(this)
                        val dialogView = inflater.inflate(R.layout.dialog_modify_role, null)

                        val volunteerInfo: TextView = dialogView.findViewById(R.id.volunteerInfo)
                        volunteerInfo.text = "${volunteer.name} - ${volunteer.role}"

                        val changeRoleButton: Button = dialogView.findViewById(R.id.changeRoleButton)
                        changeRoleButton.setOnClickListener {
                            // When the button is clicked, show a selector with the roles "Standard" and "Coordinator"
                            val roles = arrayOf("Standard", "Coordinator")
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Selecciona un rol")
                            builder.setItems(roles) { _, which ->
                                // When a role is selected, update the volunteer's role in Firestore
                                db.collection("users").document(volunteer.id)
                                    .update("role", roles[which])
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Rol cambiado correctamente", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error updating document", e)
                                    }
                            }
                            builder.show()
                        }

                        // Create an AlertDialog
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Modificar rol de un voluntario")
                        builder.setView(dialogView)
                        builder.show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }

        val modifyRankVolunteerButton: Button = findViewById(R.id.modifyRankVolunteerButton)
        modifyRankVolunteerButton.setOnClickListener {
            // Create a ScrollView and a LinearLayout to hold the volunteer views
            val scrollView = ScrollView(this)
            val linearLayout = LinearLayout(this)
            linearLayout.orientation = LinearLayout.VERTICAL
            scrollView.addView(linearLayout)

            // Get volunteers list
            db.collection("users")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    val volunteers = documents.map { document ->
                        User(
                            id = document.id,
                            email = document.getString("email") ?: "",
                            groupId = document.getString("groupId") ?: "",
                            name = document.getString("name") ?: "",
                            role = document.getString("role") ?: "",
                            rank = document.getString("rank") ?: "",
                            tasksDone = document.getLong("tasksDone")?.toInt() ?: 0,
                            experience = document.getLong("experience")?.toInt() ?: 0,
                            eventsDone = document.getLong("eventsDone")?.toInt() ?: 0
                        )
                    }

                    // For each volunteer, inflate the view and add it to the LinearLayout
                    for (volunteer in volunteers) {
                        val inflater = LayoutInflater.from(this)
                        val volunteerView = inflater.inflate(R.layout.dialog_modify_rank, null)

                        val volunteerInfo: TextView = volunteerView.findViewById(R.id.volunteerInfo)
                        volunteerInfo.text = "${volunteer.name} - ${volunteer.rank}"

                        val changeRankButton: Button = volunteerView.findViewById(R.id.changeRankButton)
                        val rankEditText: EditText = volunteerView.findViewById(R.id.rankEditText)
                        changeRankButton.setOnClickListener {
                            // When the button is clicked, update the volunteer's rank in Firestore
                            db.collection("users").document(volunteer.id)
                                .update("rank", rankEditText.text.toString())
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Rango cambiado correctamente", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error updating document", e)
                                }
                        }

                        linearLayout.addView(volunteerView)
                    }

                    // Create an AlertDialog
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Modificar rango de un voluntario")
                    builder.setView(scrollView)
                    builder.show()
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }

        // Button to add a vehicle
        val addVehicleButton: Button = findViewById(R.id.addVehicleButton)
        addVehicleButton.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_add_vehicle, null)

            val nameEditText: EditText = dialogView.findViewById(R.id.nameEditText)
            val plateEditText: EditText = dialogView.findViewById(R.id.plateEditText)
            val selectPhotoButton: Button = dialogView.findViewById(R.id.selectPhotoButton)
            val submitButton: Button = dialogView.findViewById(R.id.submitButton)

            selectPhotoButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_IMAGE_REQUEST)
            }

            submitButton.setOnClickListener {
                val name = nameEditText.text.toString()
                val plate = plateEditText.text.toString()

                if (name.isEmpty() || plate.isEmpty()) {
                    Toast.makeText(this, "Por favor, rellena todos los campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val vehicle = hashMapOf(
                    "name" to name,
                    "plate" to plate,
                    "groupId" to groupId
                )

                FirebaseFirestore.getInstance().collection("vehicles")
                    .add(vehicle)
                    .addOnSuccessListener { documentReference ->
                        if (selectedPhotoUri != null) {
                            val storageReference = FirebaseStorage.getInstance().getReference("/vehicles/${documentReference.id}")
                            storageReference.putFile(selectedPhotoUri!!)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Vehículo y foto añadidos correctamente", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error al subir la foto", e)
                                }
                        } else {
                            Toast.makeText(this, "Vehículo añadido correctamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error al añadir el vehículo", e)
                    }
            }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Añadir vehículo")
            builder.setView(dialogView)
            builder.show()
        }

        // Button to delete a vehicle
        val deleteVehicleButton: Button = findViewById(R.id.deleteVehicleButton)
        deleteVehicleButton.setOnClickListener {
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.dialog_delete_vehicle, null)

            val vehiclesLayout: LinearLayout = dialogView.findViewById(R.id.vehiclesLayout)

            FirebaseFirestore.getInstance().collection("vehicles")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val vehicleId = document.id
                        val vehicleName = document.getString("name") ?: ""
                        val vehiclePlate = document.getString("plate") ?: ""

                        val vehicleView = layoutInflater.inflate(R.layout.item_delete_vehicle, null)
                        val vehicleNameTextView: TextView = vehicleView.findViewById(R.id.vehicleName)
                        val vehiclePlateTextView: TextView = vehicleView.findViewById(R.id.vehiclePlate)
                        val deleteButton: Button = vehicleView.findViewById(R.id.deleteVehicleButton)

                        vehicleNameTextView.text = vehicleName
                        vehiclePlateTextView.text = vehiclePlate

                        deleteButton.setOnClickListener {
                            FirebaseFirestore.getInstance().collection("vehicles").document(vehicleId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Vehículo eliminado correctamente", Toast.LENGTH_SHORT).show()
                                    vehiclesLayout.removeView(vehicleView)
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error al eliminar el vehículo", e)
                                }
                        }

                        vehiclesLayout.addView(vehicleView)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error al obtener los vehículos: ", exception)
                }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Eliminar vehículo")
            builder.setView(dialogView)
            builder.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedPhotoUri = data.data
        }
    }

    fun goBack(view: View) {
        finish()
    }
}