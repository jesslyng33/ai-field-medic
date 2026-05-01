package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emergency_contact",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val isPrimary: Boolean,
    val canMakeMedicalDecisions: Boolean
)
