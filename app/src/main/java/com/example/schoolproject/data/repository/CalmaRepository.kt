package com.example.schoolproject.data.repository

import com.example.schoolproject.data.models.*
import com.example.schoolproject.data.remote.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface CalmaSession {
    data object Initializing : CalmaSession
    data object SignedOut : CalmaSession
    data class SignedIn(val userId: String) : CalmaSession
}

interface CalmaDataSource {
    val sessions: Flow<CalmaSession>
    suspend fun signUp(email: String, password: String, name: String): Boolean
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
    suspend fun fetchUserProfile(userId: String): UserProfile
    suspend fun getCounsellors(): List<Counsellor>
    suspend fun fetchBookings(userId: String, counsellors: List<Counsellor>): List<Booking>
    suspend fun fetchChatMessages(userId: String): List<ChatMessage>
    suspend fun fetchCheckIns(userId: String): List<CheckInDto>
    suspend fun fetchSlots(): List<CounsellorSlotDto>
    suspend fun saveCheckIn(userId: String, checkIn: CheckInState): CheckInDto
    suspend fun createBooking(userId: String, counsellor: Counsellor, slot: CounsellorSlotDto): Booking
    suspend fun cancelBooking(userId: String, bookingId: String)
    suspend fun saveChatMessage(userId: String, sender: MessageSender, text: String): ChatMessage
    suspend fun fetchActivities(userId: String): List<ActivityDto>
    suspend fun saveActivity(activity: ActivityDto): ActivityDto
    suspend fun updateProfileName(userId: String, name: String)
    suspend fun setReminderTime(userId: String, time: String)
    suspend fun setNotifications(userId: String, enabled: Boolean)
}

class CalmaRepository : CalmaDataSource {
    private val client = SupabaseProvider.client
    override val sessions = client.auth.sessionStatus.map { status ->
        when (status) {
            SessionStatus.Initializing -> CalmaSession.Initializing
            is SessionStatus.Authenticated -> status.session.user?.id?.let(CalmaSession::SignedIn)
                ?: CalmaSession.SignedOut
            else -> CalmaSession.SignedOut
        }
    }.distinctUntilChanged()

    override suspend fun signUp(email: String, password: String, name: String): Boolean {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("name", name) }
        }
        return client.auth.currentSessionOrNull() != null
    }

    override suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) { this.email = email; this.password = password }
    }

    override suspend fun signOut() { client.auth.signOut() }

    // Check the session at the boundary as well as enforcing ownership through database RLS.
    private fun requireUser(userId: String) {
        check(client.auth.currentUserOrNull()?.id == userId) { "Please sign in again." }
    }

    override suspend fun fetchUserProfile(userId: String): UserProfile {
        requireUser(userId)
        val dto = client.from("profiles").select { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>()
        return UserProfile(dto.name, dto.email, dto.streak_days, dto.check_ins_count,
            dto.activities_count, dto.notifications_enabled, dto.reminder_time)
    }

    override suspend fun getCounsellors() = client.from("counsellors").select {
        order("name", Order.ASCENDING)
    }.decodeList<CounsellorDto>().map { it.toModel() }

    override suspend fun fetchBookings(userId: String, counsellors: List<Counsellor>): List<Booking> {
        requireUser(userId)
        // Fetch history in small pages so older appointments are not silently cut off.
        val rows = mutableListOf<BookingDto>()
        var page: List<BookingDto>
        do {
            page = client.from("bookings").select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                range(rows.size.toLong(), rows.size.toLong() + 99)
            }.decodeList<BookingDto>()
            rows.addAll(page)
        } while (page.size == 100)
        return rows.map { dto -> dto.toModel(counsellors.first { it.id == dto.counsellor_id }) }
    }

    override suspend fun fetchChatMessages(userId: String): List<ChatMessage> {
        requireUser(userId)
        return client.from("chat_messages").select {
            filter { eq("user_id", userId) }
            order("created_at", Order.ASCENDING)
            order("id", Order.ASCENDING)
        }.decodeList<ChatMessageDto>().map { it.toModel() }
    }

    override suspend fun fetchCheckIns(userId: String): List<CheckInDto> {
        requireUser(userId)
        return client.from("daily_check_ins").select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList<CheckInDto>()
    }

    override suspend fun fetchSlots() = client.from("counsellor_slots").select {
        filter { eq("is_booked", false); gte("slot_date", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())) }
        order("slot_date", Order.ASCENDING)
        order("slot_time", Order.ASCENDING)
    }.decodeList<CounsellorSlotDto>()
        .filter { com.example.schoolproject.data.helpers.isFutureSlot(it.slot_date, it.slot_time) }
        .sortedBy { com.example.schoolproject.data.helpers.slotDateTime(it.slot_date, it.slot_time) }

    override suspend fun saveCheckIn(userId: String, checkIn: CheckInState): CheckInDto {
        requireUser(userId)
        return client.from("daily_check_ins").insert(CheckInDto(user_id = userId,
            sleep_hours = checkIn.sleepHours, stress_level = checkIn.stressLevel,
            time_of_day = checkIn.timeOfDay.name)) { select() }.decodeSingle<CheckInDto>()
    }

    override suspend fun createBooking(userId: String, counsellor: Counsellor, slot: CounsellorSlotDto): Booking {
        requireUser(userId)
        return client.from("bookings").insert(BookingDto(user_id = userId,
            counsellor_id = counsellor.id, date_slot = slot.slot_date, time_slot = slot.slot_time)) {
            select()
        }.decodeSingle<BookingDto>().toModel(counsellor)
    }

    override suspend fun cancelBooking(userId: String, bookingId: String) {
        requireUser(userId)
        client.from("bookings").update({ set("status", "CANCELLED") }) {
            filter { eq("id", bookingId); eq("user_id", userId); eq("status", "UPCOMING") }
            select()
        }.decodeSingle<BookingDto>()
    }

    override suspend fun saveChatMessage(userId: String, sender: MessageSender, text: String): ChatMessage {
        requireUser(userId)
        return client.from("chat_messages").insert(ChatMessageDto(user_id = userId,
            sender = sender.name, message_text = text)) { select() }
            .decodeSingle<ChatMessageDto>().toModel()
    }

    override suspend fun fetchActivities(userId: String): List<ActivityDto> {
        requireUser(userId)
        val rows = mutableListOf<ActivityDto>()
        var page: List<ActivityDto>
        do {
            page = client.from("user_activities").select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                range(rows.size.toLong(), rows.size.toLong() + 99)
            }.decodeList<ActivityDto>()
            rows.addAll(page)
        } while (page.size == 100)
        return rows
    }

    override suspend fun saveActivity(activity: ActivityDto): ActivityDto {
        requireUser(activity.user_id)
        // Upsert means insert once, or update that same row after a network retry.
        return client.from("user_activities").upsert(activity) { select() }.decodeSingle<ActivityDto>()
    }

    override suspend fun updateProfileName(userId: String, name: String) {
        requireUser(userId)
        client.from("profiles").update({ set("name", name) }) {
            filter { eq("id", userId) }; select()
        }.decodeSingle<ProfileDto>()
    }

    override suspend fun setReminderTime(userId: String, time: String) {
        requireUser(userId)
        client.from("profiles").update({ set("reminder_time", time) }) {
            filter { eq("id", userId) }; select()
        }.decodeSingle<ProfileDto>()
    }

    override suspend fun setNotifications(userId: String, enabled: Boolean) {
        requireUser(userId)
        client.from("profiles").update({ set("notifications_enabled", enabled) }) {
            filter { eq("id", userId) }; select()
        }.decodeSingle<ProfileDto>()
    }
}

private fun CounsellorDto.toModel() = Counsellor(id, name, title, specialties,
    availability_status, is_available_today, avatar_color_hex or 0xFF000000L)
private fun BookingDto.toModel(counsellor: Counsellor) = Booking(requireNotNull(id), counsellor,
    date_slot, time_slot, BookingStatus.valueOf(status))
private fun ChatMessageDto.toModel() = ChatMessage(requireNotNull(id), MessageSender.valueOf(sender),
    message_text, created_at ?: "")
