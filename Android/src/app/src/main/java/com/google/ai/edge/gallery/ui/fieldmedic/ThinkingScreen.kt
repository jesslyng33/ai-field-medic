package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlinx.coroutines.delay

@Composable
fun ThinkingScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3500L)
        onReady()
    }

    val anim = rememberInfiniteTransition(label = "thinking")

    val rotation by anim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val outerScale by anim.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "outerScale",
    )
    val outerAlpha by anim.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "outerAlpha",
    )

    // Dot animation delays
    val dot1alpha by anim.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot1",
    )
    val dot2alpha by anim.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, 200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot2",
    )
    val dot3alpha by anim.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, 400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot3",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Animated cross
        Box(contentAlignment = Alignment.Center) {
            // Outer pulsing ring
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = outerScale
                        scaleY = outerScale
                        alpha = outerAlpha
                    }
                    .background(FMRed, CircleShape),
            )
            // Rotating ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .background(FMRed.copy(alpha = 0.08f), CircleShape),
            )
            // Center icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(FMRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = FMRed,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        Text(
            "ANALYZING YOUR\nSITUATION",
            color = FMText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = appFontFamily,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Preparing step-by-step guidance",
            color = FMTextSub,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        // Animated dots
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(dot1alpha, dot2alpha, dot3alpha).forEach { dotAlpha ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer { alpha = dotAlpha }
                        .background(FMRed, CircleShape),
                )
            }
        }
    }
}
