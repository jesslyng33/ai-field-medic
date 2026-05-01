package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlinx.coroutines.delay

private data class GuidanceStep(
    val step: String,
    val detail: String,
    val urgent: Boolean = false,
)

private val MOCK_STEPS = listOf(
    GuidanceStep(
        step = "Stay calm. Stop all movement.",
        detail = "Panic increases blood loss and worsens shock. Sit or lie down immediately.",
        urgent = true,
    ),
    GuidanceStep(
        step = "Apply direct pressure to the wound.",
        detail = "Use the cleanest cloth available. Press firmly with both hands. Do not lift to check — add more cloth on top if it soaks through.",
    ),
    GuidanceStep(
        step = "Elevate if possible.",
        detail = "Raise the injured limb above heart level to slow bleeding. Support it with a pack or rolled clothing.",
    ),
    GuidanceStep(
        step = "Check for shock.",
        detail = "Watch for: pale or clammy skin, rapid shallow breathing, confusion or weakness. If any appear, keep the person flat and warm.",
        urgent = true,
    ),
    GuidanceStep(
        step = "Apply tourniquet if bleeding does not stop.",
        detail = "Place 2–3 inches above the wound. Tighten until bleeding stops. Note the time applied. Do not remove.",
    ),
    GuidanceStep(
        step = "Keep the person warm.",
        detail = "Cover with any available insulation. Hypothermia worsens shock significantly in outdoor settings.",
    ),
    GuidanceStep(
        step = "Monitor breathing every 5 minutes.",
        detail = "Count breaths per minute. Normal is 12–20. Note any changes.",
    ),
    GuidanceStep(
        step = "Signal for help.",
        detail = "Activate personal locator beacon if available. Use whistle: 3 blasts = distress signal. Stay visible.",
        urgent = true,
    ),
)

@Composable
fun GuidanceScreen(onEndSession: () -> Unit) {
    val visible = remember { mutableStateListOf<Boolean>().also { list ->
        repeat(MOCK_STEPS.size) { list.add(false) }
    } }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        MOCK_STEPS.forEachIndexed { index, _ ->
            delay(if (index == 0) 400L else 1400L)
            visible[index] = true
            delay(100L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding(),
    ) {
        // Header — fixed
        Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(FMGreenBright, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "GUIDANCE READY",
                    color = FMGreenBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "FOLLOW THESE\nSTEPS",
                color = FMText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                lineHeight = 40.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Laceration · Moderate bleeding · Solo",
                color = FMTextSub,
                fontSize = 13.sp,
                fontFamily = appFontFamily,
            )
        }

        // Scrollable steps
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MOCK_STEPS.forEachIndexed { index, step ->
                AnimatedVisibility(
                    visible = visible.getOrElse(index) { false },
                    enter = fadeIn(tween(500)) + slideInVertically(tween(400)) { it / 3 },
                ) {
                    GuidanceStepCard(stepNumber = index + 1, step = step)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Bottom — End Session button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FMBackground)
                .padding(horizontal = 28.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            FMPrimaryButton(
                text = "END SESSION",
                onClick = onEndSession,
                color = FMCard,
            )
        }
    }
}

@Composable
private fun GuidanceStepCard(stepNumber: Int, step: GuidanceStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (step.urgent) FMRed.copy(alpha = 0.08f) else FMSurface,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Step number badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (step.urgent) FMRed else FMCard,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$stepNumber",
                color = if (step.urgent) FMText else FMTextSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
            )
        }

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                step.step,
                color = FMText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                step.detail,
                color = FMTextSub,
                fontSize = 14.sp,
                fontFamily = appFontFamily,
                lineHeight = 20.sp,
            )
        }
    }
}
