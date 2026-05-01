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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

/** Parse LLM response into structured steps. */
private fun parseSteps(response: String): Pair<String, List<GuidanceStep>> {
    val lines = response.lines()
    var summary = ""
    val steps = mutableListOf<GuidanceStep>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("SUMMARY:", ignoreCase = true)) {
            summary = trimmed.removePrefix("SUMMARY:").trim()
        }
        // Match lines like "1. Step title | detail" or "1. URGENT: Step | detail"
        val stepMatch = Regex("""^\d+\.\s*(.+)""").find(trimmed)
        if (stepMatch != null) {
            val content = stepMatch.groupValues[1]
            val urgent = content.startsWith("URGENT:", ignoreCase = true)
            val cleaned = if (urgent) content.removePrefix("URGENT:").trim() else content
            val parts = cleaned.split("|", limit = 2)
            val title = parts[0].trim()
            val detail = if (parts.size > 1) parts[1].trim() else ""
            steps.add(GuidanceStep(step = title, detail = detail, urgent = urgent))
        }
    }

    // Fallback: if parsing produced nothing, show raw response as a single step
    if (steps.isEmpty()) {
        steps.add(GuidanceStep(step = "Guidance", detail = response))
    }
    if (summary.isEmpty()) {
        summary = "Assessment complete"
    }

    return summary to steps
}

@Composable
fun GuidanceScreen(viewModel: FieldMedicViewModel, onEndSession: () -> Unit) {
    val response by viewModel.llmResponse.collectAsState()
    val (summary, steps) = remember(response) { parseSteps(response) }

    val visible = remember(steps) { mutableStateListOf<Boolean>().also { list ->
        repeat(steps.size) { list.add(false) }
    } }
    val scrollState = rememberScrollState()

    LaunchedEffect(steps) {
        steps.forEachIndexed { index, _ ->
            delay(if (index == 0) 400L else 1400L)
            if (index < visible.size) visible[index] = true
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
                summary,
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
            steps.forEachIndexed { index, step ->
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
