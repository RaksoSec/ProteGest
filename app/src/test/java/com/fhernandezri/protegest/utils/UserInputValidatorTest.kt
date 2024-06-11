package com.fhernandezri.protegest.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserInputValidatorTest {

    private val userInputValidator = UserInputValidator()

    @Test
    fun `arePasswordsMatching returns true when passwords match`() {
        val result = userInputValidator.arePasswordsMatching("password123", "password123")
        assertTrue(result)
    }

    @Test
    fun `arePasswordsMatching returns false when passwords do not match`() {
        val result = userInputValidator.arePasswordsMatching("password123", "password124")
        assertFalse(result)
    }

    @Test
    fun `areEmailsMatching returns true when emails match`() {
        val result = userInputValidator.areEmailsMatching("test@example.com", "test@example.com")
        assertTrue(result)
    }

    @Test
    fun `areEmailsMatching returns false when emails do not match`() {
        val result = userInputValidator.areEmailsMatching("test@example.com", "test2@example.com")
        assertFalse(result)
    }
}