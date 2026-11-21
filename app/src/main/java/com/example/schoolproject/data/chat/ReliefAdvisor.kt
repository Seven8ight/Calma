package com.example.schoolproject.data.chat

/** Turns the model's strongest drivers into a short, practical relief suggestion. */
object ReliefAdvisor {

    // Only factors a student can act on are shown. Coping by social support and the habits
    // answers are left out: they are weak, easily misread signals in a small survey.
    private val actionable = listOf(
        "Peer_Pressure",
        "Academic_Pressure_Home",
        "Academic_Competition",
        "Study_Environment_Noisy",
        "Study_Environment_disrupted",
        "Coping_Strategy_Emotional",
    )

    fun pickDriver(drivers: List<String>): String? =
        drivers.firstOrNull { d -> actionable.any { d.startsWith(it, ignoreCase = true) } }

    fun tipFor(level: String, driver: String?): String {
        if (level.equals("Low", ignoreCase = true)) {
            return "That's a healthy place to be. Keep your routines, and a short break between study blocks helps it stay that way."
        }
        val tip = when {
            driver == null ->
                "Try a short break, some water and a 10-minute walk, then pick one small task to start with."
            driver.startsWith("Peer_Pressure", true) ->
                "Pressure from classmates seems to weigh most. Try comparing yourself less, and study with one or two people you trust instead of the whole group."
            driver.startsWith("Academic_Pressure_Home", true) ->
                "Expectations from home seem to weigh most. Sharing one honest update with a family member, or talking to a counsellor, can lighten that."
            driver.startsWith("Academic_Competition", true) ->
                "Competition seems to weigh most. Try measuring yourself against your own last result and set one small goal for today."
            driver.startsWith("Study_Environment", true) ->
                "Your study space seems to weigh most. Try a library or an empty classroom, or save your hardest work for the quietest hours."
            driver.startsWith("Coping_Strategy_Emotional", true) ->
                "When things build up, try the 4-7-8 breathing activity before it gets to a breakdown, and tell someone you trust how you feel."
            else ->
                "Try a short break, some water and a 10-minute walk, then pick one small task to start with."
        }
        return if (level.equals("High", ignoreCase = true)) {
            "$tip\n\nIf it feels like too much, a campus counsellor can help. Tap \"Talk to a counsellor\"."
        } else tip
    }
}
