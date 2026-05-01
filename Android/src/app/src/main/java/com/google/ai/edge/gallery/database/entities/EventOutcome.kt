package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_outcome",
    foreignKeys = [ForeignKey(
        entity = EmergencyEvent::class,
        parentColumns = ["eventId"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EventOutcome(
    @PrimaryKey val eventId: String,
    val outcome: String,       // resolved / transported / fatality / ongoing
    val handoffNotes: String,
    val userFeedback: String?,
    val reviewedByUser: Boolean
)
