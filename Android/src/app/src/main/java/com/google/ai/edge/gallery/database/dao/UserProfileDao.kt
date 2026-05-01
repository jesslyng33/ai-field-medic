package com.google.ai.edge.gallery.database.dao

import androidx.room.*
import com.google.ai.edge.gallery.database.entities.*
import com.google.ai.edge.gallery.database.relations.UserProfileWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Delete
    suspend fun deleteUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Transaction
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun getUserProfileWithDetails(userId: String): Flow<UserProfileWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicalCondition(condition: MedicalCondition)

    @Delete
    suspend fun deleteMedicalCondition(condition: MedicalCondition)

    @Query("SELECT * FROM medical_condition WHERE userId = :userId")
    fun getConditionsForUser(userId: String): Flow<List<MedicalCondition>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllergy(allergy: Allergy)

    @Delete
    suspend fun deleteAllergy(allergy: Allergy)

    @Query("SELECT * FROM allergy WHERE userId = :userId")
    fun getAllergiesForUser(userId: String): Flow<List<Allergy>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM medication WHERE userId = :userId")
    fun getMedicationsForUser(userId: String): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurgery(surgery: Surgery)

    @Delete
    suspend fun deleteSurgery(surgery: Surgery)

    @Query("SELECT * FROM surgery WHERE userId = :userId ORDER BY date DESC")
    fun getSurgeriesForUser(userId: String): Flow<List<Surgery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImplantDevice(device: ImplantDevice)

    @Delete
    suspend fun deleteImplantDevice(device: ImplantDevice)

    @Query("SELECT * FROM implant_device WHERE userId = :userId")
    fun getImplantDevicesForUser(userId: String): Flow<List<ImplantDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(contact: EmergencyContact)

    @Update
    suspend fun updateEmergencyContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteEmergencyContact(contact: EmergencyContact)

    @Query("SELECT * FROM emergency_contact WHERE userId = :userId ORDER BY isPrimary DESC")
    fun getEmergencyContactsForUser(userId: String): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contact WHERE userId = :userId AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryContact(userId: String): EmergencyContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthcareInfo(info: HealthcareInfo)

    @Update
    suspend fun updateHealthcareInfo(info: HealthcareInfo)

    @Query("SELECT * FROM healthcare_info WHERE userId = :userId")
    fun getHealthcareInfo(userId: String): Flow<HealthcareInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccessibilityNeeds(needs: AccessibilityNeeds)

    @Update
    suspend fun updateAccessibilityNeeds(needs: AccessibilityNeeds)

    @Query("SELECT * FROM accessibility_needs WHERE userId = :userId")
    fun getAccessibilityNeeds(userId: String): Flow<AccessibilityNeeds?>
}
