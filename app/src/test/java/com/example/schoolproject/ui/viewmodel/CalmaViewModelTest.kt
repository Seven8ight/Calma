package com.example.schoolproject.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.schoolproject.data.models.*
import com.example.schoolproject.data.remote.*
import com.example.schoolproject.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalmaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeRepository
    private lateinit var vm: CalmaViewModel
    private lateinit var store: ViewModelStore

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = FakeRepository()
        vm = CalmaViewModel(repo)
        store = ViewModelStore().apply { put("test", vm) }
    }
    @After fun teardown() { store.clear(); Dispatchers.resetMain() }
    private fun TestScope.login(id: String = "user-a") {
        repo.sessions.value = CalmaSession.SignedIn(id)
        advanceUntilIdle()
    }

    @Test fun restoredSessionLoadsRealUserData() = runTest(dispatcher) {
        login()
        assertEquals("user-a", vm.userProfile.value.name)
        assertEquals(1, vm.userProfile.value.checkInsCount)
        assertEquals("persisted-user-a", vm.chatMessages.value.single().text)
        assertTrue(vm.dataReady.value)
    }

    @Test fun confirmationRequiredDoesNotAuthenticate() = runTest(dispatcher) {
        repo.sessions.value = CalmaSession.SignedOut
        advanceUntilIdle()
        vm.signUpWithSupabase("student@example.com", "password123", "Student")
        advanceUntilIdle()
        assertTrue(vm.authUIState.value is AuthUIState.EmailConfirmationRequired)
        assertEquals(CalmaSession.SignedOut, vm.session.value)
        assertEquals("Student", repo.signupName)
    }

    @Test fun signInUsesCredentialsAndLoadsAccount() = runTest(dispatcher) {
        repo.sessions.value = CalmaSession.SignedOut
        advanceUntilIdle()
        vm.loginWithSupabase(" student@example.com ", "password123")
        advanceUntilIdle()
        assertEquals("student@example.com", repo.loginEmail)
        assertEquals(AuthUIState.Success, vm.authUIState.value)
        assertTrue(vm.dataReady.value)
    }

    @Test fun invalidLoginHasUsefulErrorAndNoAccountData() = runTest(dispatcher) {
        repo.failAuth = true
        vm.loginWithSupabase("student@example.com", "bad-password")
        advanceUntilIdle()
        assertEquals("Incorrect email or password. Please try again.", (vm.authUIState.value as AuthUIState.Error).message)
        assertFalse(vm.dataReady.value)
    }

    @Test fun failedCheckInDoesNotIncrementCountOrNavigate() = runTest(dispatcher) {
        login()
        repo.failWrite = true
        var navigated = false
        vm.submitCheckIn { navigated = true }
        advanceUntilIdle()
        assertFalse(navigated)
        assertEquals(1, vm.userProfile.value.checkInsCount)
        assertNotNull(vm.error.value)
    }

    @Test fun failedBookingDoesNotConfirmOrInventRecord() = runTest(dispatcher) {
        login()
        repo.failWrite = true
        var confirmed = false
        vm.createBooking { confirmed = true }
        advanceUntilIdle()
        assertFalse(confirmed)
        assertTrue(vm.bookings.value.isEmpty())
        assertNull(vm.lastBooking.value)
        assertFalse(vm.busy.value)
    }

    @Test fun bookingKeepsServerIdAndMultipleUpcomingSessions() = runTest(dispatcher) {
        login()
        vm.createBooking()
        advanceUntilIdle()
        vm.selectSlot(repo.slot.copy(id = "slot-2", slot_time = "11:00 AM"))
        vm.createBooking()
        advanceUntilIdle()
        assertEquals(2, vm.bookings.value.size)
        assertEquals("server-2", vm.lastBooking.value?.id)
        assertEquals("user-a", repo.lastUserId)
    }

    @Test fun cancellationTargetsOnlyChosenBooking() = runTest(dispatcher) {
        login()
        vm.createBooking()
        advanceUntilIdle()
        vm.selectSlot(repo.slot.copy(id = "slot-2"))
        vm.createBooking()
        advanceUntilIdle()
        vm.openCancelDialog("server-1")
        vm.confirmCancelBooking()
        advanceUntilIdle()
        assertEquals("server-1", repo.cancelledId)
        assertEquals(BookingStatus.CANCELLED, vm.bookings.value.first { it.id == "server-1" }.status)
        assertEquals(BookingStatus.UPCOMING, vm.bookings.value.first { it.id == "server-2" }.status)
    }

    @Test fun chatPersistsBothSidesForCurrentUser() = runTest(dispatcher) {
        login()
        vm.sendMessage("Hello")
        advanceUntilIdle()
        assertEquals(listOf(MessageSender.USER, MessageSender.BOT), repo.savedMessages.map { it.sender })
        assertEquals("user-a", repo.lastUserId)
        assertEquals(3, vm.chatMessages.value.size)
    }

    @Test fun failedChatKeepsInputAndDoesNotAddFakeMessages() = runTest(dispatcher) {
        login()
        repo.failWrite = true
        var clearedInput = false
        vm.sendMessage("Hello") { clearedInput = true }
        advanceUntilIdle()
        assertFalse(clearedInput)
        assertEquals(1, vm.chatMessages.value.size)
    }

    @Test fun logoutClearsAllPersonalData() = runTest(dispatcher) {
        login()
        vm.createBooking()
        advanceUntilIdle()
        vm.logout()
        advanceUntilIdle()
        assertEquals(CalmaSession.SignedOut, vm.session.value)
        assertEquals(UserProfile(), vm.userProfile.value)
        assertTrue(vm.bookings.value.isEmpty())
        assertTrue(vm.chatMessages.value.isEmpty())
        assertNull(vm.lastBooking.value)
        assertFalse(vm.dataReady.value)
    }

    @Test fun switchingAccountCancelsInFlightSaveAndOldCallback() = runTest(dispatcher) {
        login()
        repo.writeGate = CompletableDeferred()
        var confirmed = false
        vm.createBooking { confirmed = true }
        runCurrent()
        repo.sessions.value = CalmaSession.SignedIn("user-b")
        runCurrent()
        repo.writeGate!!.complete(Unit)
        advanceUntilIdle()
        assertFalse(confirmed)
        assertEquals("user-b", vm.userProfile.value.name)
        assertTrue(vm.bookings.value.isEmpty())
        assertEquals("persisted-user-b", vm.chatMessages.value.single().text)
    }

    @Test fun failedLoadCanRetryWithoutLoggingInAgain() = runTest(dispatcher) {
        repo.failLoad = true
        login()
        assertFalse(vm.dataReady.value)
        assertNotNull(vm.error.value)
        repo.failLoad = false
        vm.reloadData()
        advanceUntilIdle()
        assertTrue(vm.dataReady.value)
        assertNull(vm.error.value)
    }

    @Test fun duplicateTapSubmitsOnlyOneBooking() = runTest(dispatcher) {
        login()
        vm.createBooking()
        vm.createBooking()
        advanceUntilIdle()
        assertEquals(1, repo.bookingCount)
    }
    @Test fun breathingCannotSaveBeforeTimerFinishes() = runTest(dispatcher) {
        login()
        vm.saveActivity()
        assertTrue(repo.activities.isEmpty())
        assertFalse(vm.breathingState.value.isSaved)
    }

    @Test fun timerFinishesAtZeroAndWaitsForSave() = runTest(dispatcher) {
        login()
        vm.toggleBreathingPlayPause()
        advanceTimeBy(120_000); runCurrent()
        assertEquals(0, vm.breathingState.value.secondsRemaining)
        assertFalse(vm.breathingState.value.isPlaying)
        assertTrue(vm.breathingState.value.canSave)
        assertFalse(vm.breathingState.value.isSaved)
        vm.toggleBreathingPlayPause()
        assertEquals(0, vm.breathingState.value.secondsRemaining)
        assertTrue(repo.activities.isEmpty())
    }

    @Test fun pauseAndResumeDoNotCreateTwoTimers() = runTest(dispatcher) {
        login()
        vm.toggleBreathingPlayPause()
        advanceTimeBy(10_000); runCurrent()
        vm.pauseBreathing()
        advanceTimeBy(20_000); runCurrent()
        assertEquals(110, vm.breathingState.value.secondsRemaining)
        vm.toggleBreathingPlayPause()
        advanceTimeBy(1000); runCurrent()
        assertEquals(109, vm.breathingState.value.secondsRemaining)
        vm.pauseBreathing()
    }

    @Test fun completedSessionSavesOnceAndReloadsInHistory() = runTest(dispatcher) {
        login()
        vm.toggleBreathingPlayPause()
        advanceUntilIdle()
        vm.updateGratitudeNote("A good day")
        vm.updateStressDecrease(25f)
        vm.saveActivity(); vm.saveActivity()
        advanceUntilIdle()
        assertTrue(vm.breathingState.value.isSaved)
        assertEquals(1, repo.activities.size)
        val saved = repo.activities.values.single()
        assertEquals("user-a", saved.user_id)
        assertEquals(120, saved.duration_seconds)
        assertEquals(25, saved.stress_decrease_percent)
        vm.saveActivity()
        advanceUntilIdle()
        assertEquals(1, repo.activities.size)
        vm.reloadData()
        advanceUntilIdle()
        assertEquals(1, vm.activities.value.size)
        assertEquals(1, vm.userProfile.value.activitiesCount)
    }

    @Test fun uncertainSaveRetryUsesSameIdWithoutDoubleCounting() = runTest(dispatcher) {
        login()
        vm.startNewActivity(ActivityType.GRATITUDE_JOURNAL)
        vm.updateGratitudeNote("My friends")
        val id = vm.breathingState.value.id
        repo.failAfterActivityWrite = true
        vm.saveActivity()
        advanceUntilIdle()
        assertFalse(vm.breathingState.value.isSaved)
        assertEquals("My friends", vm.breathingState.value.gratitudeNote)
        assertNotNull(vm.breathingState.value.saveError)
        vm.saveActivity()
        advanceUntilIdle()
        assertTrue(vm.breathingState.value.isSaved)
        assertEquals(id, repo.activities.values.single().id)
        assertEquals(1, vm.userProfile.value.activitiesCount)
    }

    @Test fun journalNeedsANoteAndDoesNotPretendBreathingWasDone() = runTest(dispatcher) {
        login()
        vm.startNewActivity(ActivityType.GRATITUDE_JOURNAL)
        vm.updateGratitudeNote("   ")
        vm.saveActivity()
        advanceUntilIdle()
        assertTrue(repo.activities.isEmpty())
        vm.updateGratitudeNote("Family")
        vm.saveActivity()
        advanceUntilIdle()
        val saved = repo.activities.values.single()
        assertEquals("GRATITUDE_JOURNAL", saved.activity_type)
        assertEquals(0, saved.duration_seconds)
        assertNull(saved.stress_decrease_percent)
    }

    @Test fun resetClearsDraftButPreservesSavedHistory() = runTest(dispatcher) {
        login()
        vm.startNewActivity(ActivityType.GRATITUDE_JOURNAL)
        vm.updateGratitudeNote("Family")
        vm.saveActivity()
        advanceUntilIdle()
        val id = vm.breathingState.value.id
        vm.startNewActivity(ActivityType.BREATHING_478)
        assertNotEquals(id, vm.breathingState.value.id)
        assertEquals(120, vm.breathingState.value.secondsRemaining)
        assertEquals("", vm.breathingState.value.gratitudeNote)
        assertFalse(vm.breathingState.value.isSaved)
        assertEquals(1, vm.activities.value.size)
    }

    @Test fun switchingAccountsClearsActivityDraftAndHistory() = runTest(dispatcher) {
        login()
        vm.startNewActivity(ActivityType.GRATITUDE_JOURNAL)
        vm.updateGratitudeNote("Private note")
        vm.saveActivity()
        advanceUntilIdle()
        login("user-b")
        assertTrue(vm.activities.value.isEmpty())
        assertEquals("", vm.breathingState.value.gratitudeNote)
        assertEquals(0, vm.userProfile.value.activitiesCount)
    }

    @Test fun logoutCancelsAnInFlightActivitySave() = runTest(dispatcher) {
        login()
        vm.startNewActivity(ActivityType.GRATITUDE_JOURNAL)
        vm.updateGratitudeNote("My note")
        repo.writeGate = CompletableDeferred()
        vm.saveActivity()
        runCurrent()
        vm.logout()
        runCurrent()
        repo.writeGate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.activities.value.isEmpty())
        assertFalse(vm.breathingState.value.isSaved)
        assertTrue(repo.activities.isEmpty())
    }

    @Test fun profileChangesOnlyAfterSuccessfulSave() = runTest(dispatcher) {
        login()
        repo.failWrite = true
        vm.updateProfileName("New name") { fail("Should not report success") }
        advanceUntilIdle()
        assertEquals("user-a", vm.userProfile.value.name)
        repo.failWrite = false
        vm.updateProfileName("New name") { }
        advanceUntilIdle()
        assertEquals("New name", vm.userProfile.value.name)
        vm.updateReminderTime("7:30 PM")
        advanceUntilIdle()
        assertEquals("7:30 PM", vm.userProfile.value.reminderTime)
    }

}

private class FakeRepository : CalmaDataSource {
    override val sessions = MutableStateFlow<CalmaSession>(CalmaSession.Initializing)
    val counsellor = Counsellor("c1", "Counsellor", "Title", "", "", true, 0xFFCDE2D8)
    val slot = CounsellorSlotDto("slot-1", "c1", "2030-01-01", "9:00 AM")
    val activities = linkedMapOf<String, ActivityDto>()
    var failAfterActivityWrite = false
    var failAuth = false
    var failWrite = false
    var failLoad = false
    var bookingCount = 0
    var lastUserId: String? = null
    var cancelledId: String? = null
    var loginEmail: String? = null
    var signupName: String? = null
    var writeGate: CompletableDeferred<Unit>? = null
    val savedMessages = mutableListOf<ChatMessage>()
    override suspend fun signUp(email: String, password: String, name: String): Boolean { signupName = name; return false }
    override suspend fun signIn(email: String, password: String) {
        if (failAuth) error("invalid_credentials")
        loginEmail = email
        sessions.value = CalmaSession.SignedIn("user-a")
    }
    override suspend fun signOut() { sessions.value = CalmaSession.SignedOut }
    override suspend fun fetchUserProfile(userId: String): UserProfile {
        if (failLoad) error("offline")
        return UserProfile(name = userId)
    }
    override suspend fun getCounsellors() = listOf(counsellor)
    override suspend fun fetchBookings(userId: String, counsellors: List<Counsellor>) = emptyList<Booking>()
    override suspend fun fetchChatMessages(userId: String) = listOf(ChatMessage("saved", MessageSender.USER, "persisted-$userId"))
    override suspend fun fetchCheckIns(userId: String) = listOf(CheckInDto(user_id = userId, sleep_hours = 8f, stress_level = 3, time_of_day = "MORNING"))
    override suspend fun fetchSlots() = listOf(slot)
    private suspend fun write(userId: String) {
        lastUserId = userId
        writeGate?.await()
        if (failWrite) error("offline")
    }
    override suspend fun saveCheckIn(userId: String, checkIn: CheckInState): CheckInDto {
        write(userId)
        return CheckInDto(user_id = userId, sleep_hours = checkIn.sleepHours,
            stress_level = checkIn.stressLevel, time_of_day = checkIn.timeOfDay.name,
            created_at = java.time.OffsetDateTime.now().toString())
    }
    override suspend fun createBooking(userId: String, counsellor: Counsellor, slot: CounsellorSlotDto): Booking {
        write(userId)
        bookingCount++
        return Booking("server-$bookingCount", counsellor, slot.slot_date, slot.slot_time, BookingStatus.UPCOMING)
    }
    override suspend fun cancelBooking(userId: String, bookingId: String) { write(userId); cancelledId = bookingId }
    override suspend fun saveChatMessage(userId: String, sender: MessageSender, text: String): ChatMessage {
        write(userId)
        return ChatMessage("message-${savedMessages.size}", sender, text).also { savedMessages.add(it) }
    }
    override suspend fun fetchActivities(userId: String) = activities.values.filter { it.user_id == userId }
    override suspend fun saveActivity(activity: ActivityDto): ActivityDto {
        write(activity.user_id)
        val saved = activity.copy(created_at = java.time.OffsetDateTime.now().toString())
        activities[activity.id] = saved
        if (failAfterActivityWrite) { failAfterActivityWrite = false; error("Response lost") }
        return saved
    }
    override suspend fun updateProfileName(userId: String, name: String) { write(userId) }
    override suspend fun setReminderTime(userId: String, time: String) { write(userId) }
    override suspend fun setNotifications(userId: String, enabled: Boolean) { write(userId) }
}
