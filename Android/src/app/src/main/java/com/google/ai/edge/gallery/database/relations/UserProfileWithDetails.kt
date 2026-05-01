package com.google.ai.edge.gallery.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.google.ai.edge.gallery.database.entities.*

data class UserProfileWithDetails(
    @Embedded val profile: UserProfile,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val conditions: List<MedicalCondition>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val allergies: List<Allergy>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val medications: List<Medication>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val surgeries: List<Surgery>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val implantDevices: List<ImplantDevice>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val emergencyContacts: List<EmergencyContact>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val healthcareInfo: HealthcareInfo?,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val accessibilityNeeds: AccessibilityNeeds?
)
