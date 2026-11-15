-- Apply AFTER 20250301000000_calma_init.sql. Run as the database owner.
BEGIN;

-- Keep role lookup outside profile RLS to avoid recursive policy evaluation.
CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM PUBLIC;
GRANT USAGE ON SCHEMA private TO authenticated;
CREATE OR REPLACE FUNCTION private.current_role()
RETURNS text LANGUAGE sql STABLE SECURITY DEFINER SET search_path = '' AS $$
    SELECT role FROM public.profiles WHERE id = (SELECT auth.uid());
$$;
REVOKE ALL ON FUNCTION private.current_role() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION private.current_role() TO authenticated;

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email, role, streak_days, check_ins_count, activities_count)
    VALUES (NEW.id, COALESCE(NULLIF(btrim(NEW.raw_user_meta_data->>'name'), ''), 'Student User'),
            NEW.email, 'user', 0, 0, 0);
    RETURN NEW;
END;
$$;
ALTER TABLE public.profiles ALTER COLUMN streak_days SET DEFAULT 0;
ALTER TABLE public.profiles ALTER COLUMN check_ins_count SET DEFAULT 0;
ALTER TABLE public.profiles ALTER COLUMN activities_count SET DEFAULT 0;
ALTER TABLE public.daily_check_ins ALTER COLUMN wellbeing_score SET DEFAULT NULL;

DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
CREATE POLICY "Users can read own profile" ON public.profiles FOR SELECT TO authenticated
    USING (auth.uid() = id OR private.current_role() IN ('admin', 'counsellor'));
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE TO authenticated
    USING (auth.uid() = id) WITH CHECK (auth.uid() = id);
-- RLS protects rows; column grants prevent editing roles or counters.
REVOKE INSERT, UPDATE ON public.profiles FROM PUBLIC, anon, authenticated;
GRANT UPDATE (name, notifications_enabled, reminder_time) ON public.profiles TO authenticated;

DROP POLICY IF EXISTS "Users access own checkins" ON public.daily_check_ins;
CREATE POLICY "Users access own checkins" ON public.daily_check_ins FOR ALL TO authenticated
    USING (auth.uid() = user_id OR private.current_role() = 'admin')
    WITH CHECK (auth.uid() = user_id OR private.current_role() = 'admin');

DROP POLICY IF EXISTS "Admin write counsellors" ON public.counsellors;
CREATE POLICY "Admin write counsellors" ON public.counsellors FOR ALL TO authenticated
    USING (private.current_role() = 'admin') WITH CHECK (private.current_role() = 'admin');
DROP POLICY IF EXISTS "Counsellors manage slots" ON public.counsellor_slots;
CREATE POLICY "Counsellors manage slots" ON public.counsellor_slots FOR ALL TO authenticated
    USING (private.current_role() = 'admin' OR counsellor_id IN
        (SELECT id FROM public.counsellors WHERE user_id = auth.uid()))
    WITH CHECK (private.current_role() = 'admin' OR counsellor_id IN
        (SELECT id FROM public.counsellors WHERE user_id = auth.uid()));

DROP POLICY IF EXISTS "Users manage own bookings" ON public.bookings;
CREATE POLICY "Read permitted bookings" ON public.bookings FOR SELECT TO authenticated
    USING (auth.uid() = user_id OR private.current_role() = 'admin' OR counsellor_id IN
        (SELECT id FROM public.counsellors WHERE user_id = auth.uid()));
CREATE POLICY "Create own booking" ON public.bookings FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id AND status = 'UPCOMING');
CREATE POLICY "Cancel own booking" ON public.bookings FOR UPDATE TO authenticated
    USING (auth.uid() = user_id AND status = 'UPCOMING')
    WITH CHECK (auth.uid() = user_id AND status = 'CANCELLED');
REVOKE UPDATE, DELETE ON public.bookings FROM PUBLIC, anon, authenticated;
GRANT UPDATE (status) ON public.bookings TO authenticated;

-- Serialize reservations on the slot row, so two clients cannot reserve it together.
-- Existing duplicate upcoming bookings must be resolved before this index can be created.
CREATE UNIQUE INDEX IF NOT EXISTS one_upcoming_booking_per_slot
    ON public.bookings (counsellor_id, date_slot, time_slot) WHERE status = 'UPCOMING';
CREATE OR REPLACE FUNCTION private.reserve_booking_slot()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE chosen_slot uuid;
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.status <> 'UPCOMING' THEN
            RAISE EXCEPTION 'New bookings must be upcoming';
        END IF;
        SELECT id INTO chosen_slot FROM public.counsellor_slots
        WHERE counsellor_id = NEW.counsellor_id AND slot_date::text = NEW.date_slot
            AND slot_time = NEW.time_slot AND NOT is_booked AND slot_date >= CURRENT_DATE
        FOR UPDATE;
        IF chosen_slot IS NULL THEN RAISE EXCEPTION 'This session is no longer available'; END IF;
        UPDATE public.counsellor_slots SET is_booked = true WHERE id = chosen_slot;
        RETURN NEW;
    END IF;
    IF NEW.status = 'CANCELLED' AND OLD.status = 'UPCOMING' THEN
        UPDATE public.counsellor_slots SET is_booked = false
        WHERE counsellor_id = OLD.counsellor_id AND slot_date::text = OLD.date_slot AND slot_time = OLD.time_slot;
    END IF;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION private.reserve_booking_slot() FROM PUBLIC;
CREATE TRIGGER reserve_booking_slot BEFORE INSERT OR UPDATE OF status ON public.bookings
    FOR EACH ROW EXECUTE FUNCTION private.reserve_booking_slot();

-- Reconcile availability for existing bookings that already use ISO dates.
UPDATE public.counsellor_slots AS slots SET is_booked = true
WHERE EXISTS (SELECT 1 FROM public.bookings AS booking
    WHERE booking.counsellor_id = slots.counsellor_id AND booking.date_slot = slots.slot_date::text
        AND booking.time_slot = slots.slot_time AND booking.status = 'UPCOMING');

CREATE INDEX IF NOT EXISTS check_ins_user_created ON public.daily_check_ins(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS bookings_user_created ON public.bookings(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chat_user_created ON public.chat_messages(user_id, created_at);
COMMIT;
