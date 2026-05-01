package com.google.ai.edge.gallery.ui.fieldmedic

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.ai.edge.gallery.ui.theme.AppTypography
import com.google.ai.edge.gallery.ui.theme.appFontFamily

// Field Medic color palette — always dark, high contrast, minimal
val FMBackground = Color(0xFF080808)
val FMSurface = Color(0xFF141414)
val FMCard = Color(0xFF1E1E1E)
val FMRed = Color(0xFFD32F2F)
val FMRedBright = Color(0xFFEF5350)
val FMRedGlow = Color(0x33EF5350)
val FMGreen = Color(0xFF2E7D32)
val FMGreenBright = Color(0xFF43A047)
val FMText = Color(0xFFFFFFFF)
val FMTextSub = Color(0xFF9E9E9E)
val FMDivider = Color(0xFF222222)
val FMBorder = Color(0xFF333333)

@Composable
fun FieldMedicAppTheme(content: @Composable () -> Unit) {
    // Always force light status bar icons since field medic background is always dark
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    if (window != null) {
        SideEffect {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FMRed,
            onPrimary = Color.White,
            secondary = FMGreenBright,
            onSecondary = Color.White,
            background = FMBackground,
            surface = FMSurface,
            onBackground = FMText,
            onSurface = FMText,
            surfaceVariant = FMCard,
            onSurfaceVariant = FMTextSub,
            outline = FMBorder,
        ),
        typography = AppTypography,
        content = content,
    )
}

@Composable
fun FMSectionLabel(text: String) {
    Text(
        text = text,
        color = FMTextSub,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = appFontFamily,
        letterSpacing = 1.5.sp,
    )
}

@Composable
fun FMTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = FMBorder, fontFamily = appFontFamily, fontSize = 15.sp)
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = appFontFamily,
            fontSize = 15.sp,
            color = FMText,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FMText,
            unfocusedTextColor = FMText,
            focusedBorderColor = FMRed,
            unfocusedBorderColor = FMBorder,
            cursorColor = FMRed,
            focusedContainerColor = FMSurface,
            unfocusedContainerColor = FMSurface,
        ),
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
fun FMPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FMRed,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = text,
            color = FMText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = appFontFamily,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
fun FMGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = FMTextSub),
        border = BorderStroke(1.dp, FMBorder),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = text,
            color = FMTextSub,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = appFontFamily,
            letterSpacing = 2.sp,
        )
    }
}
