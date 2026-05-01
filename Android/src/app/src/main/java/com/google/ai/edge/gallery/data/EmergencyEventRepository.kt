package com.google.ai.edge.gallery.data

import com.google.ai.edge.gallery.database.dao.EmergencyEventDao
import com.google.ai.edge.gallery.database.entities.ActionLog
import com.google.ai.edge.gallery.database.entities.EmergencyEvent
import com.google.ai.edge.gallery.database.entities.EventOutcome
import com.google.ai.edge.gallery.database.entities.LlmInteractionLog
import com.google.ai.edge.gallery.database.entities.SymptomLog
import com.google.ai.edge.gallery.database.relations.EmergencyEventWithDetails
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class EmergencyEventRepository @Inject constructor(
    private val dao: EmergencyEventDao,
) {
    suspend fun startEvent(
        victimRelationship: String,
        eventType: String = "unknown",
        locationDescription: String = "",
        environmentContext: String = "outdoor",
    ): String {
        val eventId = UUID.randomUUID().toString()
        dao.insertEmergencyEvent(
            EmergencyEvent(
                eventId = eventId,
                userId = UserProfileRepository.PRIMARY_USER_ID,
                startTimestamp = System.currentTimeMillis(),
                endTimestamp = null,
                eventType = eventType,
                victimRelationship = victimRelationship,
                victimEstimatedAge = null,
                victimEstimatedWeightKg = null,
                locationLat = null,
                locationLng = null,
                locationDescription = locationDescription,
                environmentContext = environmentContext,
                called911 = false,
                call911Timestamp = null,
                estimatedEmsArrivalMinutes = null,
            )
        )
        return eventId
    }

    suspend fun updateEvent(event: EmergencyEvent) = dao.updateEmergencyEvent(event)

    fun observeEvent(eventId: String): Flow<EmergencyEvent?> = dao.getEventById(eventId)

    fun observeEventWithDetails(eventId: String): Flow<EmergencyEventWithDetails?> =
        dao.getEventWithDetails(eventId)

    fun observeActiveEvent(): Flow<EmergencyEvent?> =
        dao.getActiveEvent(UserProfileRepository.PRIMARY_USER_ID)

    fun observeAllEvents(): Flow<List<EmergencyEventWithDetails>> =
        dao.getAllEventsWithDetails(UserProfileRepository.PRIMARY_USER_ID)

    suspend fun logTriageSnapshot(
        eventId: String,
        consciousnessLevel: String?,
        bleedingSeverity: String?,
        breathingRate: Int?,
        heartRate: Int?,
        skinColor: String?,
        symptomDescription: String,
        severity: Int,
    ): Long = dao.insertSymptomLog(
        SymptomLog(
            eventId = eventId,
            timestamp = System.currentTimeMillis(),
            symptomDescription = symptomDescription,
            severity = severity,
            source = "user-reported",
            heartRate = heartRate,
            breathingRate = breathingRate,
            consciousnessLevel = consciousnessLevel,
            skinColor = skinColor,
            bleedingSeverity = bleedingSeverity,
        )
    )

    suspend fun logLlmTurn(
        eventId: String,
        userInput: String,
        modelResponse: String,
        modelConfidence: Float? = null,
    ): Long = dao.insertLlmInteraction(
        LlmInteractionLog(
            eventId = eventId,
            timestamp = System.currentTimeMillis(),
            userInput = userInput,
            modelResponse = modelResponse,
            modelConfidence = modelConfidence,
        )
    )

    suspend fun logAction(
        eventId: String,
        actionTaken: String,
        llmInteractionId: Long? = null,
    ): Long = dao.insertActionLog(
        ActionLog(
            eventId = eventId,
            timestamp = System.currentTimeMillis(),
            actionTaken = actionTaken,
            llmInteractionId = llmInteractionId,
        )
    )

    suspend fun closeEvent(
        eventId: String,
        outcome: String,
        handoffNotes: String,
        userFeedback: String? = null,
    ) {
        dao.insertEventOutcome(
            EventOutcome(
                eventId = eventId,
                outcome = outcome,
                handoffNotes = handoffNotes,
                userFeedback = userFeedback,
                reviewedByUser = false,
            )
        )
    }
}
