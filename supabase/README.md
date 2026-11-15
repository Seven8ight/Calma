# Calma Supabase setup

The Android app uses the existing hosted project in `SupabaseClient.kt`. Only the public anon key belongs in the app; never add a service-role key.

Apply `migrations/20250301000000_calma_init.sql` only for a new database. If it has already been applied, do not rerun it. Then apply `migrations/20261115000000_authenticated_data.sql` through the Supabase SQL editor or your usual migration deployment process. The second migration fixes recursive profile policies, prevents self-assigned roles, and makes slot reservations atomic. It deliberately fails if duplicate upcoming bookings already exist for the same counsellor/date/time; review those records before retrying.

Email/password authentication must be enabled. With Confirm email enabled, signup displays an email-confirmation notice and the user logs in after confirming. Set an accessible Site URL in Supabase for the confirmation link. This app does not require an Android deep link for that confirm-then-log-in flow. With Confirm email disabled, signup immediately opens the account.

The app loads availability from `counsellor_slots`. Add actual future dates and times for counsellors in the dashboard; no availability is invented by the app. Dates are stored as `YYYY-MM-DD`. Times retain the text format used by your slot records. Slots on the current date require staff to remove or mark elapsed times unavailable, since the existing schema stores times as free text.

## End-to-end verification

1. Create a test account through the app with a name, email you control, and password. Confirm email if required. Verify `auth.users` and the corresponding `profiles` row share the same ID and that role is `user`.
2. Log in, force-stop/reopen the app, and confirm the same profile is restored. Log out and confirm Back cannot reopen account screens.
3. Save a check-in. Verify `daily_check_ins.user_id` matches the authenticated user, and restart to verify the count and latest check-in load.
4. Add a future counsellor slot. Book it in the app. Verify `bookings.user_id`, the date/time, and `counsellor_slots.is_booked`. The confirmation must only appear after a successful insert. Cancel and verify the booking status and released slot.
5. Send a chat message. Verify both user and automated reply rows in `chat_messages`, then restart to confirm history persists. Replies are local predefined text; this integration does not add an AI service.
6. Disconnect the network and attempt a save. Expect an error, no confirmation, and no fake saved record.
7. Sign into a second account and verify profile, bookings, check-ins, and chat from the first account are absent. Test the same separation through Supabase RLS using each user's token.
8. Attempt simultaneous reservations for one slot from two accounts; exactly one should succeed after the migration.

The app now schedules approximate daily Android reminders, saves breathing/journal activities, and calculates activity counts and check-in streaks from saved rows. No medical wellbeing score is calculated. See `../APP_WALKTHROUGH.md` for the updated app flow and school presentation guide. Counsellor/admin dashboards and Google sign-in remain outside this implementation.

## Verification performed during the initial authentication implementation

The build, 14 account-flow unit tests (plus the existing sample test), and Android lint passed. Both emulator tests passed on Android 17/API 37, including startup, switching to signup, and empty-form validation. The UI test dependencies were updated for compatibility with the emulator. The migrations were applied to a disposable local PostgreSQL 17 database with Supabase-compatible auth roles. Checks covered profile access, role restrictions, cross-account reads/writes, reservation/cancellation, and concurrent booking attempts. These checks do not replace running the migration and testing email confirmation with your real hosted project.

The configured cloud project returned error `42P17: infinite recursion detected in policy for relation "profiles"` before this migration. Its email confirmation setting is enabled. No cloud accounts or records were created during these checks.

Android API 24/25 support uses core library desugaring, as required by the [Supabase Kotlin installation documentation](https://supabase.com/docs/reference/kotlin/installing).

The subsequent app-completion update passed 29 unit tests, 6 emulator tests and activity-persistence SQL checks. See `../APP_WALKTHROUGH.md` for current behavior and verification limits.
