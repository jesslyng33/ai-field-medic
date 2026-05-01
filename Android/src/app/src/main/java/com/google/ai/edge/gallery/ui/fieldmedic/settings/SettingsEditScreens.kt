package com.google.ai.edge.gallery.ui.fieldmedic.settings

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.ui.fieldmedic.FMBackground
import com.google.ai.edge.gallery.ui.fieldmedic.FMBorder
import com.google.ai.edge.gallery.ui.fieldmedic.FMCard
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

@Composable
private fun EditScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
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
                title,
                color = FMText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                letterSpacing = 4.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
        ) {
            content()
            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllergiesEditScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    var sheetOpen by remember { mutableStateOf(false) }
    val list = profile?.allergies.orEmpty()

    EditScaffold(title = "ALLERGIES", onBack = onBack) {
        if (list.isEmpty()) {
            EmptyHint("No allergies saved")
        } else {
            list.forEach { allergy ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FMCard)
                        .border(1.dp, severityColor(allergy.severity).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(severityColor(allergy.severity), CircleShape),
                    )
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(allergy.allergen, color = FMText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = appFontFamily)
                        Text("${allergy.severity} · ${allergy.reaction}", color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily)
                    }
                    IconButton(onClick = { vm.deleteAllergy(allergy) }) {
                        Icon(Icons.Filled.Close, "Remove", tint = FMTextSub, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedAddButton(text = "ADD ALLERGY", onClick = { sheetOpen = true })
        Spacer(Modifier.height(20.dp))
        FMPrimaryButton("DONE", onClick = onBack)
    }

    if (sheetOpen) {
        AllergyDialog(
            onDismiss = { sheetOpen = false },
            onSave = { allergen, severity, reaction ->
                vm.addAllergy(allergen, severity, reaction)
                sheetOpen = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConditionsEditScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    val list = profile?.conditions.orEmpty()
    val common = listOf(
        "Diabetes", "Asthma", "Heart disease", "Seizure disorder",
        "Hemophilia", "COPD", "Stroke history", "High blood pressure",
    )
    var customOpen by remember { mutableStateOf(false) }

    EditScaffold(title = "CONDITIONS", onBack = onBack) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            common.forEach { name ->
                val saved = list.firstOrNull { it.conditionName == name }
                ChoiceChip(
                    text = name,
                    selected = saved != null,
                    onClick = {
                        if (saved != null) vm.deleteCondition(saved) else vm.addCondition(name)
                    },
                )
            }
            ChoiceChip(text = "+ Other", selected = false, onClick = { customOpen = true })
        }
        Spacer(Modifier.height(20.dp))
        list.filter { it.conditionName !in common }.forEach { condition ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FMCard)
                    .border(1.dp, FMBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(condition.conditionName, color = FMText, fontSize = 14.sp, fontFamily = appFontFamily, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.deleteCondition(condition) }) {
                    Icon(Icons.Filled.Close, "Remove", tint = FMTextSub, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        FMPrimaryButton("DONE", onClick = onBack)
    }

    if (customOpen) {
        TextEntryDialog(
            title = "Other condition",
            placeholder = "e.g. Lupus",
            onDismiss = { customOpen = false },
            onConfirm = {
                if (it.isNotBlank()) vm.addCondition(it.trim())
                customOpen = false
            },
        )
    }
}

@Composable
fun MedicationsEditScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    val list = profile?.medications.orEmpty()
    var sheetOpen by remember { mutableStateOf(false) }

    EditScaffold(title = "MEDICATIONS", onBack = onBack) {
        if (list.isEmpty()) {
            EmptyHint("No medications saved")
        } else {
            list.forEach { med ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FMCard)
                        .border(1.dp, FMBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(med.name, color = FMText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = appFontFamily)
                        Text(
                            listOf(med.dosage, med.frequency, med.purpose).filter { s -> s.isNotBlank() }.joinToString(" · "),
                            color = FMTextSub,
                            fontSize = 12.sp,
                            fontFamily = appFontFamily,
                        )
                    }
                    IconButton(onClick = { vm.deleteMedication(med) }) {
                        Icon(Icons.Filled.Close, "Remove", tint = FMTextSub, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedAddButton(text = "ADD MEDICATION", onClick = { sheetOpen = true })
        Spacer(Modifier.height(20.dp))
        FMPrimaryButton("DONE", onClick = onBack)
    }

    if (sheetOpen) {
        MedicationDialog(
            onDismiss = { sheetOpen = false },
            onSave = { name, dosage, frequency, purpose ->
                vm.addMedication(name, dosage, frequency, purpose)
                sheetOpen = false
            },
        )
    }
}

@Composable
fun ContactEditScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsState()
    val existing = profile?.emergencyContacts?.firstOrNull { it.isPrimary }
        ?: profile?.emergencyContacts?.firstOrNull()
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var phone by remember(existing) { mutableStateOf(existing?.phoneNumber ?: "") }
    var rel by remember(existing) { mutableStateOf(existing?.relationship ?: "") }
    var canDecide by remember(existing) { mutableStateOf(existing?.canMakeMedicalDecisions ?: true) }
    val rels = listOf("Spouse", "Parent", "Sibling", "Friend", "Other")

    EditScaffold(title = "CONTACT", onBack = onBack) {
        FMSectionLabel("FULL NAME")
        Spacer(Modifier.height(10.dp))
        FMTextField(value = name, onValueChange = { name = it }, placeholder = "Name")
        Spacer(Modifier.height(20.dp))
        FMSectionLabel("PHONE")
        Spacer(Modifier.height(10.dp))
        FMTextField(value = phone, onValueChange = { phone = it }, placeholder = "+1 555 555 5555")
        Spacer(Modifier.height(20.dp))
        FMSectionLabel("RELATIONSHIP")
        Spacer(Modifier.height(10.dp))
        FlowRowSegmented(options = rels, selected = rel, onSelect = { rel = it })
        Spacer(Modifier.height(20.dp))
        ToggleRow(
            title = "CAN MAKE MEDICAL DECISIONS",
            subtitle = "Healthcare proxy / power of attorney",
            value = canDecide,
            onChange = { canDecide = it },
        )
        Spacer(Modifier.height(28.dp))
        FMPrimaryButton(
            "SAVE",
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    vm.upsertEmergencyContact(
                        existing = existing,
                        name = name,
                        relationship = rel.ifBlank { "Other" },
                        phoneNumber = phone,
                        canMakeMedicalDecisions = canDecide,
                    )
                    onBack()
                }
            },
            color = if (name.isNotBlank() && phone.isNotBlank()) FMRed else FMBorder,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSegmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            ChoiceChip(text = opt, selected = selected == opt, onClick = { onSelect(opt) })
        }
    }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
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
private fun ToggleRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FMSurface)
            .clickable { onChange(!value) }
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
        androidx.compose.material3.Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = FMText,
                checkedTrackColor = FMRed,
                uncheckedTrackColor = FMBorder,
                uncheckedThumbColor = FMTextSub,
            ),
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

private fun severityColor(severity: String) = when (severity.lowercase()) {
    "life-threatening" -> FMRedBright
    "severe" -> FMRed
    "moderate" -> Color(0xFFFFA000)
    else -> FMGreenBright
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
            Text("CANCEL", color = FMTextSub, fontFamily = appFontFamily, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        },
    )
}

@Composable
private fun MedicationDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
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
                        if (name.isNotBlank()) onSave(name, dosage, frequency, purpose)
                    }
                    .padding(12.dp),
            )
        },
        dismissButton = {
            Text("CANCEL", color = FMTextSub, fontFamily = appFontFamily, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        },
    )
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
                modifier = Modifier.clickable { onConfirm(text) }.padding(12.dp),
            )
        },
        dismissButton = {
            Text("CANCEL", color = FMTextSub, fontFamily = appFontFamily, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
        },
    )
}
