package com.google.ai.edge.gallery.ui.fieldmedic

/**
 * Builds the LLM prompt for the field medic assessment flow.
 */
fun buildPrompt(hasAudio: Boolean, hasImage: Boolean, hasText: Boolean): String {
    val sb = StringBuilder()
    sb.appendLine("You are a helpful outdoor safety assistant.")
    sb.appendLine("A person is describing a situation they need help with.")

    if (hasAudio) sb.appendLine("They recorded audio describing the situation. Listen to it.")
    if (hasImage) sb.appendLine("They took a photo. Examine it carefully.")
    if (hasText) sb.appendLine("They typed: \"${AssessmentData.notes}\"")

    sb.appendLine()
    sb.appendLine("Provide step-by-step guidance to help them.")
    sb.appendLine("Format your response EXACTLY like this:")
    sb.appendLine("SUMMARY: [one line describing the situation]")
    sb.appendLine("STEPS:")
    sb.appendLine("1. [Step title] | [Detail]")
    sb.appendLine("2. [Step title] | [Detail]")
    sb.appendLine("Mark critical steps with URGENT: before the title.")
    sb.appendLine("Be concise and actionable.")
    return sb.toString()
}
