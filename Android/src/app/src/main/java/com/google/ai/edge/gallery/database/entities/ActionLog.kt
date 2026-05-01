package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_log",
    foreignKeys = [
        ForeignKey(
            entity = EmergencyEvent::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LlmInteractionLog::class,
            parentColumns = ["id"],
            childColumns = ["llmInteractionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("eventId"), Index("llmInteractionId")]
)
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val timestamp: Long,
    val actionTaken: String,
    val llmInteractionId: Long?  // FK to the LLM turn that prompted this action
)
