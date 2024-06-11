package com.fhernandezri.protegest.models

data class User(
    val id: String,
    val email: String,
    val groupId: String,
    val name: String,
    val role: String,
    val rank: String,
    val experience: Int,
    val tasksDone: Int,
    val eventsDone: Int
)