package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emergency_event",
    foreignKeys = [ForeignKey(
        entity = UserProfile::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class EmergencyEvent(
    @PrimaryKey val eventId: String,
    val userId: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val eventType: String,              // cardiac / trauma / allergic / respiratory / neurological / unknown
    val victimRelationship: String,     // self / stranger / family / child
    val victimEstimatedAge: Int?,
    val victimEstimatedWeightKg: Float?,
    val locationLat: Double?,
    val locationLng: Double?,
    val locationDescription: String,
    val environmentContext: String,     // indoor / outdoor / vehicle / water / remote
    val called911: Boolean,
    val call911Timestamp: Long?,
    val estimatedEmsArrivalMinutes: Int?
)
