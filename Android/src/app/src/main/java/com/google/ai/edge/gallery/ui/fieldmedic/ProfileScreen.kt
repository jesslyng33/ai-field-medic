package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.database.entities.Allergy
import com.google.ai.edge.gallery.database.entities.EmergencyContact
import com.google.ai.edge.gallery.database.entities.MedicalCondition
import com.google.ai.edge.gallery.database.entities.Medication
import com.google.ai.edge.gallery.database.relations.UserProfileWithDetails
import com.google.ai.edge.gallery.ui.fieldmedic.settings.SettingsViewModel
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val profile by vm.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = FMTextSub)
            }
            Spacer(Modifier.size(4.dp))
            Text(
                "MEDICAL PROFILE",
                color = FMText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                letterSpacing = 3.sp,
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // AI Summary card
            AiSummaryCard(profile = profile)

            Spacer(Modifier.height(20.dp))

            // Identity card
            if (profile != null) {
                IdentityCard(profile = profile!!)
                Spacer(Modifier.height(16.dp))
            }

            // Allergies
            MedicalSection(
                icon = Icons.Filled.Warning,
                title = "ALLERGIES",
                iconTint = FMRedBright,
            ) {
                val allergies = profile?.allergies.orEmpty()
                if (allergies.isEmpty()) {
                    EmptyRow("No allergies recorded")
                } else {
                    allergies.forEach { allergy ->
                        AllergyRow(allergy)
                        HorizontalDivider(color = FMDivider, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Conditions
            MedicalSection(
                icon = Icons.Filled.LocalHospital,
                title = "CONDITIONS",
            ) {
                val conditions = profile?.conditions.orEmpty()
                if (conditions.isEmpty()) {
                    EmptyRow("No conditions recorded")
                } else {
                    conditions.forEach { condition ->
                        ConditionRow(condition)
                        HorizontalDivider(color = FMDivider, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Medications
            MedicalSection(
                icon = Icons.Filled.LocalPharmacy,
                title = "MEDICATIONS",
            ) {
                val meds = profile?.medications.orEmpty()
                if (meds.isEmpty()) {
                    EmptyRow("No medications recorded")
                } else {
                    meds.forEach { med ->
                        MedicationRow(med)
                        HorizontalDivider(color = FMDivider, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Emergency contact
            MedicalSection(
                icon = Icons.Filled.Phone,
                title = "EMERGENCY CONTACT",
            ) {
                val contact = profile?.emergencyContacts
                    ?.firstOrNull { it.isPrimary }
                    ?: profile?.emergencyContacts?.firstOrNull()
                if (contact == null) {
                    EmptyRow("No emergency contact set")
                } else {
                    ContactRow(contact)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AiSummaryCard(profile: UserProfileWithDetails?) {
    val anim = rememberInfiniteTransition(label = "summaryPulse")
    val shimmerAlpha by anim.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FMCard)
            .border(1.dp, FMRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(FMRed.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = FMRed,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "AI SUMMARY",
                    color = FMRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (profile == null) {
                // Loading shimmer lines
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (idx == 2) 0.6f else 1f)
                            .height(12.dp)
                            .graphicsLayer { alpha = shimmerAlpha }
                            .background(FMBorder, RoundedCornerShape(6.dp)),
                    )
                    if (idx < 2) Spacer(Modifier.height(8.dp))
                }
            } else {
                val summary = buildProfileSummary(profile)
                Text(
                    summary,
                    color = FMText,
                    fontSize = 14.sp,
                    fontFamily = appFontFamily,
                    lineHeight = 22.sp,
                )
                // TODO: Replace with live Gemma LLM inference when model is available on-device.
                // Pattern: create a FieldMedicViewModel scoped to this screen, call runInference()
                // with the summary text as prompt, observe inferenceState and llmResponse,
                // then display the streaming response here instead of the static summary.
                Spacer(Modifier.height(8.dp))
                Text(
                    "Static summary — live Gemma inference not yet connected",
                    color = FMTextSub,
                    fontSize = 11.sp,
                    fontFamily = appFontFamily,
                )
            }
        }
    }
}

private fun buildProfileSummary(profile: UserProfileWithDetails): String {
    val p = profile.profile
    val parts = mutableListOf<String>()

    // Identity
    val name = p.fullName.takeIf { it.isNotBlank() } ?: "Unknown patient"
    val age = if (p.dateOfBirth > 0) ageFromMillis(p.dateOfBirth).toString() + " years old" else null
    val sex = p.biologicalSex.takeIf { it.isNotBlank() }
    val blood = p.bloodType.takeIf { it.isNotBlank() }?.let { "blood type $it" }

    val identityParts = listOfNotNull(age, sex, blood)
    if (identityParts.isNotEmpty()) {
        parts.add("$name is a ${identityParts.joinToString(", ")}.")
    } else {
        parts.add("$name.")
    }

    // Vitals
    if (p.weightKg > 0 || p.heightCm > 0) {
        val vitals = mutableListOf<String>()
        if (p.weightKg > 0) vitals.add("${p.weightKg.roundToInt()} kg")
        if (p.heightCm > 0) vitals.add("${p.heightCm.roundToInt()} cm")
        parts.add("Vitals: ${vitals.joinToString(", ")}.")
    }

    // Allergies
    when (profile.allergies.size) {
        0 -> {} // omit
        1 -> parts.add("Has 1 known allergy: ${profile.allergies[0].allergen} (${profile.allergies[0].severity}).")
        else -> {
            val names = profile.allergies.joinToString(", ") { it.allergen }
            val severe = profile.allergies.filter { it.severity in listOf("severe", "life-threatening") }
            val severeNote = if (severe.isNotEmpty()) " — ${severe.joinToString(", ") { it.allergen }} are severe/life-threatening" else ""
            parts.add("Has ${profile.allergies.size} known allergies: $names$severeNote.")
        }
    }

    // Conditions
    when (profile.conditions.size) {
        0 -> {} // omit
        1 -> parts.add("Medical condition: ${profile.conditions[0].conditionName}.")
        else -> parts.add("Conditions: ${profile.conditions.joinToString(", ") { it.conditionName }}.")
    }

    // Medications
    when (profile.medications.size) {
        0 -> {}
        1 -> parts.add("Takes ${profile.medications[0].name}${if (profile.medications[0].dosage.isNotBlank()) " (${profile.medications[0].dosage})" else ""}.")
        else -> parts.add("Takes ${profile.medications.size} medications: ${profile.medications.joinToString(", ") { it.name }}.")
    }

    // Emergency contact
    val contact = profile.emergencyContacts.firstOrNull { it.isPrimary }
        ?: profile.emergencyContacts.firstOrNull()
    if (contact != null) {
        parts.add("Emergency contact: ${contact.name} (${contact.relationship}) — ${contact.phoneNumber}.")
    }

    return if (parts.isEmpty()) "No medical profile data available." else parts.joinToString(" ")
}

@Composable
private fun IdentityCard(profile: UserProfileWithDetails) {
    val p = profile.profile
    val age = if (p.dateOfBirth > 0) ageFromMillis(p.dateOfBirth) else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FMSurface)
            .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(FMRed.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, null, tint = FMRed, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    p.fullName.ifBlank { "Unknown" },
                    color = FMText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                )
                Spacer(Modifier.height(2.dp))
                val sub = listOfNotNull(
                    age?.let { "$it yrs" },
                    p.biologicalSex.takeIf { it.isNotBlank() },
                    p.bloodType.takeIf { it.isNotBlank() }?.let { "Type $it" },
                ).joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(sub, color = FMTextSub, fontSize = 13.sp, fontFamily = appFontFamily)
                }
            }
        }
        // Stats row
        if (p.weightKg > 0 || p.heightCm > 0) {
            Spacer(Modifier.height(12.dp))
        }
    }
    // Weight / height chips below if present
    if (p.weightKg > 0 || p.heightCm > 0) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (p.weightKg > 0) {
                ProfileStatChip(label = "WEIGHT", value = "${p.weightKg.roundToInt()} kg")
            }
            if (p.heightCm > 0) {
                ProfileStatChip(label = "HEIGHT", value = "${p.heightCm.roundToInt()} cm")
            }
            if (p.bloodType.isNotBlank()) {
                ProfileStatChip(label = "BLOOD TYPE", value = p.bloodType)
            }
        }
    }
}

@Composable
private fun ProfileStatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(FMCard)
            .border(1.dp, FMBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = FMTextSub,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = appFontFamily,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = FMText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = appFontFamily,
        )
    }
}

@Composable
private fun MedicalSection(
    icon: ImageVector,
    title: String,
    iconTint: androidx.compose.ui.graphics.Color = FMTextSub,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FMSurface)
            .border(1.dp, FMBorder, RoundedCornerShape(14.dp)),
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(FMCard, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                color = FMTextSub,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
                letterSpacing = 1.5.sp,
            )
        }
        HorizontalDivider(color = FMDivider)
        content()
    }
}

@Composable
private fun AllergyRow(allergy: Allergy) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(severityColor(allergy.severity), CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                allergy.allergen,
                color = FMText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
            )
            Text(
                "${allergy.severity} · ${allergy.reaction}",
                color = FMTextSub,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
            )
        }
    }
}

@Composable
private fun ConditionRow(condition: MedicalCondition) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(FMTextSub, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            condition.conditionName,
            color = FMText,
            fontSize = 14.sp,
            fontFamily = appFontFamily,
        )
    }
}

@Composable
private fun MedicationRow(medication: Medication) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                medication.name,
                color = FMText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
            )
            val sub = listOf(medication.dosage, medication.frequency, medication.purpose)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily)
            }
        }
    }
}

@Composable
private fun ContactRow(contact: EmergencyContact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.name,
                color = FMText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
            )
            Text(
                "${contact.relationship} · ${contact.phoneNumber}",
                color = FMTextSub,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
            )
            if (contact.canMakeMedicalDecisions) {
                Text(
                    "Healthcare proxy",
                    color = FMGreenBright,
                    fontSize = 11.sp,
                    fontFamily = appFontFamily,
                )
            }
        }
    }
}

@Composable
private fun EmptyRow(text: String) {
    Text(
        text,
        color = FMTextSub,
        fontSize = 13.sp,
        fontFamily = appFontFamily,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

private fun severityColor(severity: String) = when (severity.lowercase()) {
    "life-threatening" -> FMRedBright
    "severe" -> FMRed
    "moderate" -> androidx.compose.ui.graphics.Color(0xFFFFA000)
    else -> FMGreenBright
}

private fun ageFromMillis(millis: Long): Int {
    val nowYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()).toInt()
    val bornYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(millis)).toInt()
    return nowYear - bornYear
}
