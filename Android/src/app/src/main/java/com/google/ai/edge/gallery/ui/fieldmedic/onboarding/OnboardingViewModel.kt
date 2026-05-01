package com.google.ai.edge.gallery.ui.fieldmedic.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class AllergyDraft(
    val allergen: String,
    val severity: String,
    val reaction: String,
)

data class MedicationDraft(
    val name: String,
    val dosage: String,
    val frequency: String,
    val purpose: String,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: UserProfileRepository,
) : ViewModel() {

    var fullName by mutableStateOf("")
    var dateOfBirthMillis by mutableStateOf<Long?>(null)
    var biologicalSex by mutableStateOf("")

    var weightKg by mutableStateOf(70f)
    var heightCm by mutableStateOf(170f)
    var bloodType by mutableStateOf("")

    var pregnancyStatus by mutableStateOf(false)
    var organDonor by mutableStateOf(false)

    val allergies = mutableStateListOf<AllergyDraft>()
    val conditions = mutableStateListOf<String>()
    val medications = mutableStateListOf<MedicationDraft>()

    var contactName by mutableStateOf("")
    var contactRelationship by mutableStateOf("")
    var contactPhone by mutableStateOf("")
    var contactCanDecide by mutableStateOf(true)

    val identityComplete: Boolean
        get() = fullName.isNotBlank() && dateOfBirthMillis != null && biologicalSex.isNotBlank()

    val contactComplete: Boolean
        get() = contactName.isNotBlank() && contactPhone.isNotBlank()

    fun addAllergy(draft: AllergyDraft) {
        allergies.add(draft)
    }

    fun removeAllergyAt(index: Int) {
        if (index in allergies.indices) allergies.removeAt(index)
    }

    fun toggleCondition(condition: String) {
        if (condition in conditions) conditions.remove(condition) else conditions.add(condition)
    }

    fun addCustomCondition(name: String) {
        if (name.isNotBlank() && name !in conditions) conditions.add(name.trim())
    }

    fun addMedication(draft: MedicationDraft) {
        medications.add(draft)
    }

    fun removeMedicationAt(index: Int) {
        if (index in medications.indices) medications.removeAt(index)
    }

    fun commit(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.upsertProfile(
                fullName = fullName,
                dateOfBirth = dateOfBirthMillis ?: 0L,
                biologicalSex = biologicalSex,
                weightKg = weightKg,
                heightCm = heightCm,
                bloodType = bloodType.ifBlank { "Unknown" },
                pregnancyStatus = pregnancyStatus,
                organDonor = organDonor,
            )
            allergies.forEach { repo.addAllergy(it.allergen, it.severity, it.reaction) }
            conditions.forEach { repo.addCondition(it) }
            medications.forEach {
                repo.addMedication(it.name, it.dosage, it.frequency, it.purpose)
            }
            if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                repo.upsertEmergencyContact(
                    name = contactName,
                    relationship = contactRelationship.ifBlank { "Other" },
                    phoneNumber = contactPhone,
                    isPrimary = true,
                    canMakeMedicalDecisions = contactCanDecide,
                )
            }
            onDone()
        }
    }
}
