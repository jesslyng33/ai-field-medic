package com.google.ai.edge.gallery.ui.fieldmedic

/** Full snapshot of a user's medical profile + current trip context. */
data class UserMedicalContext(
    // Identity & vitals
    val name: String,
    val ageYears: Int?,
    val sex: String,
    val bloodType: String,
    val weightKg: Float,
    val heightCm: Float,
    val pregnancyStatus: Boolean,
    val organDonor: Boolean,
    // Medical history
    val allergies: List<AllergyContext>,
    val conditions: List<String>,
    val medications: List<MedicationContext>,
    val implants: List<String>,
    val surgeries: List<SurgeryContext>,
    // Support
    val emergencyContact: EmergencyContactContext?,
    val healthcare: HealthcareContext?,
    // Per-trip (set each session)
    val tripLocation: String,
    val soloTraveler: Boolean,
    val firstAidKit: List<String>,
)

data class AllergyContext(
    val allergen: String,
    val severity: String,
    val reaction: String,
)

data class MedicationContext(
    val name: String,
    val dosage: String,
    val frequency: String,
    val purpose: String,
)

data class SurgeryContext(
    val procedure: String,
    val yearApprox: Int?,
)

data class EmergencyContactContext(
    val name: String,
    val relationship: String,
    val phone: String,
    val canMakeMedicalDecisions: Boolean,
)

data class HealthcareContext(
    val physician: String,
    val preferredHospital: String,
    val dnrStatus: Boolean,
    val advanceDirective: Boolean,
)
