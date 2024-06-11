package com.fhernandezri.protegest.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class Task(
    val id: String = "",
    val createdBy: String = "",
    val name: String = "",
    val description: String = "",
    val done: Boolean = false,
    val doneBy: String = "",
    val maxDate: Timestamp = Timestamp.now(),
    val groupId: String = "",
    val experience: Int = 100
)