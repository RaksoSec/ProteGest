package com.fhernandezri.protegest.models

import com.google.firebase.Timestamp

data class Event(
    val id: String,
    val groupId: String,
    val description: String,
    val name: String,
    val date: Timestamp,
    val usersJoined: ArrayList<String>,
    val usersDeclined: ArrayList<String>
)