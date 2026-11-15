package com.example.schoolproject.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.schoolproject.data.models.*
import com.example.schoolproject.data.remote.*
import com.example.schoolproject.data.helpers.checkInStreak
import kotlin.math.roundToInt
import com.example.schoolproject.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface AuthUIState {
    data object Idle : AuthUIState
    data object Loading : AuthUIState
    data object Success : AuthUIState
    data class Error(val message: String) : AuthUIState
    data class EmailConfirmationRequired(val message: String) : AuthUIState
}

class CalmaViewModel(private val repository: CalmaDataSource = CalmaRepository()) : ViewModel() {
    private val _session = MutableStateFlow<CalmaSession>(CalmaSession.Initializing)
    val session = _session.asStateFlow()
    private val _authUIState = MutableStateFlow<AuthUIState>(AuthUIState.Idle)
    val authUIState = _authUIState.asStateFlow()
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()
    private val _checkInState = MutableStateFlow(CheckInState())
    val checkInState = _checkInState.asStateFlow()
    private var savedCheckIns = emptyList<CheckInDto>()
    private val _lastCheckIn = MutableStateFlow<CheckInDto?>(null)
    val lastCheckIn = _lastCheckIn.asStateFlow()
    private val _activities = MutableStateFlow(emptyList<ActivityDto>())
    val activities = _activities.asStateFlow()
    private val _counsellors = MutableStateFlow(emptyList<Counsellor>())
    val counsellors = _counsellors.asStateFlow()
    private val _bookings = MutableStateFlow(emptyList<Booking>())
    val bookings = _bookings.asStateFlow()
    private val emptyCounsellor = Counsellor("", "Select a counsellor in Support", "", "", "", false, 0xFFCDE2D8)
    private val _selectedCounsellorForBooking = MutableStateFlow(emptyCounsellor)
    val selectedCounsellorForBooking = _selectedCounsellorForBooking.asStateFlow()
    private val _slots = MutableStateFlow(emptyList<CounsellorSlotDto>())
    val slots = _slots.asStateFlow()
    private val _selectedSlot = MutableStateFlow<CounsellorSlotDto?>(null)
    val selectedSlot = _selectedSlot.asStateFlow()
    private val _lastBooking = MutableStateFlow<Booking?>(null)
    val lastBooking = _lastBooking.asStateFlow()
    private val _showCancelDialog = MutableStateFlow(false)
    val showCancelDialog = _showCancelDialog.asStateFlow()
    private var cancellationId: String? = null
    private val _cancellationConfirmed = MutableStateFlow(false)
    val cancellationConfirmed = _cancellationConfirmed.asStateFlow()
    private val _chatMessages = MutableStateFlow(emptyList<ChatMessage>())
    val chatMessages = _chatMessages.asStateFlow()
    private val _breathingState = MutableStateFlow(BreathingSessionState())
    val breathingState = _breathingState.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _dataReady = MutableStateFlow(false)
    val dataReady = _dataReady.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var accountJob: Job? = null
    private var accountScope: CoroutineScope? = null
    private var breathingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.sessions.distinctUntilChanged().collect { current ->
                accountJob?.cancelAndJoin()
                clearUserData()
                _session.value = current
                if (current is CalmaSession.SignedIn) {
                    _authUIState.value = AuthUIState.Success
                    accountJob = SupervisorJob(viewModelScope.coroutineContext[Job])
                    accountScope = CoroutineScope(viewModelScope.coroutineContext + accountJob!!)
                    reloadData()
                } else {
                    accountScope = null
                    if (current is CalmaSession.SignedOut && _authUIState.value == AuthUIState.Success) {
                        _authUIState.value = AuthUIState.Idle
                    }
                }
            }
        }
    }

    fun clearAuthMessage() { if (_authUIState.value != AuthUIState.Loading) _authUIState.value = AuthUIState.Idle }
    fun dismissError() { _error.value = null }

    fun loginWithSupabase(email: String, password: String) = authenticate(email, password, null)
    fun signUpWithSupabase(email: String, password: String, name: String) = authenticate(email, password, name)

    private fun authenticate(email: String, password: String, name: String?) {
        if (_authUIState.value == AuthUIState.Loading) return
        if (!email.trim().contains('@') || password.isBlank() || (name != null && (name.isBlank() || password.length < 6))) {
            _authUIState.value = AuthUIState.Error("Enter a valid email and password. For signup, add your name and use at least 6 password characters.")
            return
        }
        _authUIState.value = AuthUIState.Loading
        viewModelScope.launch {
            try {
                if (name == null) repository.signIn(email.trim(), password)
                else if (!repository.signUp(email.trim(), password, name.trim())) {
                    _authUIState.value = AuthUIState.EmailConfirmationRequired("Check your email for a confirmation link, then return here to log in.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _authUIState.value = AuthUIState.Error(authErrorMessage(e)) }
        }
    }

    fun logout() {
        if (_authUIState.value == AuthUIState.Loading) return
        _authUIState.value = AuthUIState.Loading
        viewModelScope.launch {
            try { repository.signOut(); _authUIState.value = AuthUIState.Idle }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _authUIState.value = AuthUIState.Success
                _error.value = "Could not log out. Check your connection and try again."
            }
        }
    }

    fun reloadData() = perform("Could not load your account data. Check your connection and database setup, then retry.", requireReady = false) { userId ->
        _dataReady.value = false
        val profile = repository.fetchUserProfile(userId)
        val counsellors = repository.getCounsellors()
        val bookings = repository.fetchBookings(userId, counsellors)
        val messages = repository.fetchChatMessages(userId)
        val checkIns = repository.fetchCheckIns(userId)
        val slots = repository.fetchSlots()
        val activities = repository.fetchActivities(userId)
        savedCheckIns = checkIns
        _lastCheckIn.value = checkIns.firstOrNull()
        _activities.value = activities
        _userProfile.value = profile.copy(checkInsCount = checkIns.size,
            activitiesCount = activities.size, streakDays = checkInStreak(checkIns.map { it.created_at }))
        _counsellors.value = counsellors
        _bookings.value = bookings
        _chatMessages.value = messages
        _slots.value = slots
        val selected = counsellors.find { it.id == _selectedCounsellorForBooking.value.id }
            ?: counsellors.firstOrNull() ?: emptyCounsellor
        selectCounsellorForBooking(selected)
        checkIns.firstOrNull()?.let {
            _checkInState.value = CheckInState(it.sleep_hours, it.stress_level, TimeOfDay.valueOf(it.time_of_day))
        }
        _dataReady.value = true
    }

    private fun perform(message: String, requireReady: Boolean = true, block: suspend (String) -> Unit) {
        val user = _session.value as? CalmaSession.SignedIn ?: return
        if (_busy.value || (requireReady && !_dataReady.value)) return
        val scope = accountScope ?: return
        _busy.value = true
        _error.value = null
        scope.launch {
            try { block(user.userId) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _error.value = message }
            finally { _busy.value = false }
        }
    }

    fun updateSleepHours(hours: Float) { _checkInState.update { it.copy(sleepHours = hours) } }
    fun updateStressLevel(level: Int) { _checkInState.update { it.copy(stressLevel = level) } }
    fun updateTimeOfDay(timeOfDay: TimeOfDay) { _checkInState.update { it.copy(timeOfDay = timeOfDay) } }

    fun submitCheckIn(onSuccess: () -> Unit = {}) {
        val checkIn = _checkInState.value
        perform("Your check-in could not be saved. Check your connection and try again.") { userId ->
            val saved = repository.saveCheckIn(userId, checkIn)
            savedCheckIns = listOf(saved) + savedCheckIns
            _lastCheckIn.value = saved
            _userProfile.update { it.copy(checkInsCount = savedCheckIns.size,
                streakDays = checkInStreak(savedCheckIns.map { row -> row.created_at })) }
            onSuccess()
        }
    }

    fun sendMessage(text: String, onSaved: () -> Unit = {}) {
        if (text.isBlank()) return
        perform("The conversation could not be fully saved. Any messages shown were saved; please try again.") { userId ->
            val saved = repository.saveChatMessage(userId, MessageSender.USER, text.trim())
            _chatMessages.update { it + saved }
            onSaved()
            val reply = when {
                text.contains("anxious", true) -> "Thank you for sharing. Would you like to try a breathing exercise or speak with a counsellor?"
                text.contains("counsellor", true) || text.contains("therapist", true) -> "You can find campus counsellors and their available sessions in the Counselling tab."
                else -> "Thank you for sharing. You can try a breathing activity or book a conversation with a counsellor."
            }
            val bot = repository.saveChatMessage(userId, MessageSender.BOT, reply)
            _chatMessages.update { it + bot }
        }
    }

    fun selectCounsellorForBooking(counsellor: Counsellor) {
        _selectedCounsellorForBooking.value = counsellor
        _selectedSlot.value = _slots.value.firstOrNull { it.counsellor_id == counsellor.id }
        _cancellationConfirmed.value = false
    }
    fun selectSlot(slot: CounsellorSlotDto) { _selectedSlot.value = slot }

    fun createBooking(onSuccess: () -> Unit = {}) {
        val counsellor = _selectedCounsellorForBooking.value
        val slot = _selectedSlot.value ?: return
        if (slot.counsellor_id != counsellor.id) return
        if (!com.example.schoolproject.data.helpers.isFutureSlot(slot.slot_date, slot.slot_time)) {
            _error.value = "This session time has passed. Refresh availability and choose another slot."
            return
        }
        perform("The booking could not be saved. The slot may have been taken. Refresh availability and try again.") { userId ->
            val booking = repository.createBooking(userId, counsellor, slot)
            _bookings.update { listOf(booking) + it }
            _lastBooking.value = booking
            _slots.update { slots -> slots.filterNot { it.id == slot.id } }
            _selectedSlot.value = null
            onSuccess()
        }
    }

    fun openCancelDialog(bookingId: String) { cancellationId = bookingId; _showCancelDialog.value = true }
    fun closeCancelDialog() { _showCancelDialog.value = false; cancellationId = null }
    fun confirmCancelBooking() {
        val id = cancellationId ?: return
        closeCancelDialog()
        perform("The booking could not be cancelled. Please try again.") { userId ->
            repository.cancelBooking(userId, id)
            _bookings.update { list -> list.map { if (it.id == id) it.copy(status = BookingStatus.CANCELLED) else it } }
            _cancellationConfirmed.value = true
            try { _slots.value = repository.fetchSlots() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _error.value = "Booking cancelled. Availability could not refresh; tap Refresh availability to try again." }
        }
    }

    fun toggleBreathingPlayPause() {
        val state = _breathingState.value
        if (state.activityType != ActivityType.BREATHING_478 || state.isComplete || state.isSaved || state.isSaving) return
        if (state.isPlaying) { pauseBreathing(); return }
        breathingJob?.cancel()
        _breathingState.update { it.copy(isPlaying = true) }
        breathingJob = viewModelScope.launch {
            while (_breathingState.value.isPlaying && _breathingState.value.secondsRemaining > 0) {
                delay(1000)
                _breathingState.update { it.copy(secondsRemaining = it.secondsRemaining - 1,
                    isPlaying = it.secondsRemaining > 1) }
            }
        }
    }

    // Pause on leaving the screen or backgrounding the app. No hidden timer runs.
    fun pauseBreathing() {
        breathingJob?.cancel()
        _breathingState.update { it.copy(isPlaying = false) }
    }

    fun startNewActivity(type: ActivityType = _breathingState.value.activityType) {
        if (_breathingState.value.isSaving) return
        pauseBreathing()
        _breathingState.value = BreathingSessionState(activityType = type)
    }

    fun updateGratitudeNote(note: String) {
        if (!_breathingState.value.isSaved && !_breathingState.value.isSaving)
            _breathingState.update { it.copy(gratitudeNote = note.take(2000)) }
    }

    fun updateStressDecrease(percent: Float) {
        if (!_breathingState.value.isSaved && !_breathingState.value.isSaving)
            _breathingState.update { it.copy(stressDecreasePercent = percent.coerceIn(0f, 100f)) }
    }

    fun saveActivity() {
        val state = _breathingState.value
        if (!state.canSave || _busy.value || !_dataReady.value) return
        perform("Your activity was not confirmed as saved. Please try Save again.") { userId ->
            _breathingState.update { it.copy(isSaving = true, saveError = null) }
            try {
                val activity = ActivityDto(id = state.id, user_id = userId,
                    activity_type = state.activityType.name,
                    duration_seconds = if (state.activityType == ActivityType.BREATHING_478) state.totalSeconds else 0,
                    gratitude_note = state.gratitudeNote.trim().ifBlank { null },
                    stress_decrease_percent = state.stressDecreasePercent?.roundToInt())
                val saved = repository.saveActivity(activity)
                _activities.update { listOf(saved) + it.filterNot { row -> row.id == saved.id } }
                _userProfile.update { it.copy(activitiesCount = _activities.value.size) }
                _breathingState.update { it.copy(isSaved = true) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _breathingState.update { it.copy(saveError = "Not confirmed as saved. Your session is still here; try again.") }
                throw e
            } finally {
                _breathingState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun updateProfileName(name: String, onSaved: () -> Unit) {
        val cleaned = name.trim()
        if (cleaned.isBlank()) { _error.value = "Please enter your name."; return }
        perform("Your name could not be saved. Please try again.") { userId ->
            repository.updateProfileName(userId, cleaned)
            _userProfile.update { it.copy(name = cleaned) }
            onSaved()
        }
    }

    fun updateReminderTime(time: String) {
        perform("Your reminder time could not be saved. Please try again.") { userId ->
            repository.setReminderTime(userId, time)
            _userProfile.update { it.copy(reminderTime = time) }
        }
    }

    fun toggleNotifications() {
        val enabled = !_userProfile.value.notificationsEnabled
        perform("Could not save notification preferences. Please try again.") { userId ->
            repository.setNotifications(userId, enabled)
            _userProfile.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    private fun clearUserData() {
        breathingJob?.cancel()
        _userProfile.value = UserProfile()
        savedCheckIns = emptyList()
        _lastCheckIn.value = null
        _activities.value = emptyList()
        _checkInState.value = CheckInState()
        _counsellors.value = emptyList()
        _bookings.value = emptyList()
        _chatMessages.value = emptyList()
        _slots.value = emptyList()
        _selectedSlot.value = null
        _selectedCounsellorForBooking.value = emptyCounsellor
        _lastBooking.value = null
        _breathingState.value = BreathingSessionState()
        _showCancelDialog.value = false
        cancellationId = null
        _cancellationConfirmed.value = false
        _dataReady.value = false
        _busy.value = false
        _error.value = null
    }
}

internal fun authErrorMessage(error: Exception): String {
    val message = error.message.orEmpty().lowercase()
    return when {
        "invalid_credentials" in message || "invalid login" in message -> "Incorrect email or password. Please try again."
        "email_not_confirmed" in message || "email not confirmed" in message -> "Confirm your email before logging in."
        "already registered" in message || "user_already_exists" in message -> "An account already exists with this email. Please log in."
        "rate" in message || "too many" in message -> "Too many attempts. Please wait a moment and try again."
        "password" in message -> "Your password does not meet the account requirements. Try a longer password."
        else -> "Unable to sign in or create your account. Check your connection and try again."
    }
}
