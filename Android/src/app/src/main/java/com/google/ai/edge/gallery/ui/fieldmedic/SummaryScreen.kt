package com.google.ai.edge.gallery.ui.fieldmedic

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily

private val ACTIONS_TAKEN = listOf(
    "Applied direct pressure with clean cloth",
    "Elevated injured limb above heart level",
    "Monitored for signs of shock",
    "Applied tourniquet at 14:32 — noted time",
    "Kept patient warm with emergency blanket",
    "Activated personal locator beacon",
)

@Composable
fun SummaryScreen(onNewSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(52.dp))

        // Check mark
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FMGreenBright.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = FMGreenBright,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "SESSION\nCOMPLETE",
            color = FMText,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontFamily = appFontFamily,
            lineHeight = 46.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Show this screen to emergency responders when help arrives.",
            color = FMTextSub,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = FMDivider)
        Spacer(Modifier.height(28.dp))

        // Injury details card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FMSurface, RoundedCornerShape(16.dp))
                .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SummaryRow(label = "INJURY TYPE", value = "Laceration — lower left forearm")
            SummaryRow(label = "SEVERITY", value = "Moderate — arterial bleeding suspected")
            SummaryRow(label = "SESSION START", value = "14:18 local time")
            SummaryRow(label = "DURATION", value = "22 minutes")
            SummaryRow(label = "LOCATION", value = "Rocky Mtn. National Park, CO")
            SummaryRow(label = "TRAVELER STATUS", value = "Solo")
        }

        Spacer(Modifier.height(28.dp))

        // Alert banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FMRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, FMRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = FMRed,
                modifier = Modifier.size(20.dp),
            )
            Text(
                "Seek emergency care within 4 hours. Tourniquet was applied — do not remove.",
                color = FMRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(28.dp))

        // Actions taken
        FMSectionLabel("ACTIONS TAKEN")
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ACTIONS_TAKEN.forEach { action ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = FMGreenBright,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        action,
                        color = FMText,
                        fontSize = 14.sp,
                        fontFamily = appFontFamily,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(44.dp))

        // Share button
        Button(
            onClick = { /* share intent — not wired to backend */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FMGreen),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                "SHARE WITH EMERGENCY SERVICES",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                letterSpacing = 1.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        // New session button
        OutlinedButton(
            onClick = onNewSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FMTextSub),
            border = androidx.compose.foundation.BorderStroke(1.dp, FMBorder),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                "NEW SESSION",
                color = FMTextSub,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = appFontFamily,
                letterSpacing = 2.sp,
            )
        }

        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            color = FMTextSub,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = appFontFamily,
            letterSpacing = 1.5.sp,
        )
        Text(
            value,
            color = FMText,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
            lineHeight = 20.sp,
        )
    }
}
