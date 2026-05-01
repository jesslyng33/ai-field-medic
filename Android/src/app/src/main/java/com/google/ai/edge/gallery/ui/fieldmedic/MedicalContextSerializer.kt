package com.google.ai.edge.gallery.ui.fieldmedic

import com.google.ai.edge.gallery.database.relations.UserProfileWithDetails
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a [UserMedicalContext] from the full Room profile + current trip inputs.
 * Call this once after the trip screen completes, store in [AssessmentData.userContext].
 */
fun buildUserMedicalContext(
    profile: UserProfileWithDetails,
    tripLocation: String,
    soloTraveler: Boolean,
    firstAidKit: Set<String>,
): UserMedicalContext {
    val p = profile.profile

    val ageYears: Int? = if (p.dateOfBirth > 0) {
        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        val bornYear = Calendar.getInstance().apply { timeInMillis = p.dateOfBirth }.get(Calendar.YEAR)
        nowYear - bornYear
    } else null

    return UserMedicalContext(
        name = p.fullName,
        ageYears = ageYears,
        sex = p.biologicalSex,
        bloodType = p.bloodType,
        weightKg = p.weightKg,
        heightCm = p.heightCm,
        pregnancyStatus = p.pregnancyStatus,
        organDonor = p.organDonor,
        allergies = profile.allergies.map { AllergyContext(it.allergen, it.severity, it.reaction) },
        conditions = profile.conditions.map { it.conditionName },
        medications = profile.medications.map {
            MedicationContext(it.name, it.dosage, it.frequency, it.purpose)
        },
        implants = profile.implantDevices.map {
            buildString {
                append(it.deviceType)
                if (it.description.isNotBlank()) append(": ${it.description}")
            }
        },
        surgeries = profile.surgeries.map { s ->
            val year = if (s.date > 0)
                Calendar.getInstance().apply { timeInMillis = s.date }.get(Calendar.YEAR)
            else null
            SurgeryContext(s.procedure, year)
        },
        emergencyContact = profile.emergencyContacts
            .firstOrNull { it.isPrimary }
            ?.let { EmergencyContactContext(it.name, it.relationship, it.phoneNumber, it.canMakeMedicalDecisions) },
        healthcare = profile.healthcareInfo?.let { h ->
            HealthcareContext(
                physician = buildString {
                    if (h.primaryPhysicianName.isNotBlank()) append(h.primaryPhysicianName)
                    if (h.physicianPhone.isNotBlank()) append(" (${h.physicianPhone})")
                },
                preferredHospital = h.preferredHospital,
                dnrStatus = h.dnrStatus,
                advanceDirective = h.advanceDirectiveExists,
            )
        },
        tripLocation = tripLocation,
        soloTraveler = soloTraveler,
        firstAidKit = firstAidKit.sorted(),
    )
}

/**
 * Serializes the full medical context to a pretty-printed JSON string.
 * This is the canonical representation injected into Gemma prompts.
 */
fun UserMedicalContext.toJson(): String {
    val patient = JSONObject().apply {
        put("name", name.ifBlank { "Unknown" })
        ageYears?.let { put("age_years", it) }
        put("sex", sex.ifBlank { "unknown" })
        put("blood_type", bloodType.ifBlank { "unknown" })
        put("weight_kg", weightKg)
        put("height_cm", heightCm)
        if (pregnancyStatus) put("pregnancy_status", true)
        if (organDonor) put("organ_donor", true)
    }

    val allergiesArr = JSONArray().apply {
        allergies.forEach { a ->
            put(JSONObject().apply {
                put("allergen", a.allergen)
                put("severity", a.severity)
                put("reaction", a.reaction)
            })
        }
    }

    val conditionsArr = JSONArray().apply { conditions.forEach { put(it) } }

    val medsArr = JSONArray().apply {
        medications.forEach { m ->
            put(JSONObject().apply {
                put("name", m.name)
                if (m.dosage.isNotBlank()) put("dosage", m.dosage)
                if (m.frequency.isNotBlank()) put("frequency", m.frequency)
                if (m.purpose.isNotBlank()) put("purpose", m.purpose)
            })
        }
    }

    val root = JSONObject().apply {
        put("patient", patient)
        put("allergies", allergiesArr)
        put("conditions", conditionsArr)
        put("medications", medsArr)
        if (implants.isNotEmpty()) put("implants_devices", JSONArray().apply { implants.forEach { put(it) } })
        if (surgeries.isNotEmpty()) put("surgeries", JSONArray().apply {
            surgeries.forEach { s ->
                put(JSONObject().apply {
                    put("procedure", s.procedure)
                    s.yearApprox?.let { put("year", it) }
                })
            }
        })
        emergencyContact?.let { ec ->
            put("emergency_contact", JSONObject().apply {
                put("name", ec.name)
                put("relationship", ec.relationship)
                put("phone", ec.phone)
                put("can_make_medical_decisions", ec.canMakeMedicalDecisions)
            })
        }
        healthcare?.let { h ->
            put("healthcare", JSONObject().apply {
                if (h.physician.isNotBlank()) put("physician", h.physician)
                if (h.preferredHospital.isNotBlank()) put("preferred_hospital", h.preferredHospital)
                put("dnr_status", h.dnrStatus)
                put("advance_directive", h.advanceDirective)
            })
        }
        put("trip_context", JSONObject().apply {
            put("location", tripLocation.ifBlank { "unknown" })
            put("solo_traveler", soloTraveler)
            put("first_aid_kit", JSONArray().apply { firstAidKit.forEach { put(it) } })
        })
    }

    return root.toString(2)
}

/**
 * Wraps [toJson] in a clearly delimited block for embedding in Gemma system prompts.
 */
fun UserMedicalContext.toContextBlock(): String =
    "=== PATIENT MEDICAL RECORD (do NOT read aloud) ===\n${toJson()}\n=== END PATIENT RECORD ==="
