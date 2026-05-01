package com.google.ai.edge.gallery.ui.fieldmedic.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.ui.fieldmedic.FMBackground
import com.google.ai.edge.gallery.ui.fieldmedic.FMBorder
import com.google.ai.edge.gallery.ui.fieldmedic.FMCard
import com.google.ai.edge.gallery.ui.fieldmedic.FMDivider
import com.google.ai.edge.gallery.ui.fieldmedic.FMRed
import com.google.ai.edge.gallery.ui.fieldmedic.FMRedBright
import com.google.ai.edge.gallery.ui.fieldmedic.FMSurface
import com.google.ai.edge.gallery.ui.fieldmedic.FMText
import com.google.ai.edge.gallery.ui.fieldmedic.FMTextSub
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onViewProfile: () -> Unit = {},
    onEditVitals: () -> Unit = {},
    onEditAllergies: () -> Unit,
    onEditConditions: () -> Unit,
    onEditMedications: () -> Unit,
    onEditContacts: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val profile by vm.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
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
                "SETTINGS",
                color = FMText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                letterSpacing = 4.sp,
            )
        }

        Spacer(Modifier.height(20.dp))

        ProfileHeaderCard(
            name = profile?.profile?.fullName ?: "Not set",
            subtitle = profile?.profile?.let {
                val age = if (it.dateOfBirth > 0) ageFromMillis(it.dateOfBirth) else null
                listOfNotNull(
                    age?.let { "$it yrs" },
                    it.biologicalSex.takeIf { s -> s.isNotBlank() },
                    it.bloodType.takeIf { b -> b.isNotBlank() },
                ).joinToString(" · ").ifEmpty { "Tap to view profile" }
            } ?: "Tap to view profile",
            onClick = onViewProfile,
        )

        Spacer(Modifier.height(28.dp))
        SectionHeader("MEDICAL")
        SettingsRow(
            icon = Icons.Filled.Favorite,
            label = "Vitals",
            summary = profile?.profile?.let {
                "${it.weightKg.roundToInt()} kg · ${it.heightCm.roundToInt()} cm · ${it.bloodType.ifBlank { "Unknown" }}"
            } ?: "Not set",
            onClick = onEditVitals,
        )
        SettingsRow(
            icon = Icons.Filled.Warning,
            label = "Allergies",
            summary = profile?.allergies?.size?.let {
                if (it == 0) "None" else "$it saved"
            } ?: "None",
            onClick = onEditAllergies,
            iconTint = FMRedBright,
        )
        SettingsRow(
            icon = Icons.Filled.LocalHospital,
            label = "Conditions",
            summary = profile?.conditions?.size?.let {
                if (it == 0) "None" else "$it saved"
            } ?: "None",
            onClick = onEditConditions,
        )
        SettingsRow(
            icon = Icons.Filled.LocalPharmacy,
            label = "Medications",
            summary = profile?.medications?.size?.let {
                if (it == 0) "None" else "$it saved"
            } ?: "None",
            onClick = onEditMedications,
        )

        Spacer(Modifier.height(28.dp))
        SectionHeader("CONTACTS")
        SettingsRow(
            icon = Icons.Filled.Phone,
            label = "Emergency Contact",
            summary = profile?.emergencyContacts?.firstOrNull()?.name ?: "Not set",
            onClick = onEditContacts,
        )

        Spacer(Modifier.height(28.dp))
        SectionHeader("APP")
        SettingsRow(
            icon = Icons.Filled.AccessibilityNew,
            label = "Accessibility",
            summary = "Default",
            onClick = {},
        )

        Spacer(Modifier.height(40.dp))
        Text(
            "All data is stored locally and never leaves this device.",
            color = FMTextSub,
            fontSize = 12.sp,
            fontFamily = appFontFamily,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ProfileHeaderCard(name: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .clickable(onClick = onClick)
            .background(FMSurface, RoundedCornerShape(16.dp))
            .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(FMRed.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, null, tint = FMRed, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                color = FMText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
            )
            Text(subtitle, color = FMTextSub, fontSize = 13.sp, fontFamily = appFontFamily)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = FMTextSub)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = FMTextSub,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = appFontFamily,
        letterSpacing = 1.5.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    summary: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = FMTextSub,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(FMCard, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = FMText, fontSize = 15.sp, fontFamily = appFontFamily, fontWeight = FontWeight.Medium)
            Text(summary, color = FMTextSub, fontSize = 12.sp, fontFamily = appFontFamily)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = FMTextSub, modifier = Modifier.size(20.dp))
    }
    HorizontalDivider(color = FMDivider, modifier = Modifier.padding(start = 78.dp, end = 28.dp))
}

private fun ageFromMillis(millis: Long): Int {
    val nowYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()).toInt()
    val bornYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(millis)).toInt()
    return nowYear - bornYear
}
