package com.example.schoolproject.data.chat


data class BotReply(
    val text: String,
    val chips: List<String>,
    /** Ask the UI to switch to the Counselling tab. */
    val openCounselling: Boolean = false,
)

/** Answers collected so far during a check-in. Null progress means no check-in is running. */
data class CheckInProgress(val answers: List<String> = emptyList())

data class ChatTurn(val reply: BotReply, val progress: CheckInProgress?)

/**
 * Rule-based support companion. A short check-in collects the same six self-reported answers
 * the notebook model was trained on, scores them on the device and replies with a relief tip.
 * The caller keeps the CheckInProgress and commits it once the reply is saved.
 */
class ChatEngine(private val model: StressModel = StressModel()) {

    private class Question(val prompt: String, val options: List<String>, val numeric: Boolean)

    private val scale = listOf("1", "2", "3", "4", "5")
    private val questions = listOf(
        Question("1/6 How much pressure do you feel from classmates or friends? (1 = none, 5 = a lot)", scale, true),
        Question("2/6 How much academic pressure do you feel from home? (1 = none, 5 = a lot)", scale, true),
        Question("3/6 How competitive does your academic life feel? (1 = not at all, 5 = very)", scale, true),
        Question("4/6 What is your study space usually like?", listOf("Peaceful", "Noisy", "Disrupted"), false),
        Question(
            "5/6 When things get tough, what do you usually do?",
            listOf("Think it through", "Lean on people", "Break down and cry"), false,
        ),
        Question(
            "6/6 Do you have daily habits like smoking or drinking? You can skip this.",
            listOf("No", "Yes", "Prefer not to say"), false,
        ),
    )

    // Chip label -> category string used when the model was trained (matched ignoring case).
    private val categoryValue = mapOf(
        "Peaceful" to "Peaceful",
        "Noisy" to "Noisy",
        "Disrupted" to "disrupted",
        "Think it through" to "Analyze the situation and handle it with intellect",
        "Lean on people" to "Social support (friends, family)",
        "Break down and cry" to "Emotional breakdown (crying a lot)",
        "No" to "No",
        "Yes" to "Yes",
        "Prefer not to say" to "prefer not to say",
    )

    private val crisisPhrases = listOf(
        "kill myself", "end my life", "suicid", "hurt myself", "harm myself", "self harm",
        "want to die", "wanna die", "do not want to live", "dont want to live", "no reason to live",
        "end it all", "better off dead", "take my own life",
    )
    private val cancelWords = listOf("cancel", "stop", "skip the check", "never mind", "nevermind", "quit", "exit")
    private val activities = listOf(
        "Try the 4-7-8 breathing activity: open the Home tab and tap \"Open breathing & journal\". It takes about two minutes.",
        "Try the gratitude journal: open the Home tab, tap \"Open breathing & journal\" and write down one thing that went well today.",
        "Step outside for 10 minutes and name five things you can see. Then pick one small task to start with.",
    )
    private var activityIndex = 0

    fun respond(input: String, progress: CheckInProgress? = null): ChatTurn {
        val text = input.trim()
        val t = normalise(text)

        if (crisisPhrases.any { it in t }) return ChatTurn(crisisReply(), null)
        if (progress != null) return continueCheckIn(text, t, progress)

        return when {
            listOf("counsell", "therapist", "talk to someone", "speak to someone").any { it in t } ->
                ChatTurn(
                    BotReply(
                        "Opening the Counselling tab for you. You can choose a campus counsellor and book a time there.",
                        DEFAULT_CHIPS, openCounselling = true,
                    ),
                    null,
                )
            listOf("anxious", "anxiety", "stress", "overwhelm", "worried", "panic", "check in", "check-in").any { it in t } ->
                ChatTurn(
                    BotReply(
                        "I'm sorry you're feeling that way. Let's do a quick 6-question check-in. You can type \"cancel\" at any time.\n\n" +
                                questions[0].prompt,
                        questions[0].options,
                    ),
                    CheckInProgress(),
                )
            listOf("activity", "breath", "exercise", "relax", "calming").any { it in t } ->
                ChatTurn(BotReply(nextActivity(), listOf("I feel anxious", "Talk to a counsellor")), null)
            else ->
                ChatTurn(
                    BotReply(
                        "Hi, I'm Calmly Companion, an automated assistant and not a live counsellor. " +
                                "I can run a quick check-in, suggest an activity, or point you to a counsellor.",
                        DEFAULT_CHIPS,
                    ),
                    null,
                )
        }
    }

    private fun continueCheckIn(text: String, t: String, progress: CheckInProgress): ChatTurn {
        if (cancelWords.any { it in t }) {
            return ChatTurn(BotReply("No problem. I'm here whenever you want to try again.", DEFAULT_CHIPS), null)
        }
        val q = questions[progress.answers.size]
        val answer = parseAnswer(q, text, t)
            ?: return ChatTurn(BotReply("Please pick one of the options below, or type \"cancel\" to stop.\n\n${q.prompt}", q.options), progress)

        val next = CheckInProgress(progress.answers + answer)
        return if (next.answers.size < questions.size) {
            val nq = questions[next.answers.size]
            ChatTurn(BotReply(nq.prompt, nq.options), next)
        } else {
            ChatTurn(finish(next.answers), null)
        }
    }

    private fun parseAnswer(q: Question, text: String, t: String): String? =
        if (q.numeric) Regex("\\b([1-5])\\b").find(text)?.groupValues?.get(1)
        else q.options.firstOrNull { it.equals(text, ignoreCase = true) }
            ?: q.options.firstOrNull { normalise(it) in t }

    private fun finish(answers: List<String>): BotReply {
        val result = try {
            model.predict(
                numeric = answers.take(3).map { it.toDouble() },
                categorical = answers.drop(3).map { categoryValue.getValue(it) },
            )
        } catch (_: IllegalArgumentException) {
            return BotReply("I couldn't work that out just now. You can try the check-in again or talk to a counsellor.", DEFAULT_CHIPS)
        } catch (_: IllegalStateException) {
            return BotReply("I couldn't work that out just now. You can try the check-in again or talk to a counsellor.", DEFAULT_CHIPS)
        }
        val word = when (result.level) { "Low" -> "on the lower side"; "Medium" -> "moderate"; else -> "high" }
        val tip = ReliefAdvisor.tipFor(result.level, ReliefAdvisor.pickDriver(result.drivers))
        val chips = if (result.level == "High") listOf("Talk to a counsellor", "Suggest activity")
        else listOf("Suggest activity", "Talk to a counsellor")
        return BotReply(
            "Thanks for sharing. From your answers, your stress looks $word right now.\n\n$tip\n\n" +
                    "This is a quick check-in based on a small student survey, not a diagnosis.",
            chips,
        )
    }

    private fun crisisReply() = BotReply(
        "I'm really sorry you're going through this. I'm an automated companion and can't give the help you need right now. " +
                "Please contact your campus counselling office or local emergency services, or reach out to someone you trust and stay with them. " +
                "You deserve support from a real person.",
        listOf("Talk to a counsellor"),
    )

    private fun nextActivity() = activities[activityIndex++ % activities.size]

    private fun normalise(s: String) = s.lowercase()
        .replace('\u2019', '\'').replace("'", "").replace("-", " ").replace(Regex("\\s+"), " ").trim()

    companion object {
        val DEFAULT_CHIPS = listOf("I feel anxious", "Talk to a counsellor", "Suggest activity")
    }
}
