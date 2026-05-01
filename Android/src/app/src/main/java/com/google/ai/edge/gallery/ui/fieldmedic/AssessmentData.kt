package com.google.ai.edge.gallery.ui.fieldmedic

import android.graphics.Bitmap

/**
 * Holds captured assessment data shared across the Field Medic nav graph.
 * Singleton scoped to the nav graph lifetime.
 */
object AssessmentData {
    var audioWavBytes: ByteArray? = null
    var photoBitmap: Bitmap? = null
    var notes: String = ""

    // Built once after trip setup; persists across assessment sessions
    var userContext: UserMedicalContext? = null
    var tripLocation: String = ""
    var soloTraveler: Boolean = true
    var firstAidKit: Set<String> = emptySet()

    var sessionReport: SessionReport? = null

    fun clear() {
        audioWavBytes = null
        photoBitmap = null
        notes = ""
        sessionReport = null
        // userContext, tripLocation, soloTraveler, firstAidKit intentionally preserved
    }

    fun hasInput(): Boolean =
        audioWavBytes != null || photoBitmap != null || notes.isNotBlank()
}
