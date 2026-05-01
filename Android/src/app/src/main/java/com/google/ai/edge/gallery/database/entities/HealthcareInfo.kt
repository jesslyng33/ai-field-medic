package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "healthcare_info",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class HealthcareInfo(
    @PrimaryKey val userId: String,
    val primaryPhysicianName: String,
    val physicianPhone: String,
    val preferredHospital: String,
    val insuranceProvider: String,
    val policyNumber: String,
    val advanceDirectiveExists: Boolean,
    val dnrStatus: Boolean
)
