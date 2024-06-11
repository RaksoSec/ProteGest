package com.fhernandezri.protegest.utils

class UserInputValidator {
    fun arePasswordsMatching(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }

    fun areEmailsMatching(email: String, confirmEmail: String): Boolean {
        return email == confirmEmail
    }
}