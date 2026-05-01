package com.google.ai.edge.gallery.customtasks.fieldmedic

// Severity levels matching Gemma's JSON output
enum class Severity { RED, YELLOW, GREEN }

// Injury types matching Gemma's JSON output
enum class InjuryType {
    SEVERE_BLEEDING,
    FRACTURE,
    BURN,
    AIRWAY_OBSTRUCTION,
    UNKNOWN
}

data class TriageInput(
    val injury: String,
    val severity: String,
    val bodyPart: String,
    val supplies: List<String> = emptyList(),
    val userProfile: UserProfile = UserProfile()
)

data class UserProfile(
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val conditions: List<String> = emptyList()
)

data class Protocol(
    val injuryType: InjuryType,
    val severity: Severity,
    val steps: List<String>
)

data class RegenerationContext(
    val linesRead: List<Int>,
    val userInput: String,
    val currentProtocol: Protocol
)

object TriageEngine {

    private val protocols: Map<InjuryType, Map<Severity, List<String>>> = mapOf(

        InjuryType.SEVERE_BLEEDING to mapOf(
            Severity.RED to listOf(
                "Call out to anyone nearby for help.",
                "Press down HARD on the wound with the cleanest cloth available.",
                "Do not lift the cloth — if it soaks through, add more on top.",
                "Keep firm, continuous pressure for at least 10 minutes.",
                "Check — is the bleeding slowing down?",
                "If bleeding does not slow, apply a tourniquet 2 inches above the wound.",
                "Tighten the tourniquet until bleeding stops, then note the time.",
                "Keep the person lying down and warm. Do not give food or water.",
                "Monitor breathing and keep talking to them until help arrives."
            ),
            Severity.YELLOW to listOf(
                "Apply firm pressure to the wound with a clean cloth.",
                "Elevate the injured limb above heart level if possible.",
                "Hold pressure for at least 5 minutes without lifting.",
                "Check — has bleeding slowed significantly?",
                "If still bleeding steadily, increase pressure and reassess in 5 minutes.",
                "Once bleeding is controlled, secure the cloth with a bandage or tape.",
                "Monitor for signs of shock: pale skin, rapid breathing, confusion."
            ),
            Severity.GREEN to listOf(
                "Clean the wound gently with clean water if available.",
                "Apply light pressure with a clean cloth until bleeding stops.",
                "Cover with a clean bandage or cloth.",
                "Monitor for increased bleeding or signs of infection."
            )
        ),

        InjuryType.FRACTURE to mapOf(
            Severity.RED to listOf(
                "Do not move the person unless they are in immediate danger.",
                "If there is bleeding at the fracture site, apply gentle pressure around (not on) the bone.",
                "Immobilize the limb in the position you find it — do not try to straighten it.",
                "Use sticks, rolled clothing, or any rigid object as a splint on both sides of the limb.",
                "Tie the splint above and below the break — not directly on it.",
                "Check circulation below the injury: can they feel their fingers or toes?",
                "Treat for shock: keep them warm and lying down.",
                "If the bone is exposed, cover loosely with the cleanest material available.",
                "Do not give food or water. Keep monitoring until help arrives."
            ),
            Severity.YELLOW to listOf(
                "Have the person stop moving the injured limb.",
                "Immobilize using a splint — rigid object tied above and below the break.",
                "Check circulation: sensation, warmth, and movement below the injury.",
                "Apply ice wrapped in cloth if available to reduce swelling.",
                "Elevate the limb if it is an arm or lower leg fracture.",
                "Monitor for worsening pain, numbness, or color change in the limb."
            ),
            Severity.GREEN to listOf(
                "Rest the injured area and avoid putting weight on it.",
                "Apply ice wrapped in cloth for 20 minutes to reduce swelling.",
                "Immobilize if possible and seek medical attention."
            )
        ),

        InjuryType.BURN to mapOf(
            Severity.RED to listOf(
                "Remove the person from the source of the burn immediately.",
                "Do not remove clothing stuck to burned skin.",
                "Cool the burn with cool (not cold) running water for at least 20 minutes.",
                "Do not use ice, butter, or any cream on the burn.",
                "Cover loosely with a clean dry cloth or cling wrap — do not wrap tightly.",
                "If the face or airway is burned, watch for swelling and breathing difficulty.",
                "If breathing becomes difficult, position them upright and keep the airway open.",
                "Treat for shock: keep them warm and lying down.",
                "Do not give food or water. Keep monitoring until help arrives."
            ),
            Severity.YELLOW to listOf(
                "Cool the burn with cool running water for 20 minutes.",
                "Remove jewelry or tight items near the burn before swelling starts.",
                "Do not pop any blisters — cover loosely with a clean cloth.",
                "Monitor for signs of infection or worsening over the next hours."
            ),
            Severity.GREEN to listOf(
                "Cool with cool running water for 10 minutes.",
                "Cover with a clean, loose bandage.",
                "Monitor for blistering or increased pain."
            )
        ),

        InjuryType.AIRWAY_OBSTRUCTION to mapOf(
            Severity.RED to listOf(
                "Ask the person: 'Are you choking?' If they cannot speak or cough, act immediately.",
                "Stand behind them, lean them slightly forward.",
                "Give 5 firm back blows between the shoulder blades with the heel of your hand.",
                "Check mouth — if object is visible, carefully remove it. Do not do a blind finger sweep.",
                "If still obstructed: give 5 abdominal thrusts — fist above navel, sharp inward and upward.",
                "Alternate 5 back blows and 5 abdominal thrusts until object is cleared or they lose consciousness.",
                "If they lose consciousness, lower them carefully to the ground.",
                "Begin CPR — each time you open the airway to give a breath, look for the object.",
                "Continue until the object is cleared or help arrives."
            ),
            Severity.YELLOW to listOf(
                "Encourage the person to keep coughing forcefully.",
                "Stay with them and monitor — do not interfere if they are coughing effectively.",
                "If coughing becomes ineffective or they cannot speak, escalate to back blows and abdominal thrusts.",
                "Reassure them and keep them calm."
            ),
            Severity.GREEN to listOf(
                "Encourage coughing to clear the obstruction.",
                "Monitor breathing and stay close.",
                "Seek medical attention if discomfort persists."
            )
        )
    )

    private val fallbackProtocol = Protocol(
        injuryType = InjuryType.UNKNOWN,
        severity = Severity.YELLOW,
        steps = listOf(
            "Keep the person as calm and still as possible.",
            "Check their breathing — is the chest rising and falling?",
            "Check for visible bleeding — apply pressure to any wounds.",
            "Keep them warm and lying down.",
            "Do not give food or water.",
            "Stay with them and keep monitoring until help arrives."
        )
    )

    fun getProtocol(input: TriageInput): Protocol {
        val injuryType = parseInjuryType(input.injury)
        val severity = parseSeverity(input.severity)
        val steps = protocols[injuryType]?.get(severity) ?: fallbackProtocol.steps
        return Protocol(injuryType = injuryType, severity = severity, steps = steps)
    }

    fun buildScriptPrompt(protocol: Protocol, input: TriageInput): String {
        val profileContext = buildProfileContext(input.userProfile)
        val suppliesContext = if (input.supplies.isNotEmpty())
            "Available supplies: ${input.supplies.joinToString(", ")}." else ""

        return """
You are a calm, clear 911 emergency dispatcher giving life-saving instructions.
$profileContext
$suppliesContext
Injury: ${input.injury} on ${input.bodyPart}, severity: ${input.severity}.

Convert these protocol steps into numbered instructions in your dispatcher voice.
Be direct, use plain language, and keep each line to one action.
Do not add steps beyond what is listed.

Protocol steps:
${protocol.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")}

Output only the numbered lines. No intro, no closing remarks.
        """.trimIndent()
    }

    fun buildRegenerationPrompt(context: RegenerationContext, input: TriageInput): String {
        val readSteps = context.currentProtocol.steps
            .filterIndexed { i, _ -> i in context.linesRead }
            .mapIndexed { i, s -> "${context.linesRead[i] + 1}. $s" }
            .joinToString("\n")

        val remainingSteps = context.currentProtocol.steps
            .filterIndexed { i, _ -> i !in context.linesRead }
            .mapIndexed { i, s -> "${context.linesRead.size + i + 1}. $s" }
            .joinToString("\n")

        return """
You are a calm, clear 911 emergency dispatcher giving life-saving instructions.
Injury: ${input.injury} on ${input.bodyPart}, severity: ${input.severity}.

Instructions already given:
$readSteps

The person just said: "${context.userInput}"

Remaining protocol steps to adapt:
$remainingSteps

Rewrite only the remaining steps incorporating what they just told you.
Be direct, use plain language, one action per line.
Output only the numbered lines continuing from line ${context.linesRead.size + 1}.
        """.trimIndent()
    }

    private fun parseInjuryType(injury: String): InjuryType {
        return when {
            injury.contains("bleed", ignoreCase = true) ||
            injury.contains("lacerat", ignoreCase = true) ||
            injury.contains("cut", ignoreCase = true) ||
            injury.contains("wound", ignoreCase = true) -> InjuryType.SEVERE_BLEEDING

            injury.contains("fracture", ignoreCase = true) ||
            injury.contains("break", ignoreCase = true) ||
            injury.contains("broken", ignoreCase = true) -> InjuryType.FRACTURE

            injury.contains("burn", ignoreCase = true) -> InjuryType.BURN

            injury.contains("chok", ignoreCase = true) ||
            injury.contains("airway", ignoreCase = true) ||
            injury.contains("obstruct", ignoreCase = true) -> InjuryType.AIRWAY_OBSTRUCTION

            else -> InjuryType.UNKNOWN
        }
    }

    private fun parseSeverity(severity: String): Severity {
        return when (severity.uppercase()) {
            "RED" -> Severity.RED
            "GREEN" -> Severity.GREEN
            else -> Severity.YELLOW
        }
    }

    private fun buildProfileContext(profile: UserProfile): String {
        val parts = mutableListOf<String>()
        if (profile.bloodType.isNotBlank()) parts.add("Blood type: ${profile.bloodType}.")
        if (profile.allergies.isNotEmpty()) parts.add("Allergies: ${profile.allergies.joinToString(", ")}.")
        if (profile.medications.isNotEmpty()) parts.add("Current medications: ${profile.medications.joinToString(", ")}.")
        if (profile.conditions.isNotEmpty()) parts.add("Medical conditions: ${profile.conditions.joinToString(", ")}.")
        return parts.joinToString(" ")
    }
}
