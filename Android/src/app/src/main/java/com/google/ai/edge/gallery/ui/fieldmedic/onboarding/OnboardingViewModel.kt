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
import kotlin.math.roundToInt
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

    var useMetric by mutableStateOf(false)

    var pregnancyStatus by mutableStateOf(false)
    var organDonor by mutableStateOf(false)

    val allergies = mutableStateListOf<AllergyDraft>()
    val conditions = mutableStateListOf<String>()
    val medications = mutableStateListOf<MedicationDraft>()

    var contactName by mutableStateOf("")
    var contactRelationship by mutableStateOf("")
    var contactRelationshipCustom by mutableStateOf("")
    var contactPhone by mutableStateOf("")
    var contactCanDecide by mutableStateOf(true)

    // Imperial display conversions
    val displayWeightLbs: Float get() = weightKg * 2.20462f
    val displayHeightFt: Int get() = (heightCm / 30.48f).toInt().coerceIn(3, 7)
    val displayHeightIn: Int get() = ((heightCm - displayHeightFt * 30.48f) / 2.54f).roundToInt().coerceIn(0, 11)

    fun setWeightFromLbs(lbs: Float) {
        weightKg = lbs / 2.20462f
    }

    fun setHeightFromFtIn(ft: Int, inches: Int) {
        heightCm = (ft * 12 + inches) * 2.54f
    }

    val identityComplete: Boolean
        get() = fullName.isNotBlank() && dateOfBirthMillis != null && biologicalSex.isNotBlank()

    val contactPhoneValid: Boolean
        get() {
            val digits = contactPhone.filter { it.isDigit() }
            return when {
                contactPhone.startsWith("+") && contactPhone.length >= 8 -> true
                digits.length == 10 -> true
                digits.length == 11 && digits.startsWith("1") -> true
                else -> false
            }
        }

    val contactComplete: Boolean
        get() = contactName.isNotBlank() && contactPhone.isNotBlank() && contactPhoneValid

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

    fun removeCondition(name: String) {
        conditions.remove(name)
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
                val resolvedRelationship = if (contactRelationship == "Other") {
                    contactRelationshipCustom.ifBlank { "Other" }
                } else {
                    contactRelationship.ifBlank { "Other" }
                }
                repo.upsertEmergencyContact(
                    name = contactName,
                    relationship = resolvedRelationship,
                    phoneNumber = contactPhone,
                    isPrimary = true,
                    canMakeMedicalDecisions = contactCanDecide,
                )
            }
            onDone()
        }
    }
}
