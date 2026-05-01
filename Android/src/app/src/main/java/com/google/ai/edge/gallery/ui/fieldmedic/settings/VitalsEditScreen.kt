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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.google.ai.edge.gallery.ui.fieldmedic.FMPrimaryButton
import com.google.ai.edge.gallery.ui.fieldmedic.FMRed
import com.google.ai.edge.gallery.ui.fieldmedic.FMRedBright
import com.google.ai.edge.gallery.ui.fieldmedic.FMSectionLabel
import com.google.ai.edge.gallery.ui.fieldmedic.FMSurface
import com.google.ai.edge.gallery.ui.fieldmedic.FMText
import com.google.ai.edge.gallery.ui.fieldmedic.FMTextField
import com.google.ai.edge.gallery.ui.fieldmedic.FMTextSub
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VitalsEditScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val profile by vm.profile.collectAsState()
    val currentProfile = profile?.profile

    val bloodTypes = listOf("A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−", "Unknown")

    // Weight / height state
    var useMetric by remember { mutableStateOf(true) }
    var weightKg by remember(currentProfile) {
        mutableStateOf(currentProfile?.weightKg ?: 70f)
    }
    var heightCm by remember(currentProfile) {
        mutableStateOf(currentProfile?.heightCm ?: 170f)
    }
    var bloodType by remember(currentProfile) {
        mutableStateOf(currentProfile?.bloodType ?: "")
    }

    // Additional vitals (stored as local state — TODO: add schema fields for persistence)
    // TODO: Add bloodPressureSystolic, bloodPressureDiastolic, restingHeartRate, spo2,
    //       and temperatureCelsius to the UserProfile entity (or a VitalsLog entity)
    //       and wire them through the repository / SettingsViewModel.updateVitals().
    var bpSystolic by remember { mutableStateOf("") }
    var bpDiastolic by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf(70) }
    var spo2 by remember { mutableStateOf(98) }
    var temperature by remember { mutableStateOf("") }

    // Imperial helpers
    val displayWeightLbs: Float get() = weightKg * 2.20462f
    val displayHeightFt: Int get() = (heightCm / 30.48f).toInt()
    val displayHeightIn: Int get() = ((heightCm / 2.54f).toInt() % 12)

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
                "VITALS",
                color = FMText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                letterSpacing = 4.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
        ) {
            Spacer(Modifier.height(8.dp))

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
                            .background(if (useMetric == isMetric) FMRed else Color.Transparent)
                            .clickable { useMetric = isMetric }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = if (useMetric == isMetric) Color.White else FMTextSub,
                            fontSize = 12.sp,
                            fontWeight = if (useMetric == isMetric) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = appFontFamily,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            if (useMetric) {
                VitalsSlider(
                    label = "WEIGHT",
                    value = weightKg,
                    onChange = { weightKg = it },
                    range = 30f..200f,
                    unit = "kg",
                )
                Spacer(Modifier.height(28.dp))
                VitalsSlider(
                    label = "HEIGHT",
                    value = heightCm,
                    onChange = { heightCm = it },
                    range = 100f..230f,
                    unit = "cm",
                )
            } else {
                VitalsSlider(
                    label = "WEIGHT",
                    value = displayWeightLbs,
                    onChange = { weightKg = it / 2.20462f },
                    range = 66f..440f,
                    unit = "lbs",
                )
                Spacer(Modifier.height(28.dp))
                FMSectionLabel("HEIGHT")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$displayHeightFt'",
                        color = FMText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "$displayHeightIn\"",
                        color = FMText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                    )
                }
                Slider(
                    value = displayHeightFt.toFloat(),
                    onValueChange = { ft ->
                        heightCm = ((ft.toInt() * 12 + displayHeightIn) * 2.54f)
                    },
                    valueRange = 3f..7f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = FMRed,
                        activeTrackColor = FMRed,
                        inactiveTrackColor = FMBorder,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Feet", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                    Text("$displayHeightFt ft", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                }
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = displayHeightIn.toFloat(),
                    onValueChange = { inches ->
                        heightCm = ((displayHeightFt * 12 + inches.toInt()) * 2.54f)
                    },
                    valueRange = 0f..11f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = FMRed,
                        activeTrackColor = FMRed,
                        inactiveTrackColor = FMBorder,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Inches", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                    Text("$displayHeightIn in", color = FMTextSub, fontSize = 11.sp, fontFamily = appFontFamily)
                }
            }

            Spacer(Modifier.height(28.dp))

            // Blood type
            FMSectionLabel("BLOOD TYPE")
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                bloodTypes.forEach { type ->
                    VitalsChoiceChip(
                        text = type,
                        selected = bloodType == type,
                        onClick = { bloodType = if (bloodType == type) "" else type },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Blood pressure
            FMSectionLabel("BLOOD PRESSURE (mmHg)")
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FMTextField(
                    value = bpSystolic,
                    onValueChange = { bpSystolic = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = "Systolic",
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "/",
                    color = FMTextSub,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                )
                FMTextField(
                    value = bpDiastolic,
                    onValueChange = { bpDiastolic = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = "Diastolic",
                    modifier = Modifier.weight(1f),
                )
            }
            if (bpSystolic.isNotBlank() || bpDiastolic.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Normal: 90–120 / 60–80",
                    color = FMTextSub,
                    fontSize = 11.sp,
                    fontFamily = appFontFamily,
                )
            }

            Spacer(Modifier.height(28.dp))

            // Resting heart rate
            FMSectionLabel("RESTING HEART RATE")
            Spacer(Modifier.height(10.dp))
            VitalsNumberStepper(
                label = "bpm",
                value = heartRate,
                range = 30..200,
                onChange = { heartRate = it },
            )

            Spacer(Modifier.height(28.dp))

            // SpO2
            FMSectionLabel("OXYGEN SATURATION (SpO2)")
            Spacer(Modifier.height(10.dp))
            VitalsNumberStepper(
                label = "%",
                value = spo2,
                range = 85..100,
                onChange = { spo2 = it },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Normal: ≥ 95%",
                color = FMTextSub,
                fontSize = 11.sp,
                fontFamily = appFontFamily,
            )

            Spacer(Modifier.height(28.dp))

            // Temperature
            FMSectionLabel("BODY TEMPERATURE (°C)")
            Spacer(Modifier.height(10.dp))
            FMTextField(
                value = temperature,
                onValueChange = { temperature = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                placeholder = "e.g. 36.6",
            )
            if (temperature.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Normal: 36.1–37.2 °C",
                    color = FMTextSub,
                    fontSize = 11.sp,
                    fontFamily = appFontFamily,
                )
            }

            Spacer(Modifier.height(32.dp))

            FMPrimaryButton(
                "SAVE",
                onClick = {
                    vm.updateVitals(
                        weightKg = weightKg,
                        heightCm = heightCm,
                        bloodType = bloodType,
                    )
                    // TODO: Persist bpSystolic, bpDiastolic, heartRate, spo2, temperature
                    //       once schema fields are added for these values.
                    onBack()
                },
                color = FMRed,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun VitalsSlider(
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
private fun VitalsNumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    var showEdit by remember { mutableStateOf(false) }
    var editText by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = FMTextSub,
            fontSize = 12.sp,
            fontFamily = appFontFamily,
            modifier = Modifier.weight(1f),
        )
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
private fun VitalsChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
