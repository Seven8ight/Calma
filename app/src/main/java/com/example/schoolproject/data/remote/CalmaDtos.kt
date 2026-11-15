package com.example.schoolproject.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "user",
    val streak_days: Int = 0,
    val check_ins_count: Int = 0,
    val activities_count: Int = 0,
    val notifications_enabled: Boolean = true,
    val reminder_time: String = "8:00 PM"
)

@Serializable
data class CheckInDto(
    val id: String? = null,
    val user_id: String,
    val sleep_hours: Float,
    val stress_level: Int,
    val time_of_day: String,
    val wellbeing_score: Int? = null,
    val created_at: String? = null
)

@Serializable
data class CounsellorDto(
    val id: String,
    val name: String,
    val title: String,
    val specialties: String,
    val avatar_color_hex: Long = 13754904,
    val is_available_today: Boolean = true,
    val availability_status: String = "Available today"
)

@Serializable
data class BookingDto(
    val id: String? = null,
    val user_id: String,
    val counsellor_id: String,
    val date_slot: String,
    val time_slot: String,
    val status: String = "UPCOMING"
)

@Serializable
data class ChatMessageDto(
    val id: String? = null,
    val user_id: String,
    val sender: String,
    val message_text: String,
    val created_at: String? = null
)

@Serializable
data class CounsellorSlotDto(
    val id: String,
    val counsellor_id: String,
    val slot_date: String,
    val slot_time: String,
    val is_booked: Boolean = false
)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class ActivityDto(
    val id: String,
    val user_id: String,
    val activity_type: String,
    val duration_seconds: Int,
    val gratitude_note: String? = null,
    @kotlinx.serialization.EncodeDefault
    val stress_decrease_percent: Int? = null,
    val created_at: String? = null
)
