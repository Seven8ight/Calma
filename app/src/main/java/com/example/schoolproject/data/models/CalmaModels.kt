package com.example.schoolproject.data.models

enum class TimeOfDay(val displayName: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening")
}

enum class BookingStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED
}

enum class MessageSender {
    USER,
    BOT
}

data class Counsellor(
    val id: String,
    val name: String,
    val title: String,
    val specialties: String,
    val availabilityStatus: String,
    val isAvailableToday: Boolean,
    val avatarColorHex: Long
)

data class Booking(
    val id: String,
    val counsellor: Counsellor,
    val dateSlot: String,
    val timeSlot: String,
    val status: BookingStatus
)

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String = "Just now"
)

data class CheckInState(
    val sleepHours: Float = 7f,
    val stressLevel: Int = 5,
    val timeOfDay: TimeOfDay = TimeOfDay.MORNING
)

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val streakDays: Int = 0,
    val checkInsCount: Int = 0,
    val activitiesCount: Int = 0,
    val notificationsEnabled: Boolean = true,
    val reminderTime: String = "8:00 PM"
)

enum class ActivityType(val title: String) {
    BREATHING_478("Breathing"),
    GRATITUDE_JOURNAL("Gratitude journal")
}

data class BreathingSessionState(
    // Reuse this ID when retrying a save, so the database cannot record it twice.
    val id: String = java.util.UUID.randomUUID().toString(),
    val activityType: ActivityType = ActivityType.BREATHING_478,
    val totalSeconds: Int = 120,
    val secondsRemaining: Int = 120,
    val isPlaying: Boolean = false,
    val gratitudeNote: String = "",
    val stressDecreasePercent: Float? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
) {
    val isComplete: Boolean get() = secondsRemaining == 0
    val canSave: Boolean get() = !isSaved && !isSaving &&
        if (activityType == ActivityType.BREATHING_478) isComplete else gratitudeNote.isNotBlank()
    val hasDraft: Boolean get() = !isSaved && (secondsRemaining < totalSeconds || gratitudeNote.isNotBlank())

    // A cycle has 4 seconds in, 7 holding, and 8 out: 19 seconds in total.
    val breathingInstruction: String get() {
        if (isComplete) return "Session complete"
        if (!isPlaying) return if (secondsRemaining == totalSeconds) "Ready when you are" else "Paused"
        val cycleSecond = (totalSeconds - secondsRemaining) % 19
        return when {
            cycleSecond < 4 -> "Breathe in · ${4 - cycleSecond}"
            cycleSecond < 11 -> "Hold · ${11 - cycleSecond}"
            else -> "Breathe out · ${19 - cycleSecond}"
        }
    }
}
