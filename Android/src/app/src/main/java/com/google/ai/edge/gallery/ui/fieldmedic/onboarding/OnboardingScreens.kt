package com.google.ai.edge.gallery.ui.fieldmedic.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.fieldmedic.FMBackground
import com.google.ai.edge.gallery.ui.fieldmedic.FMBorder
import com.google.ai.edge.gallery.ui.fieldmedic.FMCard
import com.google.ai.edge.gallery.ui.fieldmedic.FMDivider
import com.google.ai.edge.gallery.ui.fieldmedic.FMGreen
import com.google.ai.edge.gallery.ui.fieldmedic.FMGreenBright
import com.google.ai.edge.gallery.ui.fieldmedic.FMPrimaryButton
import com.google.ai.edge.gallery.ui.fieldmedic.FMRed
import com.google.ai.edge.gallery.ui.fieldmedic.FMRedBright
import com.google.ai.edge.gallery.ui.fieldmedic.FMSectionLabel
import com.google.ai.edge.gallery.ui.fieldmedic.FMSurface
import com.google.ai.edge.gallery.ui.fieldmedic.FMText
import com.google.ai.edge.gallery.ui.fieldmedic.FMTextField
import com.google.ai.edge.gallery.ui.fieldmedic.FMTextSub
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val TOTAL_STEPS = 7

@Composable
fun OnboardingWelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .background(FMBackground)
            .fillMaxWidth()
            .padding(28.dp),
    ) {
        Spacer(Modifier.height(96.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(FMRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, null, tint = FMRed, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "FIELD\nMEDIC",
            color = FMText,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            fontFamily = appFontFamily,
            lineHeight = 56.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "AI-powered first aid that works when nothing else does.",
            color = FMTextSub,
            fontSize = 16.sp,
            fontFamily = appFontFamily,
            lineHeight = 24.sp,
        )

        Spacer(Modifier.height(48.dp))
        ValueProp(Icons.Filled.WifiOff, "WORKS WITH NO SIGNAL", "100% offline. No cell, no Wi-Fi, no problem.")
        Spacer(Modifier.height(20.dp))
        ValueProp(Icons.Filled.Lock, "STAYS ON THIS DEVICE", "Your medical info never leaves your phone.")
        Spacer(Modifier.height(20.dp))
        ValueProp(Icons.Filled.Warning, "BUILT FOR THE WORST DAY", "Trained for trauma, bleeding, and unconsciousness.")

        Spacer(Modifier.height(56.dp))
        FMPrimaryButton("GET STARTED", onClick = onContinue)
        Spacer(Modifier.height(12.dp))
        Text(
            "Takes ~2 minutes. You can edit anything later.",
            color = FMTextSub,
            fontSize = 12.sp,
            fontFamily = appFontFamily,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ValueProp(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(FMSurface, RoundedCornerShape(12.dp))
                .border(1.dp, FMBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = FMRed, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.size(16.dp))
        Column {
            Text(
                title,
                color = FMText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(body, color = FMTextSub, fontSize = 13.sp, fontFamily = appFontFamily, lineHeight = 18.sp)
        }
    }
}

@Composable
fun OnboardingIdentityScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val sexOptions = listOf("Male", "Female", "Intersex", "Prefer not")

    OnboardingStep(
        title = "WHO ARE\nYOU?",
        subtitle = "Private. On-device only.",
        step = 1,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = {
            OnboardingNavBar(
                onBack = onBack,
                onContinue = onContinue,
                continueEnabled = vm.identityComplete,
            )
        },
        content = {
            FMSectionLabel("FULL NAME")
            Spacer(Modifier.height(10.dp))
            FMTextField(value = vm.fullName, onValueChange = { vm.fullName = it }, placeholder = "First and last name")

            Spacer(Modifier.height(28.dp))
            FMSectionLabel("DATE OF BIRTH")
            Spacer(Modifier.height(10.dp))
            DateField(
                millis = vm.dateOfBirthMillis,
                onClick = { showDatePicker = true },
            )

            Spacer(Modifier.height(28.dp))
            FMSectionLabel("BIOLOGICAL SEX")
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = sexOptions,
                selected = vm.biologicalSex,
                onSelect = { vm.biologicalSex = it },
            )
        },
    )

    if (showDatePicker) {
        SimpleYearPicker(
            initialMillis = vm.dateOfBirthMillis,
            onDismiss = { showDatePicker = false },
            onPick = {
                vm.dateOfBirthMillis = it
                showDatePicker = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingVitalsScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val bloodTypes = listOf("A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−", "Unknown")

    OnboardingStep(
        title = "QUICK\nVITALS",
        subtitle = "Approximate is fine. We use this to scale dosage advice.",
        step = 2,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = { OnboardingNavBar(onBack = onBack, onContinue = onContinue) },
        content = {
            ValueSlider(
                label = "WEIGHT",
                value = vm.weightKg,
                onChange = { vm.weightKg = it },
                range = 30f..200f,
                unit = "kg",
            )
            Spacer(Modifier.height(28.dp))
            ValueSlider(
                label = "HEIGHT",
                value = vm.heightCm,
                onChange = { vm.heightCm = it },
                range = 100f..230f,
                unit = "cm",
            )
            Spacer(Modifier.height(28.dp))
            FMSectionLabel("BLOOD TYPE")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bloodTypes.forEach { type ->
                    ChoiceChip(
                        text = type,
                        selected = vm.bloodType == type,
                        onClick = { vm.bloodType = if (vm.bloodType == type) "" else type },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingAllergiesScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }

    OnboardingStep(
        title = "ANY\nALLERGIES?",
        subtitle = "Tag severity. Gemma uses this in an emergency.",
        step = 3,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = {
            OnboardingNavBar(
                onBack = onBack,
                onContinue = onContinue,
                continueLabel = if (vm.allergies.isEmpty()) "SKIP" else "CONTINUE",
            )
        },
        content = {
            vm.allergies.forEachIndexed { index, allergy ->
                AllergyRow(
                    allergen = allergy.allergen,
                    severity = allergy.severity,
                    reaction = allergy.reaction,
                    onRemove = { vm.removeAllergyAt(index) },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (vm.allergies.isEmpty()) {
                EmptyHint("None — that's fine")
                Spacer(Modifier.height(12.dp))
            }
            OutlinedAddButton(text = "ADD ALLERGY", onClick = { sheetOpen = true })
        },
    )

    if (sheetOpen) {
        AllergyDialog(
            onDismiss = { sheetOpen = false },
            onSave = { allergen, severity, reaction ->
                vm.addAllergy(AllergyDraft(allergen, severity, reaction))
                sheetOpen = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingConditionsScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val common = listOf(
        "Diabetes", "Asthma", "Heart disease", "Seizure disorder",
        "Hemophilia", "COPD", "Stroke history", "High blood pressure",
    )
    var customOpen by remember { mutableStateOf(false) }

    OnboardingStep(
        title = "ANY\nCONDITIONS?",
        subtitle = "Tap any that apply. These change how Gemma responds.",
        step = 4,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = {
            OnboardingNavBar(
                onBack = onBack,
                onContinue = onContinue,
                continueLabel = if (vm.conditions.isEmpty()) "SKIP" else "CONTINUE",
            )
        },
        content = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                common.forEach { condition ->
                    ChoiceChip(
                        text = condition,
                        selected = condition in vm.conditions,
                        onClick = { vm.toggleCondition(condition) },
                    )
                }
                ChoiceChip(text = "+ Other", selected = false, onClick = { customOpen = true })
            }
        },
    )

    if (customOpen) {
        TextEntryDialog(
            title = "Other condition",
            placeholder = "e.g. Lupus",
            onDismiss = { customOpen = false },
            onConfirm = {
                vm.addCustomCondition(it)
                customOpen = false
            },
        )
    }
}

@Composable
fun OnboardingMedicationsScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }

    OnboardingStep(
        title = "MEDICATIONS",
        subtitle = "Especially important: blood thinners, insulin, EpiPens.",
        step = 5,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = {
            OnboardingNavBar(
                onBack = onBack,
                onContinue = onContinue,
                continueLabel = if (vm.medications.isEmpty()) "SKIP" else "CONTINUE",
            )
        },
        content = {
            vm.medications.forEachIndexed { index, med ->
                MedicationRow(
                    med = med,
                    onRemove = { vm.removeMedicationAt(index) },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (vm.medications.isEmpty()) {
                EmptyHint("None — that's fine")
                Spacer(Modifier.height(12.dp))
            }
            OutlinedAddButton(text = "ADD MEDICATION", onClick = { sheetOpen = true })
        },
    )

    if (sheetOpen) {
        MedicationDialog(
            onDismiss = { sheetOpen = false },
            onSave = {
                vm.addMedication(it)
                sheetOpen = false
            },
        )
    }
}

@Composable
fun OnboardingContactScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val rels = listOf("Spouse", "Parent", "Sibling", "Friend", "Other")

    OnboardingStep(
        title = "EMERGENCY\nCONTACT",
        subtitle = "One trusted person. Required.",
        step = 6,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = {
            OnboardingNavBar(
                onBack = onBack,
                onContinue = onContinue,
                continueEnabled = vm.contactComplete,
            )
        },
        content = {
            FMSectionLabel("FULL NAME")
            Spacer(Modifier.height(10.dp))
            FMTextField(value = vm.contactName, onValueChange = { vm.contactName = it }, placeholder = "Name")
            Spacer(Modifier.height(20.dp))
            FMSectionLabel("PHONE")
            Spacer(Modifier.height(10.dp))
            FMTextField(value = vm.contactPhone, onValueChange = { vm.contactPhone = it }, placeholder = "+1 555 555 5555")
            Spacer(Modifier.height(20.dp))
            FMSectionLabel("RELATIONSHIP")
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = rels,
                selected = vm.contactRelationship,
                onSelect = { vm.contactRelationship = it },
            )
            Spacer(Modifier.height(20.dp))
            ToggleRow(
                title = "CAN MAKE MEDICAL DECISIONS",
                subtitle = "Healthcare proxy / power of attorney",
                value = vm.contactCanDecide,
                onChange = { vm.contactCanDecide = it },
            )
        },
    )
}

@Composable
fun OnboardingSummaryScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    OnboardingStep(
        title = "READY",
        subtitle = "Review and save. Edit anytime in Settings.",
        step = 7,
        total = TOTAL_STEPS,
        onBack = onBack,
        bottom = {
            OnboardingNavBar(
                onBack = onBack,
                onContinue = onFinish,
                continueLabel = "FINISH SETUP",
            )
        },
        content = {
            SummaryRow("Name", vm.fullName.ifBlank { "—" })
            SummaryRow(
                "DOB",
                vm.dateOfBirthMillis?.let { formatDate(it) } ?: "—",
            )
            SummaryRow("Sex", vm.biologicalSex.ifBlank { "—" })
            SummaryRow("Weight", "${vm.weightKg.roundToInt()} kg")
            SummaryRow("Height", "${vm.heightCm.roundToInt()} cm")
            SummaryRow("Blood type", vm.bloodType.ifBlank { "Unknown" })
            SummaryRow("Allergies", "${vm.allergies.size} saved")
            SummaryRow("Conditions", "${vm.conditions.size} saved")
            SummaryRow("Medications", "${vm.medications.size} saved")
            SummaryRow(
                "Emergency contact",
                if (vm.contactComplete) vm.contactName else "Not set",
            )
        },
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = FMTextSub, fontSize = 14.sp, fontFamily = appFontFamily)
        Text(value, color = FMText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = appFontFamily)
    }
    HorizontalDivider(color = FMDivider)
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) FMRed else FMSurface)
            .border(
                1.dp,
                if (selected) FMRed else FMBorder,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else FMTextSub,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = appFontFamily,
        )
    }
}

@Composable
private fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FMSurface)
            .padding(4.dp),
    ) {
        options.forEach { opt ->
            val isSel = selected == opt
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) FMRed else Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(vertical = 12.dp)
                    .weight(1f, fill = true),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    opt,
                    color = if (isSel) Color.White else FMTextSub,
                    fontSize = 12.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = appFontFamily,
                )
            }
        }
    }
}

@Composable
private fun ValueSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
) {
    Column {
        FMSectionLabel(label)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value.roundToInt().toString(),
                color = FMText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                unit,
                color = FMTextSub,
                fontSize = 16.sp,
                fontFamily = appFontFamily,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = FMRed,
                activeTrackColor = FMRed,
                inactiveTrackColor = FMBorder,
            ),
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FMSurface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = FMText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
                letterSpacing = 0.5.sp,
            )
            Text(subtitle, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FMText,
                checkedTrackColor = FMRed,
                uncheckedTrackColor = FMBorder,
                uncheckedThumbColor = FMTextSub,
            ),
        )
    }
}

@Composable
private fun OutlinedAddButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, FMBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = FMRed, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text,
            color = FMText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = appFontFamily,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        color = FMTextSub,
        fontSize = 14.sp,
        fontFamily = appFontFamily,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AllergyRow(
    allergen: String,
    severity: String,
    reaction: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FMCard)
            .border(1.dp, severityColor(severity).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(severityColor(severity), CircleShape),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(allergen, color = FMText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = appFontFamily)
            Text(
                "$severity · $reaction",
                color = FMTextSub,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, "Remove", tint = FMTextSub, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MedicationRow(med: MedicationDraft, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FMCard)
            .border(1.dp, FMBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(med.name, color = FMText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = appFontFamily)
            Text(
                listOf(med.dosage, med.frequency, med.purpose).filter { it.isNotBlank() }.joinToString(" · "),
                color = FMTextSub,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, "Remove", tint = FMTextSub, modifier = Modifier.size(18.dp))
        }
    }
}

private fun severityColor(severity: String): Color = when (severity.lowercase()) {
    "life-threatening" -> FMRedBright
    "severe" -> FMRed
    "moderate" -> Color(0xFFFFA000)
    else -> FMGreenBright
}

@Composable
private fun DateField(millis: Long?, onClick: () -> Unit) {
    val text = millis?.let { formatDate(it) } ?: "Tap to pick"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FMSurface)
            .border(1.dp, FMBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = if (millis == null) FMBorder else FMText,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(millis))

@Composable
private fun SimpleYearPicker(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    val cal = Calendar.getInstance().apply {
        if (initialMillis != null) timeInMillis = initialMillis
    }
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(cal.get(Calendar.MONTH) + 1) }
    var day by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMSurface,
        title = { Text("Date of Birth", color = FMText, fontFamily = appFontFamily) },
        text = {
            Column {
                NumberStepper("Year", year, 1900..Calendar.getInstance().get(Calendar.YEAR)) { year = it }
                Spacer(Modifier.height(12.dp))
                NumberStepper("Month", month, 1..12) { month = it }
                Spacer(Modifier.height(12.dp))
                NumberStepper("Day", day, 1..31) { day = it }
            }
        },
        confirmButton = {
            Text(
                "OK",
                color = FMRedBright,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                modifier = Modifier
                    .clickable {
                        val c = Calendar.getInstance()
                        c.set(year, month - 1, day, 0, 0, 0)
                        c.set(Calendar.MILLISECOND, 0)
                        onPick(c.timeInMillis)
                    }
                    .padding(12.dp),
            )
        },
        dismissButton = {
            Text(
                "CANCEL",
                color = FMTextSub,
                fontFamily = appFontFamily,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
            )
        },
    )
}

@Composable
private fun NumberStepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }) {
            Icon(Icons.Filled.Close, null, tint = FMTextSub, modifier = Modifier.size(18.dp))
        }
        Text(
            value.toString(),
            color = FMText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = appFontFamily,
            modifier = Modifier
                .background(FMCard, RoundedCornerShape(8.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }) {
            Icon(Icons.Filled.Add, null, tint = FMRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TextEntryDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMSurface,
        title = { Text(title, color = FMText, fontFamily = appFontFamily) },
        text = { FMTextField(value = text, onValueChange = { text = it }, placeholder = placeholder) },
        confirmButton = {
            Text(
                "ADD",
                color = FMRedBright,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                modifier = Modifier
                    .clickable { onConfirm(text) }
                    .padding(12.dp),
            )
        },
        dismissButton = {
            Text(
                "CANCEL",
                color = FMTextSub,
                fontFamily = appFontFamily,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergyDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var allergen by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("moderate") }
    var reaction by remember { mutableStateOf("") }

    val severities = listOf("mild", "moderate", "severe", "life-threatening")
    val reactions = listOf("Anaphylaxis", "Rash", "Hives", "Swelling", "Breathing", "GI", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMSurface,
        title = { Text("Allergy", color = FMText, fontFamily = appFontFamily) },
        text = {
            Column {
                FMSectionLabel("ALLERGEN")
                Spacer(Modifier.height(8.dp))
                FMTextField(value = allergen, onValueChange = { allergen = it }, placeholder = "e.g. Penicillin")
                Spacer(Modifier.height(16.dp))
                FMSectionLabel("SEVERITY")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    severities.forEach { s ->
                        ChoiceChip(text = s, selected = severity == s, onClick = { severity = s })
                    }
                }
                Spacer(Modifier.height(16.dp))
                FMSectionLabel("REACTION")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reactions.forEach { r ->
                        ChoiceChip(text = r, selected = reaction == r, onClick = { reaction = r })
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "SAVE",
                color = if (allergen.isNotBlank()) FMRedBright else FMBorder,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                modifier = Modifier
                    .clickable {
                        if (allergen.isNotBlank()) onSave(allergen, severity, reaction.ifBlank { "Unknown" })
                    }
                    .padding(12.dp),
            )
        },
        dismissButton = {
            Text(
                "CANCEL",
                color = FMTextSub,
                fontFamily = appFontFamily,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
            )
        },
    )
}

@Composable
private fun MedicationDialog(
    onDismiss: () -> Unit,
    onSave: (MedicationDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMSurface,
        title = { Text("Medication", color = FMText, fontFamily = appFontFamily) },
        text = {
            Column {
                FMSectionLabel("NAME")
                Spacer(Modifier.height(8.dp))
                FMTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Metformin")
                Spacer(Modifier.height(12.dp))
                FMSectionLabel("DOSAGE")
                Spacer(Modifier.height(8.dp))
                FMTextField(value = dosage, onValueChange = { dosage = it }, placeholder = "e.g. 500 mg")
                Spacer(Modifier.height(12.dp))
                FMSectionLabel("FREQUENCY")
                Spacer(Modifier.height(8.dp))
                FMTextField(value = frequency, onValueChange = { frequency = it }, placeholder = "e.g. Twice daily")
                Spacer(Modifier.height(12.dp))
                FMSectionLabel("PURPOSE (OPTIONAL)")
                Spacer(Modifier.height(8.dp))
                FMTextField(value = purpose, onValueChange = { purpose = it }, placeholder = "e.g. Diabetes")
            }
        },
        confirmButton = {
            Text(
                "SAVE",
                color = if (name.isNotBlank()) FMRedBright else FMBorder,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                modifier = Modifier
                    .clickable {
                        if (name.isNotBlank()) {
                            onSave(MedicationDraft(name, dosage, frequency, purpose))
                        }
                    }
                    .padding(12.dp),
            )
        },
        dismissButton = {
            Text(
                "CANCEL",
                color = FMTextSub,
                fontFamily = appFontFamily,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
            )
        },
    )
}
