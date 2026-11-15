package com.example.schoolproject

import com.example.schoolproject.data.models.*
import com.example.schoolproject.data.remote.*
import com.example.schoolproject.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow

// UI tests use this instead of the real account on the emulator.
class TestRepository : CalmaDataSource {
    override val sessions = MutableStateFlow<CalmaSession>(CalmaSession.SignedOut)
    override suspend fun signUp(email: String, password: String, name: String) = false
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun fetchUserProfile(userId: String) = UserProfile(name = "Test student")
    override suspend fun getCounsellors() = emptyList<Counsellor>()
    override suspend fun fetchBookings(userId: String, counsellors: List<Counsellor>) = emptyList<Booking>()
    override suspend fun fetchChatMessages(userId: String) = emptyList<ChatMessage>()
    override suspend fun fetchCheckIns(userId: String) = emptyList<CheckInDto>()
    override suspend fun fetchSlots() = emptyList<CounsellorSlotDto>()
    override suspend fun saveCheckIn(userId: String, checkIn: CheckInState): CheckInDto = error("Not used by UI test")
    override suspend fun createBooking(userId: String, counsellor: Counsellor, slot: CounsellorSlotDto): Booking = error("Not used by UI test")
    override suspend fun cancelBooking(userId: String, bookingId: String) = Unit
    override suspend fun saveChatMessage(userId: String, sender: MessageSender, text: String): ChatMessage = error("Not used by UI test")
    override suspend fun setNotifications(userId: String, enabled: Boolean) = Unit
    override suspend fun setReminderTime(userId: String, time: String) = Unit
    override suspend fun updateProfileName(userId: String, name: String) = Unit
    override suspend fun fetchActivities(userId: String) = emptyList<ActivityDto>()
    override suspend fun saveActivity(activity: ActivityDto) = activity
}
