package com.google.ai.edge.gallery.customtasks.fieldmedic

import android.graphics.Bitmap

/** Produces a text description of a wound from a camera frame (FastVLM). */
interface WoundDescriber {
    suspend fun describe(bitmap: Bitmap): String
}

/** Stub — returns placeholder. Swap with FastVLM implementation later. */
class StubWoundDescriber : WoundDescriber {
    override suspend fun describe(bitmap: Bitmap): String = "Wound visible in frame"
}
