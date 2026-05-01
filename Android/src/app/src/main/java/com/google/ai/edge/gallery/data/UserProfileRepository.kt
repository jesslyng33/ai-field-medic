package com.google.ai.edge.gallery.data

import com.google.ai.edge.gallery.database.dao.UserProfileDao
import com.google.ai.edge.gallery.database.entities.AccessibilityNeeds
import com.google.ai.edge.gallery.database.entities.Allergy
import com.google.ai.edge.gallery.database.entities.EmergencyContact
import com.google.ai.edge.gallery.database.entities.HealthcareInfo
import com.google.ai.edge.gallery.database.entities.ImplantDevice
import com.google.ai.edge.gallery.database.entities.MedicalCondition
import com.google.ai.edge.gallery.database.entities.Medication
import com.google.ai.edge.gallery.database.entities.Surgery
import com.google.ai.edge.gallery.database.entities.UserProfile
import com.google.ai.edge.gallery.database.relations.UserProfileWithDetails
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
) {
    val profileWithDetails: Flow<UserProfileWithDetails?> =
        dao.getUserProfileWithDetails(PRIMARY_USER_ID)

    val profile: Flow<UserProfile?> = dao.getUserProfile(PRIMARY_USER_ID)
    val allergies: Flow<List<Allergy>> = dao.getAllergiesForUser(PRIMARY_USER_ID)
    val conditions: Flow<List<MedicalCondition>> = dao.getConditionsForUser(PRIMARY_USER_ID)
    val medications: Flow<List<Medication>> = dao.getMedicationsForUser(PRIMARY_USER_ID)
    val surgeries: Flow<List<Surgery>> = dao.getSurgeriesForUser(PRIMARY_USER_ID)
    val implants: Flow<List<ImplantDevice>> = dao.getImplantDevicesForUser(PRIMARY_USER_ID)
    val emergencyContacts: Flow<List<EmergencyContact>> =
        dao.getEmergencyContactsForUser(PRIMARY_USER_ID)
    val healthcareInfo: Flow<HealthcareInfo?> = dao.getHealthcareInfo(PRIMARY_USER_ID)
    val accessibilityNeeds: Flow<AccessibilityNeeds?> = dao.getAccessibilityNeeds(PRIMARY_USER_ID)

    suspend fun upsertProfile(
        fullName: String,
        dateOfBirth: Long,
        biologicalSex: String,
        weightKg: Float,
        heightCm: Float,
        bloodType: String,
        preferredLanguage: String = "en",
        pregnancyStatus: Boolean = false,
        dueDate: Long? = null,
        organDonor: Boolean = false,
    ) {
        dao.insertUserProfile(
            UserProfile(
                userId = PRIMARY_USER_ID,
                fullName = fullName,
                dateOfBirth = dateOfBirth,
                biologicalSex = biologicalSex,
                weightKg = weightKg,
                heightCm = heightCm,
                bloodType = bloodType,
                preferredLanguage = preferredLanguage,
                pregnancyStatus = pregnancyStatus,
                dueDate = dueDate,
                organDonor = organDonor,
            )
        )
    }

    suspend fun addAllergy(allergen: String, severity: String, reaction: String) {
        dao.insertAllergy(
            Allergy(userId = PRIMARY_USER_ID, allergen = allergen, severity = severity, reaction = reaction)
        )
    }

    suspend fun deleteAllergy(allergy: Allergy) = dao.deleteAllergy(allergy)

    suspend fun addCondition(name: String) {
        dao.insertMedicalCondition(MedicalCondition(userId = PRIMARY_USER_ID, conditionName = name))
    }

    suspend fun deleteCondition(condition: MedicalCondition) = dao.deleteMedicalCondition(condition)

    suspend fun addMedication(name: String, dosage: String, frequency: String, purpose: String) {
        dao.insertMedication(
            Medication(
                userId = PRIMARY_USER_ID,
                name = name,
                dosage = dosage,
                frequency = frequency,
                purpose = purpose,
            )
        )
    }

    suspend fun deleteMedication(medication: Medication) = dao.deleteMedication(medication)

    suspend fun addImplant(deviceType: String, description: String) {
        dao.insertImplantDevice(
            ImplantDevice(userId = PRIMARY_USER_ID, deviceType = deviceType, description = description)
        )
    }

    suspend fun deleteImplant(device: ImplantDevice) = dao.deleteImplantDevice(device)

    suspend fun addSurgery(procedure: String, date: Long) {
        dao.insertSurgery(Surgery(userId = PRIMARY_USER_ID, procedure = procedure, date = date))
    }

    suspend fun deleteSurgery(surgery: Surgery) = dao.deleteSurgery(surgery)

    suspend fun upsertEmergencyContact(
        id: Long = 0,
        name: String,
        relationship: String,
        phoneNumber: String,
        isPrimary: Boolean,
        canMakeMedicalDecisions: Boolean,
    ) {
        dao.insertEmergencyContact(
            EmergencyContact(
                id = id,
                userId = PRIMARY_USER_ID,
                name = name,
                relationship = relationship,
                phoneNumber = phoneNumber,
                isPrimary = isPrimary,
                canMakeMedicalDecisions = canMakeMedicalDecisions,
            )
        )
    }

    suspend fun deleteEmergencyContact(contact: EmergencyContact) =
        dao.deleteEmergencyContact(contact)

    suspend fun primaryContact(): EmergencyContact? = dao.getPrimaryContact(PRIMARY_USER_ID)

    suspend fun upsertHealthcareInfo(info: HealthcareInfo) {
        dao.insertHealthcareInfo(info.copy(userId = PRIMARY_USER_ID))
    }

    suspend fun upsertAccessibilityNeeds(needs: AccessibilityNeeds) {
        dao.insertAccessibilityNeeds(needs.copy(userId = PRIMARY_USER_ID))
    }

    companion object {
        const val PRIMARY_USER_ID = "primary_user"
    }
}
