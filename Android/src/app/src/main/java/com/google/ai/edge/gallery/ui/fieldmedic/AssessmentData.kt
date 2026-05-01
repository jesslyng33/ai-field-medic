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

    fun clear() {
        audioWavBytes = null
        photoBitmap = null
        notes = ""
    }

    fun hasInput(): Boolean =
        audioWavBytes != null || photoBitmap != null || notes.isNotBlank()
}
