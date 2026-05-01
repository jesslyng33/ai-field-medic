package com.google.ai.edge.gallery.ui.fieldmedic

import android.content.Intent
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SummaryScreen(
    report: SessionReport?,
    onNewSession: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var logExpanded by remember { mutableStateOf(false) }

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

        if (report != null) {
            // --- Session details card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FMSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryRow(label = "PATIENT", value = report.patientName)
                SummaryRow(label = "SESSION START", value = report.startTimeFormatted)
                SummaryRow(label = "DURATION", value = report.durationFormatted)
                SummaryRow(
                    label = "LOCATION",
                    value = report.location.ifBlank { "Not specified" },
                )
                SummaryRow(
                    label = "TRAVELER STATUS",
                    value = if (report.soloTraveler) "Solo" else "With group",
                )
                if (report.firstAidKit.isNotEmpty()) {
                    SummaryRow(
                        label = "FIRST AID KIT",
                        value = report.firstAidKit.joinToString(", "),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // --- AI Summary ---
            FMSectionLabel("AI INCIDENT SUMMARY")
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FMSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Text(
                    report.aiSummary,
                    color = FMText,
                    fontSize = 14.sp,
                    fontFamily = appFontFamily,
                    lineHeight = 22.sp,
                )
            }

            // --- Conversation log (expandable) ---
            if (report.conversationLog.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { logExpanded = !logExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FMSectionLabel("CONVERSATION LOG (${report.conversationLog.count { it.role != TriageRole.SYSTEM }})")
                    Icon(
                        if (logExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (logExpanded) "Collapse" else "Expand",
                        tint = FMTextSub,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                ) {
                    if (logExpanded) {
                        val timeFmt = remember {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FMSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            report.conversationLog
                                .filter { it.role != TriageRole.SYSTEM }
                                .forEach { msg ->
                                    val isUser = msg.role == TriageRole.USER
                                    Column {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                if (isUser) "PATIENT" else "MEDIC",
                                                color = if (isUser) FMGreenBright else FMTextSub,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = appFontFamily,
                                                letterSpacing = 1.sp,
                                            )
                                            Text(
                                                timeFmt.format(Date(msg.timestamp)),
                                                color = FMTextSub.copy(alpha = 0.6f),
                                                fontSize = 9.sp,
                                                fontFamily = appFontFamily,
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            msg.content,
                                            color = FMText,
                                            fontSize = 13.sp,
                                            fontFamily = appFontFamily,
                                            lineHeight = 19.sp,
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        } else {
            // Fallback if no report data
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FMSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, FMBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Text(
                    "Session data unavailable.",
                    color = FMTextSub,
                    fontSize = 14.sp,
                    fontFamily = appFontFamily,
                )
            }
        }

        Spacer(Modifier.height(44.dp))

        // Share / export PDF button
        Button(
            onClick = {
                if (report == null || isGeneratingPdf) return@Button
                isGeneratingPdf = true
                scope.launch {
                    val file = withContext(Dispatchers.IO) {
                        PdfReportGenerator.generate(context, report)
                    }
                    isGeneratingPdf = false
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file,
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Field Medic Incident Report")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FMGreen),
            shape = RoundedCornerShape(14.dp),
            enabled = report != null && !isGeneratingPdf,
        ) {
            if (isGeneratingPdf) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "GENERATING PDF...",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    letterSpacing = 1.sp,
                )
            } else {
                Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text(
                    "EXPORT & SHARE PDF",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    letterSpacing = 1.sp,
                )
            }
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
