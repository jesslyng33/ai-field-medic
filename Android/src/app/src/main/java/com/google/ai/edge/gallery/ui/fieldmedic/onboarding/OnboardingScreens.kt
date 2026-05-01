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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
            // Unit toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(FMSurface)
                    .padding(4.dp),
            ) {
                listOf("Imperial (lbs / ft)" to false, "Metric (kg / cm)" to true).forEach { (label, isMetric) ->
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (vm.useMetric == isMetric) FMRed else Color.Transparent)
                            .clickable { vm.useMetric = isMetric }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (vm.useMetric == isMetric) Color.White else FMTextSub,
                            fontSize = 12.sp,
                            fontWeight = if (vm.useMetric == isMetric) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = appFontFamily,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            if (vm.useMetric) {
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
            } else {
                ValueSlider(
                    label = "WEIGHT",
                    value = vm.displayWeightLbs,
                    onChange = { vm.setWeightFromLbs(it) },
                    range = 66f..440f,
                    unit = "lbs",
                )
                Spacer(Modifier.height(28.dp))
                // Imperial height
                FMSectionLabel("HEIGHT")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${vm.displayHeightFt}'",
                        color = FMText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${vm.displayHeightIn}\"",
                        color = FMText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                    )
                }
                Slider(
                    value = vm.displayHeightFt.toFloat(),
                    onValueChange = { vm.setHeightFromFtIn(it.toInt(), vm.displayHeightIn) },
                    valueRange = 3f..7f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = FMRed, activeTrackColor = FMRed, inactiveTrackColor = FMBorder),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Feet", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                    Text("${vm.displayHeightFt} ft", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                }
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = vm.displayHeightIn.toFloat(),
                    onValueChange = { vm.setHeightFromFtIn(vm.displayHeightFt, it.toInt()) },
                    valueRange = 0f..11f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = FMRed, activeTrackColor = FMRed, inactiveTrackColor = FMBorder),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Inches", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                    Text("${vm.displayHeightIn} in", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                }
            }

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

@Composable
fun OnboardingConditionsScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val commonConditions = listOf(
        "Diabetes", "Asthma", "Heart disease", "Seizure disorder",
        "Hemophilia", "COPD", "Stroke history", "High blood pressure",
        "Kidney disease", "Liver disease", "Cancer", "Thyroid disorder",
        "Anxiety disorder", "Depression", "HIV/AIDS", "Autism spectrum",
    )
    var query by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val suggestions = remember(query, vm.conditions.toList()) {
        if (query.length < 1) emptyList()
        else commonConditions.filter {
            it.contains(query, ignoreCase = true) && it !in vm.conditions
        }.take(6)
    }

    OnboardingStep(
        title = "ANY\nCONDITIONS?",
        subtitle = "These change how Gemma responds to your emergency.",
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
            Box {
                FMTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        dropdownExpanded = it.isNotBlank()
                    },
                    placeholder = "Search or type a condition...",
                )
                DropdownMenu(
                    expanded = dropdownExpanded && (suggestions.isNotEmpty() || query.isNotBlank()),
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FMCard),
                ) {
                    suggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion, color = FMText, fontSize = 14.sp, fontFamily = appFontFamily) },
                            onClick = {
                                vm.addCustomCondition(suggestion)
                                query = ""
                                dropdownExpanded = false
                            },
                        )
                    }
                    val trimmed = query.trim()
                    if (trimmed.isNotBlank() && trimmed !in vm.conditions && !commonConditions.any { it.equals(trimmed, ignoreCase = true) }) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Add, null, tint = FMRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Add \"$trimmed\"", color = FMRed, fontSize = 14.sp, fontFamily = appFontFamily)
                                }
                            },
                            onClick = {
                                vm.addCustomCondition(trimmed)
                                query = ""
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (vm.conditions.isEmpty()) {
                EmptyHint("None — that's fine")
            } else {
                vm.conditions.forEach { condition ->
                    ConditionRow(condition = condition, onRemove = { vm.removeCondition(condition) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
    )
}

@Composable
private fun ConditionRow(condition: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FMCard)
            .border(1.dp, FMBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Check, null, tint = FMGreenBright, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(10.dp))
        Text(condition, color = FMText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = appFontFamily, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, "Remove", tint = FMTextSub, modifier = Modifier.size(18.dp))
        }
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
            FMSectionLabel("FULL NAME  *")
            Spacer(Modifier.height(10.dp))
            FMTextField(value = vm.contactName, onValueChange = { vm.contactName = it }, placeholder = "Contact's full name")

            Spacer(Modifier.height(20.dp))
            FMSectionLabel("PHONE  *")
            Spacer(Modifier.height(10.dp))
            FMTextField(value = vm.contactPhone, onValueChange = { vm.contactPhone = it }, placeholder = "+1 555 555 5555 or 10-digit")
            if (vm.contactPhone.isNotBlank() && !vm.contactPhoneValid) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Enter a valid phone number (10 digits, or start with + for international)",
                    color = FMRedBright,
                    fontSize = 11.sp,
                    fontFamily = appFontFamily,
                    lineHeight = 15.sp,
                )
            }

            Spacer(Modifier.height(20.dp))
            FMSectionLabel("RELATIONSHIP  *")
            Spacer(Modifier.height(10.dp))
            Segmented(
                options = rels,
                selected = vm.contactRelationship,
                onSelect = { vm.contactRelationship = it },
            )
            if (vm.contactRelationship == "Other") {
                Spacer(Modifier.height(10.dp))
                FMTextField(
                    value = vm.contactRelationshipCustom,
                    onValueChange = { vm.contactRelationshipCustom = it },
                    placeholder = "e.g. Coach, Neighbor, Doctor",
                )
            }

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
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) FMRed else FMSurface)
            .border(1.dp, if (selected) FMRed else FMBorder, RoundedCornerShape(20.dp))
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
            textAlign = TextAlign.Center,
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
    var showEdit by remember { mutableStateOf(false) }
    var editText by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily, modifier = Modifier.weight(1f))
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }) {
            Icon(Icons.Filled.Remove, null, tint = FMTextSub, modifier = Modifier.size(18.dp))
        }
        Text(
            value.toString(),
            color = FMText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = appFontFamily,
            modifier = Modifier
                .background(FMCard, RoundedCornerShape(8.dp))
                .clickable {
                    editText = value.toString()
                    showEdit = true
                }
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }) {
            Icon(Icons.Filled.Add, null, tint = FMRed, modifier = Modifier.size(18.dp))
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            containerColor = FMSurface,
            title = { Text(label, color = FMText, fontFamily = appFontFamily) },
            text = {
                FMTextField(
                    value = editText,
                    onValueChange = { editText = it.filter { c -> c.isDigit() } },
                    placeholder = "${range.first}–${range.last}",
                )
            },
            confirmButton = {
                Text(
                    "OK",
                    color = FMRedBright,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    modifier = Modifier.clickable {
                        val parsed = editText.toIntOrNull()
                        if (parsed != null && parsed in range) onChange(parsed)
                        showEdit = false
                    }.padding(12.dp),
                )
            },
            dismissButton = {
                Text(
                    "CANCEL",
                    color = FMTextSub,
                    fontFamily = appFontFamily,
                    modifier = Modifier.clickable { showEdit = false }.padding(12.dp),
                )
            },
        )
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
    var reactionQuery by remember { mutableStateOf("") }
    var selectedReaction by remember { mutableStateOf("") }
    var showSeverityInfo by remember { mutableStateOf(false) }
    var showReactionInfo by remember { mutableStateOf(false) }
    var reactionDropdownExpanded by remember { mutableStateOf(false) }

    val severityDescriptions = mapOf(
        "mild" to "Minor, localized reactions (runny nose, mild rash). No danger signs.",
        "moderate" to "More widespread symptoms (hives, vomiting). May need antihistamines.",
        "severe" to "Multi-system symptoms (severe swelling, difficulty breathing). May need epinephrine.",
        "life-threatening" to "Anaphylaxis — airway swelling, blood pressure drop, shock. EpiPen required immediately.",
    )
    val reactionDescriptions = mapOf(
        "Anaphylaxis" to "Severe whole-body reaction with airway swelling and/or shock.",
        "Rash" to "Localized red, irritated skin.",
        "Hives" to "Raised, itchy welts anywhere on the body.",
        "Swelling" to "Swelling of face, lips, tongue, or throat (angioedema).",
        "Breathing difficulty" to "Wheezing, shortness of breath, or chest tightness.",
        "GI symptoms" to "Nausea, vomiting, diarrhea, or stomach cramps.",
        "Itching" to "Generalized or localized itching without visible rash.",
        "Flushing" to "Sudden redness and warmth of the skin.",
        "Dizziness" to "Lightheadedness or feeling faint.",
        "Throat tightening" to "Sensation of the throat closing (laryngeal edema).",
    )
    val reactionSuggestions = listOf(
        "Anaphylaxis", "Rash", "Hives", "Swelling", "Breathing difficulty",
        "GI symptoms", "Itching", "Flushing", "Dizziness", "Throat tightening",
    )
    val effectiveReaction = selectedReaction.ifBlank { reactionQuery.trim() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMSurface,
        title = { Text("Add Allergy", color = FMText, fontFamily = appFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                FMSectionLabel("ALLERGEN")
                Spacer(Modifier.height(8.dp))
                FMTextField(value = allergen, onValueChange = { allergen = it }, placeholder = "e.g. Penicillin, Peanuts")

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FMSectionLabel("SEVERITY")
                    Spacer(Modifier.size(6.dp))
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Severity info",
                        tint = FMTextSub,
                        modifier = Modifier.size(14.dp).clickable { showSeverityInfo = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Equal-width 2x2 severity grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("mild", severity == "mild", { severity = "mild" }, Modifier.weight(1f))
                        ChoiceChip("moderate", severity == "moderate", { severity = "moderate" }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("severe", severity == "severe", { severity = "severe" }, Modifier.weight(1f))
                        ChoiceChip("life-threatening", severity == "life-threatening", { severity = "life-threatening" }, Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FMSectionLabel("REACTION TYPE")
                    Spacer(Modifier.size(6.dp))
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Reaction info",
                        tint = FMTextSub,
                        modifier = Modifier.size(14.dp).clickable { showReactionInfo = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Reaction text field with autofill dropdown
                Box {
                    FMTextField(
                        value = reactionQuery,
                        onValueChange = { query ->
                            reactionQuery = query
                            selectedReaction = ""
                            reactionDropdownExpanded = query.isNotBlank()
                        },
                        placeholder = "Type a reaction...",
                    )
                    DropdownMenu(
                        expanded = reactionDropdownExpanded,
                        onDismissRequest = { reactionDropdownExpanded = false },
                        modifier = Modifier
                            .background(FMSurface)
                            .border(1.dp, FMBorder, RoundedCornerShape(8.dp)),
                    ) {
                        val query = reactionQuery.trim()
                        val filtered = reactionSuggestions.filter {
                            it.contains(query, ignoreCase = true)
                        }
                        filtered.forEach { suggestion ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        suggestion,
                                        color = FMText,
                                        fontSize = 14.sp,
                                        fontFamily = appFontFamily,
                                    )
                                },
                                onClick = {
                                    selectedReaction = suggestion
                                    reactionQuery = suggestion
                                    reactionDropdownExpanded = false
                                },
                            )
                        }
                        // "Add [typed text]" option if not already in list
                        if (query.isNotBlank() && filtered.none { it.equals(query, ignoreCase = true) }) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add \"$query\"",
                                        color = FMRedBright,
                                        fontSize = 14.sp,
                                        fontFamily = appFontFamily,
                                    )
                                },
                                onClick = {
                                    selectedReaction = query
                                    reactionQuery = query
                                    reactionDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
                // Selected reaction chip tag
                if (selectedReaction.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FMRed.copy(alpha = 0.15f))
                            .border(1.dp, FMRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            selectedReaction,
                            color = FMRedBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = appFontFamily,
                        )
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear reaction",
                            tint = FMRedBright,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    selectedReaction = ""
                                    reactionQuery = ""
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                "SAVE",
                color = if (allergen.isNotBlank() && effectiveReaction.isNotBlank()) FMRedBright else FMBorder,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                modifier = Modifier.clickable {
                    if (allergen.isNotBlank() && effectiveReaction.isNotBlank()) {
                        onSave(allergen, severity, effectiveReaction)
                    }
                }.padding(12.dp),
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

    if (showSeverityInfo) {
        AlertDialog(
            onDismissRequest = { showSeverityInfo = false },
            containerColor = FMSurface,
            title = { Text("Severity Guide", color = FMText, fontFamily = appFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    severityDescriptions.forEach { (level, desc) ->
                        Row {
                            Text(
                                level.replaceFirstChar { it.uppercase() },
                                color = severityColor(level),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = appFontFamily,
                                modifier = Modifier.width(110.dp),
                            )
                            Text(desc, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily, lineHeight = 17.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Text("GOT IT", color = FMRedBright, fontWeight = FontWeight.Bold, fontFamily = appFontFamily,
                    modifier = Modifier.clickable { showSeverityInfo = false }.padding(12.dp))
            },
        )
    }

    if (showReactionInfo) {
        AlertDialog(
            onDismissRequest = { showReactionInfo = false },
            containerColor = FMSurface,
            title = { Text("Reaction Types", color = FMText, fontFamily = appFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reactionDescriptions.forEach { (type, desc) ->
                        Row {
                            Text(
                                type,
                                color = FMText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = appFontFamily,
                                modifier = Modifier.width(130.dp),
                            )
                            Text(desc, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily, lineHeight = 17.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Text("GOT IT", color = FMRedBright, fontWeight = FontWeight.Bold, fontFamily = appFontFamily,
                    modifier = Modifier.clickable { showReactionInfo = false }.padding(12.dp))
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationDialog(
    onDismiss: () -> Unit,
    onSave: (MedicationDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var selectedFrequencies by remember { mutableStateOf(setOf<String>()) }
    var purpose by remember { mutableStateOf("") }

    val frequencyPresets = listOf("Morning", "Midday", "Evening", "Night", "As needed")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FMSurface,
        title = { Text("Add Medication", color = FMText, fontFamily = appFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Required name field
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FMSectionLabel("NAME")
                        Text(" *", color = FMRedBright, fontSize = 11.sp, fontFamily = appFontFamily)
                    }
                    Spacer(Modifier.height(6.dp))
                    FMTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Metformin, Lisinopril")
                }

                // Dosage
                Column {
                    FMSectionLabel("DOSAGE")
                    Spacer(Modifier.height(6.dp))
                    FMTextField(value = dosage, onValueChange = { dosage = it }, placeholder = "e.g. 500 mg, 10 mg")
                }

                // Frequency with presets
                Column {
                    FMSectionLabel("WHEN TAKEN")
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        frequencyPresets.forEach { preset ->
                            ChoiceChip(
                                text = preset,
                                selected = preset in selectedFrequencies,
                                onClick = {
                                    selectedFrequencies = if (preset in selectedFrequencies)
                                        selectedFrequencies - preset
                                    else
                                        selectedFrequencies + preset
                                },
                            )
                        }
                    }
                }

                // Purpose
                Column {
                    FMSectionLabel("PURPOSE (OPTIONAL)")
                    Spacer(Modifier.height(6.dp))
                    FMTextField(value = purpose, onValueChange = { purpose = it }, placeholder = "e.g. Diabetes, Blood pressure")
                }
            }
        },
        confirmButton = {
            Text(
                "SAVE",
                color = if (name.isNotBlank()) FMRedBright else FMBorder,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                modifier = Modifier.clickable {
                    if (name.isNotBlank()) {
                        val freq = selectedFrequencies.joinToString(", ").ifBlank { "" }
                        onSave(MedicationDraft(name, dosage, freq, purpose))
                    }
                }.padding(12.dp),
            )
        },
        dismissButton = {
            Text("CANCEL", color = FMTextSub, fontFamily = appFontFamily,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        },
    )
}
