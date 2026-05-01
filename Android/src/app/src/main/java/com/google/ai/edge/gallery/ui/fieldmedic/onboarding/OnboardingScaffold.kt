package com.google.ai.edge.gallery.ui.fieldmedic.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.fieldmedic.FMBackground
import com.google.ai.edge.gallery.ui.fieldmedic.FMBorder
import com.google.ai.edge.gallery.ui.fieldmedic.FMGhostButton
import com.google.ai.edge.gallery.ui.fieldmedic.FMPrimaryButton
import com.google.ai.edge.gallery.ui.fieldmedic.FMRed
import com.google.ai.edge.gallery.ui.fieldmedic.FMText
import com.google.ai.edge.gallery.ui.fieldmedic.FMTextSub
import com.google.ai.edge.gallery.ui.theme.appFontFamily

@Composable
fun OnboardingStep(
    title: String,
    subtitle: String?,
    step: Int,
    total: Int,
    onBack: (() -> Unit)? = null,
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = FMTextSub)
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
        ) {
            StepProgress(step = step, total = total)
            Spacer(Modifier.height(28.dp))

            Text(
                title,
                color = FMText,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                lineHeight = 46.sp,
                letterSpacing = 1.sp,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    subtitle,
                    color = FMTextSub,
                    fontSize = 15.sp,
                    fontFamily = appFontFamily,
                    lineHeight = 22.sp,
                )
            }

            Spacer(Modifier.height(36.dp))
            content()
            Spacer(Modifier.height(36.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bottom()
        }
    }
}

@Composable
fun OnboardingNavBar(
    onContinue: () -> Unit,
    continueLabel: String = "CONTINUE",
    continueEnabled: Boolean = true,
    onBack: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            FMGhostButton(
                text = "← BACK",
                onClick = onBack,
                modifier = Modifier.weight(0.38f),
            )
        }
        FMPrimaryButton(
            text = continueLabel,
            onClick = onContinue,
            modifier = Modifier.weight(if (onBack != null) 0.62f else 1f),
            color = if (continueEnabled) FMRed else FMBorder,
        )
    }
}

@Composable
private fun StepProgress(step: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val active = index == step - 1
            val complete = index < step - 1
            Canvas(
                modifier = Modifier
                    .height(6.dp)
                    .fillMaxWidth(if (active) 0.10f else 0.04f)
            ) {
                drawRoundRect(
                    color = when {
                        active -> FMRed
                        complete -> FMRed.copy(alpha = 0.45f)
                        else -> FMBorder
                    },
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}
