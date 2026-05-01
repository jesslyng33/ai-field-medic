package com.google.ai.edge.gallery.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.google.ai.edge.gallery.database.dao.EmergencyEventDao
import com.google.ai.edge.gallery.database.dao.UserProfileDao
import com.google.ai.edge.gallery.database.entities.*

@Database(
    entities = [
        UserProfile::class,
        MedicalCondition::class,
        Allergy::class,
        Medication::class,
        Surgery::class,
        ImplantDevice::class,
        EmergencyContact::class,
        HealthcareInfo::class,
        AccessibilityNeeds::class,
        EmergencyEvent::class,
        SymptomLog::class,
        LlmInteractionLog::class,
        ActionLog::class,
        EventOutcome::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun emergencyEventDao(): EmergencyEventDao
}
