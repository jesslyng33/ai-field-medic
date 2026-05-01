package com.google.ai.edge.gallery.database.dao

import androidx.room.*
import com.google.ai.edge.gallery.database.entities.*
import com.google.ai.edge.gallery.database.relations.EmergencyEventWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyEvent(event: EmergencyEvent)

    @Update
    suspend fun updateEmergencyEvent(event: EmergencyEvent)

    @Query("SELECT * FROM emergency_event WHERE eventId = :eventId")
    fun getEventById(eventId: String): Flow<EmergencyEvent?>

    @Query("SELECT * FROM emergency_event WHERE userId = :userId ORDER BY startTimestamp DESC")
    fun getEventsForUser(userId: String): Flow<List<EmergencyEvent>>

    @Query("SELECT * FROM emergency_event WHERE userId = :userId AND endTimestamp IS NULL ORDER BY startTimestamp DESC LIMIT 1")
    fun getActiveEvent(userId: String): Flow<EmergencyEvent?>

    @Transaction
    @Query("SELECT * FROM emergency_event WHERE eventId = :eventId")
    fun getEventWithDetails(eventId: String): Flow<EmergencyEventWithDetails?>

    @Transaction
    @Query("SELECT * FROM emergency_event WHERE userId = :userId ORDER BY startTimestamp DESC")
    fun getAllEventsWithDetails(userId: String): Flow<List<EmergencyEventWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomLog(log: SymptomLog): Long

    @Query("SELECT * FROM symptom_log WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun getSymptomLogsForEvent(eventId: String): Flow<List<SymptomLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLlmInteraction(log: LlmInteractionLog): Long

    @Query("SELECT * FROM llm_interaction_log WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun getLlmInteractionsForEvent(eventId: String): Flow<List<LlmInteractionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLog): Long

    @Query("SELECT * FROM action_log WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun getActionLogsForEvent(eventId: String): Flow<List<ActionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventOutcome(outcome: EventOutcome)

    @Update
    suspend fun updateEventOutcome(outcome: EventOutcome)

    @Query("SELECT * FROM event_outcome WHERE eventId = :eventId")
    fun getEventOutcome(eventId: String): Flow<EventOutcome?>
}
