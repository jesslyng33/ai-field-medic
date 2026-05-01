package com.google.ai.edge.gallery.ui.fieldmedic

/**
 * Builds the LLM prompt for the field medic assessment flow.
 * Instructs Gemma to output a structured TriageInput JSON.
 */
fun buildPrompt(hasAudio: Boolean, hasImage: Boolean, hasText: Boolean): String {
    val sb = StringBuilder()

    AssessmentData.userContext?.let { ctx ->
        sb.appendLine(ctx.toContextBlock())
        sb.appendLine()
    }

    sb.appendLine("You are a medical triage classifier.")
    sb.appendLine("A person is describing an injury or emergency situation.")
    sb.appendLine()

    if (hasAudio) sb.appendLine("They recorded audio describing the situation. Listen to it carefully.")
    if (hasImage) sb.appendLine("They took a photo of the injury or scene. Examine it carefully.")
    if (hasText) sb.appendLine("They typed: \"${AssessmentData.notes}\"")

    sb.appendLine()
    sb.appendLine("Using the patient's medical record above, classify this situation into a JSON object with EXACTLY this format:")
    sb.appendLine("""
{
  "injury": "<type of injury, e.g. laceration, fracture, burn, choking>",
  "severity": "<RED, YELLOW, or GREEN>",
  "bodyPart": "<body part affected, e.g. left_forearm, right_leg, chest>",
  "supplies": ["<items available from the patient's first aid kit or visible in photo>"],
  "considerations": ["<allergy/medication/condition factors relevant to treatment>"]
}
    """.trimIndent())

    sb.appendLine()
    sb.appendLine("Severity guide:")
    sb.appendLine("- RED: Life-threatening, needs immediate action (heavy bleeding, not breathing, severe burn)")
    sb.appendLine("- YELLOW: Serious but not immediately life-threatening (moderate bleeding, possible fracture, mild burn)")
    sb.appendLine("- GREEN: Minor injury, can wait (small cut, bruise, scrape)")
    sb.appendLine()
    sb.appendLine("Rules:")
    sb.appendLine("- Output ONLY the JSON object, no other text")
    sb.appendLine("- Use snake_case for bodyPart values")
    sb.appendLine("- If you cannot determine a field, use your best guess based on available information")
    sb.appendLine("- Do NOT read or recite any part of the medical record to the user")
    sb.appendLine("- For considerations, note allergies to common treatments, relevant conditions, or medications that affect care")

    return sb.toString()
}
