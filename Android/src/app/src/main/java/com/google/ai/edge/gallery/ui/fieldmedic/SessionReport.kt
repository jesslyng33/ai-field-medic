package com.google.ai.edge.gallery.ui.fieldmedic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SessionReport(
    val aiSummary: String,
    val conversationLog: List<TriageMessage>,
    val sessionStartMs: Long,
    val sessionEndMs: Long,
    val patientName: String,
    val location: String,
    val soloTraveler: Boolean,
    val firstAidKit: Set<String>,
) {
    val durationFormatted: String
        get() {
            val diff = sessionEndMs - sessionStartMs
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            val secs = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
        }

    val startTimeFormatted: String
        get() {
            val fmt = SimpleDateFormat("HH:mm, MMM d yyyy", Locale.getDefault())
            return fmt.format(Date(sessionStartMs))
        }

    val dateFormatted: String
        get() {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return fmt.format(Date(sessionStartMs))
        }
}
