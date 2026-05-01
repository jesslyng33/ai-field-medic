package com.google.ai.edge.gallery.ui.fieldmedic.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.UserProfileRepository
import com.google.ai.edge.gallery.database.entities.Allergy
import com.google.ai.edge.gallery.database.entities.EmergencyContact
import com.google.ai.edge.gallery.database.entities.MedicalCondition
import com.google.ai.edge.gallery.database.entities.Medication
import com.google.ai.edge.gallery.database.relations.UserProfileWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: UserProfileRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfileWithDetails?> = repo.profileWithDetails
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateVitals(weightKg: Float, heightCm: Float, bloodType: String) {
        val current = profile.value?.profile ?: return
        viewModelScope.launch {
            repo.upsertProfile(
                fullName = current.fullName,
                dateOfBirth = current.dateOfBirth,
                biologicalSex = current.biologicalSex,
                weightKg = weightKg,
                heightCm = heightCm,
                bloodType = bloodType,
                preferredLanguage = current.preferredLanguage,
                pregnancyStatus = current.pregnancyStatus,
                dueDate = current.dueDate,
                organDonor = current.organDonor,
            )
        }
    }

    fun updateIdentity(fullName: String, dateOfBirth: Long, biologicalSex: String) {
        val current = profile.value?.profile ?: return
        viewModelScope.launch {
            repo.upsertProfile(
                fullName = fullName,
                dateOfBirth = dateOfBirth,
                biologicalSex = biologicalSex,
                weightKg = current.weightKg,
                heightCm = current.heightCm,
                bloodType = current.bloodType,
                preferredLanguage = current.preferredLanguage,
                pregnancyStatus = current.pregnancyStatus,
                dueDate = current.dueDate,
                organDonor = current.organDonor,
            )
        }
    }

    fun addAllergy(allergen: String, severity: String, reaction: String) {
        viewModelScope.launch { repo.addAllergy(allergen, severity, reaction) }
    }

    fun deleteAllergy(allergy: Allergy) {
        viewModelScope.launch { repo.deleteAllergy(allergy) }
    }

    fun addCondition(name: String) {
        viewModelScope.launch { repo.addCondition(name) }
    }

    fun deleteCondition(condition: MedicalCondition) {
        viewModelScope.launch { repo.deleteCondition(condition) }
    }

    fun addMedication(name: String, dosage: String, frequency: String, purpose: String) {
        viewModelScope.launch { repo.addMedication(name, dosage, frequency, purpose) }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch { repo.deleteMedication(medication) }
    }

    fun upsertEmergencyContact(
        existing: EmergencyContact?,
        name: String,
        relationship: String,
        phoneNumber: String,
        canMakeMedicalDecisions: Boolean,
    ) {
        viewModelScope.launch {
            repo.upsertEmergencyContact(
                id = existing?.id ?: 0,
                name = name,
                relationship = relationship,
                phoneNumber = phoneNumber,
                isPrimary = true,
                canMakeMedicalDecisions = canMakeMedicalDecisions,
            )
        }
    }
}
