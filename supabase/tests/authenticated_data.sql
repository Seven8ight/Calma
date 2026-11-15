-- Local disposable PostgreSQL test only. Bootstrap a minimal Supabase auth environment first.
\set ON_ERROR_STOP on
INSERT INTO auth.users (id,email,raw_user_meta_data) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','a@example.test','{"name":"Student A", "role":"admin"}'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','b@example.test','{"name":"Student B"}');
DO $$ BEGIN
    ASSERT (SELECT role = 'user' AND check_ins_count = 0 FROM public.profiles WHERE email = 'a@example.test'), 'Signup must not allow self-assigned admin';
END $$;
INSERT INTO public.counsellor_slots(id, counsellor_id,slot_date,slot_time) VALUES
('cccccccc-cccc-cccc-cccc-cccccccccccc','11111111-1111-1111-1111-111111111111',CURRENT_DATE + 1,'9:00 AM'),
('dddddddd-dddd-dddd-dddd-dddddddddddd','11111111-1111-1111-1111-111111111111',CURRENT_DATE + 1,'11:00 AM');
SET ROLE authenticated;
SELECT set_config('request.jwt.claim.sub','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',false);
DO $$ BEGIN
    ASSERT (SELECT count(*) = 1 FROM public.profiles), 'Profiles must be readable without recursion and limited to the current student';
    BEGIN
        UPDATE public.profiles SET role = 'admin' WHERE id = auth.uid();
        RAISE EXCEPTION 'Role escalation unexpectedly allowed';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
END $$;
UPDATE public.profiles SET notifications_enabled = false WHERE id = auth.uid();
INSERT INTO public.daily_check_ins(user_id,sleep_hours,stress_level,time_of_day)
VALUES (auth.uid(),8,3,'MORNING');
INSERT INTO public.chat_messages(user_id,sender,message_text) VALUES (auth.uid(),'USER','Test message');
INSERT INTO public.bookings(id,user_id,counsellor_id,date_slot,time_slot)
VALUES ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',auth.uid(),'11111111-1111-1111-1111-111111111111',(CURRENT_DATE + 1)::text,'9:00 AM');
DO $$ BEGIN
    ASSERT (SELECT is_booked FROM public.counsellor_slots WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'), 'Booking must reserve slot';
END $$;
SELECT set_config('request.jwt.claim.sub','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',false);
DO $$ BEGIN
    ASSERT (SELECT count(*) = 0 FROM public.bookings), 'Another student must not see bookings';
    ASSERT (SELECT count(*) = 0 FROM public.daily_check_ins), 'Another student must not see checkins';
    ASSERT (SELECT count(*) = 0 FROM public.chat_messages), 'Another student must not see chat';
    BEGIN
        INSERT INTO public.daily_check_ins(user_id,sleep_hours,stress_level,time_of_day)
        VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',8,3,'MORNING');
        RAISE EXCEPTION 'Cross-user insert unexpectedly allowed';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
    BEGIN
        INSERT INTO public.bookings(user_id,counsellor_id,date_slot,time_slot)
        VALUES (auth.uid(),'11111111-1111-1111-1111-111111111111',(CURRENT_DATE+1)::text,'9:00 AM');
        RAISE EXCEPTION 'Duplicate reservation unexpectedly allowed';
    EXCEPTION WHEN raise_exception THEN
        IF SQLERRM <> 'This session is no longer available' THEN RAISE; END IF;
    END;
    UPDATE public.bookings SET status = 'CANCELLED' WHERE id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
    ASSERT NOT FOUND, 'Another student cannot cancel booking';
END $$;
SELECT set_config('request.jwt.claim.sub','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',false);
UPDATE public.bookings SET status = 'CANCELLED' WHERE id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
DO $$ BEGIN
    ASSERT (SELECT NOT is_booked FROM public.counsellor_slots WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'), 'Cancellation must release slot';
    ASSERT (SELECT status = 'CANCELLED' FROM public.bookings WHERE id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'), 'Cancelled booking must remain readable';
END $$;
SELECT set_config('request.jwt.claim.sub','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',false);
INSERT INTO public.bookings(user_id,counsellor_id,date_slot,time_slot)
VALUES (auth.uid(),'11111111-1111-1111-1111-111111111111',(CURRENT_DATE+1)::text,'9:00 AM');
RESET ROLE;
SELECT 'PASS: signup role, nonrecursive RLS, cross-user isolation, profile preference, check-in, chat, booking, cancellation and rebooking' AS result;
