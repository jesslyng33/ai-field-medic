package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily

@Composable
fun FieldMedicHomeScreen(onGetHelp: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top — branding
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(64.dp))
            // Medical cross icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(FMRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Field Medic",
                    tint = FMRed,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "FIELD MEDIC",
                color = FMText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "AI-powered wilderness first aid",
                color = FMTextSub,
                fontSize = 14.sp,
                fontFamily = appFontFamily,
                letterSpacing = 0.5.sp,
            )
        }

        // Center — SOS button
        Box(
            contentAlignment = Alignment.Center,
        ) {
            // Pulsing outer ring
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(FMRed, CircleShape),
            )
            // Pulsing mid ring
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = (pulseScale + 1f) / 2f
                        scaleY = (pulseScale + 1f) / 2f
                        alpha = pulseAlpha * 0.5f
                    }
                    .background(FMRed.copy(alpha = 0.4f), CircleShape),
            )
            // Main button
            androidx.compose.material3.FilledIconButton(
                onClick = onGetHelp,
                modifier = Modifier.size(180.dp),
                shape = CircleShape,
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                    containerColor = FMRed,
                    contentColor = FMText,
                ),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Get Help",
                        tint = FMText,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "GET HELP",
                        color = FMText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                        letterSpacing = 3.sp,
                    )
                }
            }
        }

        // Bottom — hint
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 40.dp),
        ) {
            Text(
                "Tap to describe your emergency",
                color = FMTextSub,
                fontSize = 14.sp,
                fontFamily = appFontFamily,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Works offline · No internet needed",
                color = FMBorder,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
