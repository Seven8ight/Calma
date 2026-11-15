# Calma: app walkthrough for the school presentation

## The simple structure

There are three main parts:

1. **Screens** draw the interface and send button clicks to the ViewModel.
2. **CalmaViewModel** holds the current screen data and decides what an action does.
3. **CalmaRepository** reads and writes Supabase records.

The flow is: **button → ViewModel → repository → Supabase → updated screen**.

`StateFlow` is a value that the screen watches. When that value changes, Jetpack Compose redraws the relevant UI. `viewModelScope.launch` runs a timer or network request without freezing the interface. The repository interface also lets tests use a small fake database instead of the real student account.

## Breathing and journaling

`PersonalisedActivityScreen.kt` now shows two activity choices. Despite the earlier “pomodoro” description, the existing feature is a two-minute breathing exercise, not a study/work Pomodoro timer.

For breathing:

1. Start the 120-second timer.
2. The prompts follow a repeating 4-second inhale, 7-second hold and 8-second exhale cycle. The final cycle stops when the two-minute session ends.
3. Pause/resume is available. Leaving the screen or putting the app in the background pauses it.
4. At zero, the timer stops. The screen says the session is complete but still needs saving.
5. Add an optional reflection and optional self-reported stress decrease.
6. Tap **Save session**. The screen only says **Activity saved** after Supabase returns the saved record.
7. Tap **Done**, or start another session.

For journaling, a nonblank gratitude note is required. It saves with type `GRATITUDE_JOURNAL` and duration zero, so it is not counted as a completed breathing timer.

Each draft gets one UUID (a unique ID). Retrying a save uses the same ID. The repository uses `upsert`: insert a new record or update the record with that ID. This prevents duplicate activities when a network response is lost. Buttons are also disabled during a save.

An unanswered stress question is stored as `null`, not a made-up percentage. No medical wellbeing score is calculated.

Unfinished timers and notes are held in memory, not stored offline. The app explains this before leaving an unfinished activity. Saved activities remain in Supabase and reload after logging in again.

## Where activity records are stored

The existing `user_activities` table already has all the needed fields:

- `id`: stable ID for that session/entry
- `user_id`: the signed-in student's ID
- `activity_type`: `BREATHING_478` or `GRATITUDE_JOURNAL`
- `duration_seconds`: 120 for completed breathing; 0 for journaling
- `gratitude_note`: optional reflection or journal text
- `stress_decrease_percent`: optional self-reported value
- `created_at`: database timestamp

**No additional SQL migration is needed for these app changes**, assuming the initial schema and authentication migration have already been applied.

## Other completed controls

- **Profile → Activity History:** shows the latest 20 saved activities, notes and timestamps.
- **Profile → Personal Details:** edits and saves the name. Email is displayed without pretending to update the authentication address.
- **Profile → Privacy & Data:** explains storage and what is saved.
- **Daily reminder:** requests Android notification permission, saves the chosen time and schedules an approximate daily reminder. Android battery restrictions may delay delivery. Logout disables the reminder on this device.
- **Bookings → Change:** selects a different counsellor. Elapsed or unreadable slot times are hidden, and the selected time is checked again before booking.
- **Chat:** shows an empty-state explanation, scrolls to the latest message and keeps unsent input on failure.
- **Home:** shows the latest real sleep/stress check-in, actual saved activity count and a calculated check-in streak. Multiple entries on one day count as one streak day.
- **Appearance:** uses the app's light design consistently, including when the device is set to dark mode.

The displayed counters are calculated from saved activity/check-in records, rather than maintaining a second counter in the profile table.

The summary counts currently use the records returned by Supabase's normal list queries (normally up to 1,000 per query). That is adequate for the school demo; server-side aggregate counts and pagination are the next step for a large production dataset.

## Reminders in simple terms

`ReminderScheduler.kt` uses Android's built-in `AlarmManager`. A `PendingIntent` tells Android which receiver to call. The receiver builds a small notification that opens Calma when tapped. A boot receiver restores the schedule after a phone restart or clock change. No extra scheduling library was added.

The reminder is approximate, not an alarm-clock promise. Notifications require permission and can also be disabled in Android's notification settings.

## Suggested demonstration

1. Log in and save a daily check-in. Show the updated count and last saved values.
2. Open breathing, start it, pause, resume, and finish the two minutes.
3. Point out that completion and saving are separate steps.
4. Add a reflection, save, and show the saved confirmation.
5. Open Profile → Activity History and show the same record.
6. Write and save a separate gratitude entry.
7. Change the profile name and reminder time.
8. Show selecting a counsellor and an available booking slot.
9. Log out and back in to demonstrate saved activity history returning.

For an offline demonstration, disconnect before saving an activity. The app must show an error and keep the draft. Reconnect and retry; the same activity ID is reused.

## Checks completed

The Android debug build and lint pass. All 29 local unit tests and 6 emulator tests pass. These cover timer completion, pause/resume, required journal text, saving once, retrying a lost response, account changes, saved confirmation, reset confirmation and the existing authentication flows. The activity save/update and account-isolation rules were also checked against a temporary PostgreSQL database using the existing migrations.

The emulator tests use a fake repository, so they do not create cloud accounts or write to your real account. Do the demonstration above with your own account to confirm the hosted project behaves the same way. Notification permission and an actual delayed reminder should also be checked on your phone.

## Remaining platform setup

Email confirmation, invitation and password-recovery redirect pages belong to the planned website. Supabase's Site URL must point to a real deployed page rather than localhost. This app work does not change your cloud authentication settings or create test accounts.

The support chat still uses predefined replies; it is not an AI model or a live counsellor. Counsellor availability must be entered in the database until the staff website is built. The old unverified campus helpline number has been removed from the mockup.
