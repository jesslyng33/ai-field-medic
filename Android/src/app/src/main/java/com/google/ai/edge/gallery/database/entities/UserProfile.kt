package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val userId: String,
    val fullName: String,
    val dateOfBirth: Long,
    val biologicalSex: String,
    val weightKg: Float,
    val heightCm: Float,
    val bloodType: String,
    val preferredLanguage: String,
    val pregnancyStatus: Boolean,
    val dueDate: Long?,
    val organDonor: Boolean
)
