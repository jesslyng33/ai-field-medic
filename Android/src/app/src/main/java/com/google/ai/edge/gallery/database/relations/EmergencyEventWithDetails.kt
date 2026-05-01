package com.google.ai.edge.gallery.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.google.ai.edge.gallery.database.entities.*

data class EmergencyEventWithDetails(
    @Embedded val event: EmergencyEvent,
    @Relation(parentColumn = "eventId", entityColumn = "eventId")
    val symptomLogs: List<SymptomLog>,
    @Relation(parentColumn = "eventId", entityColumn = "eventId")
    val actionLogs: List<ActionLog>,
    @Relation(parentColumn = "eventId", entityColumn = "eventId")
    val llmInteractions: List<LlmInteractionLog>,
    @Relation(parentColumn = "eventId", entityColumn = "eventId")
    val outcome: EventOutcome?
)
