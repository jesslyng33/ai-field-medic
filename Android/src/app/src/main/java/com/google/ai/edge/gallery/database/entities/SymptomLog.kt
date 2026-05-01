package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "symptom_log",
    foreignKeys = [ForeignKey(
        entity = EmergencyEvent::class,
        parentColumns = ["eventId"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class SymptomLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val timestamp: Long,
    val symptomDescription: String,
    val severity: Int,                // 1-10
    val source: String,               // user-reported / sensor
    val heartRate: Int?,
    val breathingRate: Int?,
    val consciousnessLevel: String?,  // A / V / P / U  (AVPU scale)
    val skinColor: String?,
    val bleedingSeverity: String?     // none / minor / moderate / severe
)
