package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.theme.appFontFamily

@Composable
fun GuidanceScreen(viewModel: FieldMedicViewModel, onEndSession: () -> Unit) {
    val response by viewModel.llmResponse.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .padding(top = 48.dp),
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(FMGreenBright, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "CLASSIFICATION COMPLETE",
                    color = FMGreenBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "TRIAGE JSON",
                color = FMText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                lineHeight = 40.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Structured output from Gemma EB2",
                color = FMTextSub,
                fontSize = 13.sp,
                fontFamily = appFontFamily,
            )
        }

        Spacer(Modifier.height(20.dp))

        // JSON display
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FMSurface, RoundedCornerShape(14.dp))
                    .padding(16.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = response.ifBlank { "Waiting for response..." },
                    color = FMGreenBright,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 22.sp,
                )
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
