package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingMedicalScreen(onContinue: () -> Unit) {
    var allergies by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var selectedBloodType by remember { mutableStateOf("") }
    val bloodTypes = listOf("A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−", "Unknown")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(52.dp))

        // Step indicator
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StepDot(active = true)
            StepDot(active = false)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "MEDICAL\nPROFILE",
            color = FMText,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontFamily = appFontFamily,
            lineHeight = 46.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Set up once. Helps us give you accurate guidance.",
            color = FMTextSub,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(40.dp))
        HorizontalDivider(color = FMDivider)
        Spacer(Modifier.height(32.dp))

        // Allergies
        FMSectionLabel("ALLERGIES")
        Spacer(Modifier.height(10.dp))
        FMTextField(
            value = allergies,
            onValueChange = { allergies = it },
            placeholder = "e.g. Penicillin, bee stings, shellfish",
        )

        Spacer(Modifier.height(28.dp))

        // Blood Type
        FMSectionLabel("BLOOD TYPE")
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bloodTypes.forEach { type ->
                val selected = selectedBloodType == type
                FilterChip(
                    selected = selected,
                    onClick = { selectedBloodType = if (selected) "" else type },
                    label = {
                        Text(
                            type,
                            fontFamily = appFontFamily,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FMRed,
                        selectedLabelColor = Color.White,
                        containerColor = FMSurface,
                        labelColor = FMTextSub,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = FMBorder,
                        selectedBorderColor = FMRed,
                    ),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Medical Conditions
        FMSectionLabel("MEDICAL CONDITIONS")
        Spacer(Modifier.height(10.dp))
        FMTextField(
            value = conditions,
            onValueChange = { conditions = it },
            placeholder = "e.g. Diabetes, asthma, heart condition",
            singleLine = false,
            minLines = 3,
            maxLines = 5,
        )

        Spacer(Modifier.height(28.dp))

        // Medications
        FMSectionLabel("CURRENT MEDICATIONS")
        Spacer(Modifier.height(10.dp))
        FMTextField(
            value = medications,
            onValueChange = { medications = it },
            placeholder = "e.g. Metformin, aspirin, EpiPen",
            singleLine = false,
            minLines = 3,
            maxLines = 5,
        )

        Spacer(Modifier.height(44.dp))

        FMPrimaryButton("SAVE & CONTINUE", onClick = {
            AssessmentData.bloodType = selectedBloodType
            AssessmentData.allergies = allergies
            AssessmentData.medications = medications
            AssessmentData.conditions = conditions
            onContinue()
        })

        Spacer(Modifier.height(12.dp))
        Text(
            "You can update this at any time in settings",
            color = FMTextSub,
            fontSize = 12.sp,
            fontFamily = appFontFamily,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun StepDot(active: Boolean) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .height(6.dp)
            .fillMaxWidth(if (active) 0.08f else 0.04f)
    ) {
        drawRoundRect(
            color = if (active) FMRed else FMBorder,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
        )
    }
}
