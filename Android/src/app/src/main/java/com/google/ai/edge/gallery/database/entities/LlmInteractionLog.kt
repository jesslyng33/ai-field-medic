package com.google.ai.edge.gallery.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "llm_interaction_log",
    foreignKeys = [ForeignKey(
        entity = EmergencyEvent::class,
        parentColumns = ["eventId"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class LlmInteractionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val timestamp: Long,
    val userInput: String,
    val modelResponse: String,
    val modelConfidence: Float?
)
