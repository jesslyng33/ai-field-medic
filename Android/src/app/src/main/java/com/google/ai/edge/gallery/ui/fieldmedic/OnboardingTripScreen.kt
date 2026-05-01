package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily

private val FIRST_AID_ITEMS = listOf(
    "Bandages / gauze",
    "Tourniquet",
    "Medical tape",
    "Antiseptic wipes",
    "Scissors / knife",
    "Splint",
    "Tweezers",
    "CPR mask",
    "Gloves",
    "Pain reliever",
    "Antihistamines",
    "EpiPen",
    "SAM splint",
    "Moleskin",
    "Ace bandage",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingTripScreen(onReady: () -> Unit) {
    var location by remember { mutableStateOf("") }
    var soloTraveler by remember { mutableStateOf(true) }
    val checkedItems = remember {
        mutableStateOf(setOf<String>())
    }

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
            StepPill(active = false, widthFraction = 0.04f)
            StepPill(active = true, widthFraction = 0.08f)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "BEFORE YOUR\nTRIP",
            color = FMText,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontFamily = appFontFamily,
            lineHeight = 46.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Filled in each trip. Helps us guide your response.",
            color = FMTextSub,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
        )

        Spacer(Modifier.height(40.dp))
        HorizontalDivider(color = FMDivider)
        Spacer(Modifier.height(32.dp))

        // Location
        FMSectionLabel("WHERE ARE YOU GOING?")
        Spacer(Modifier.height(10.dp))
        FMTextField(
            value = location,
            onValueChange = { location = it },
            placeholder = "e.g. Rocky Mtn. National Park, CO",
        )

        Spacer(Modifier.height(28.dp))

        // Solo traveler toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FMSurface)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "SOLO TRAVELER",
                    color = FMText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = appFontFamily,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    "Worst-case guidance, no help nearby",
                    color = FMTextSub,
                    fontSize = 12.sp,
                    fontFamily = appFontFamily,
                )
            }
            Switch(
                checked = soloTraveler,
                onCheckedChange = { soloTraveler = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FMText,
                    checkedTrackColor = FMRed,
                    uncheckedTrackColor = FMBorder,
                    uncheckedThumbColor = FMTextSub,
                ),
            )
        }

        Spacer(Modifier.height(28.dp))

        // First aid kit
        FMSectionLabel("FIRST AID KIT — CHECK WHAT YOU HAVE")
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FIRST_AID_ITEMS.forEach { item ->
                val checked = item in checkedItems.value
                Row(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (checked) FMGreenBright else FMBorder,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(
                            color = if (checked) FMGreen.copy(alpha = 0.2f) else FMSurface,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable {
                            checkedItems.value = if (checked) {
                                checkedItems.value - item
                            } else {
                                checkedItems.value + item
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (checked) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = FMGreenBright,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        item,
                        color = if (checked) FMGreenBright else FMTextSub,
                        fontSize = 13.sp,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = appFontFamily,
                    )
                }
            }
        }

        Spacer(Modifier.height(44.dp))

        FMPrimaryButton("I'M READY", onClick = onReady, color = FMRed)

        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun StepPill(active: Boolean, widthFraction: Float) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .height(6.dp)
            .fillMaxWidth(widthFraction)
    ) {
        drawRoundRect(
            color = if (active) FMRed else FMBorder,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
        )
    }
}
