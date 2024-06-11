package com.fhernandezri.protegest.utils

import com.fhernandezri.protegest.models.Achievement

class ExperienceCalculator {
    fun calculateExperience(achievements: List<Achievement>): Int {
        return achievements.sumOf { it.xp }
    }
}