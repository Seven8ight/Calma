-- ========================================================
-- CALMA MENTAL HEALTH APP - SUPABASE POSTGRESQL MIGRATION
-- ========================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. PROFILES TABLE (Tied to auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'counsellor', 'admin')),
    streak_days INT DEFAULT 12,
    check_ins_count INT DEFAULT 47,
    activities_count INT DEFAULT 24,
    notifications_enabled BOOLEAN DEFAULT TRUE,
    reminder_time TEXT DEFAULT '8:00 PM',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Automatic trigger to populate public.profiles when auth.users signs up
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'name', 'Student User'),
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'role', 'user')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- 2. DAILY CHECK-INS TABLE
CREATE TABLE IF NOT EXISTS public.daily_check_ins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sleep_hours NUMERIC(3,1) NOT NULL,
    stress_level INT CHECK (stress_level BETWEEN 1 AND 10) NOT NULL,
    time_of_day TEXT CHECK (time_of_day IN ('MORNING', 'AFTERNOON', 'EVENING')) NOT NULL,
    wellbeing_score INT DEFAULT 78,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. COUNSELLORS TABLE
CREATE TABLE IF NOT EXISTS public.counsellors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    title TEXT NOT NULL,
    specialties TEXT NOT NULL,
    avatar_color_hex BIGINT DEFAULT 13754904, -- 0xFFCDE2D8
    is_available_today BOOLEAN DEFAULT TRUE,
    availability_status TEXT DEFAULT 'Available today',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. COUNSELLOR AVAILABLE SLOTS TABLE
CREATE TABLE IF NOT EXISTS public.counsellor_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    counsellor_id UUID NOT NULL REFERENCES public.counsellors(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    slot_time TEXT NOT NULL,
    is_booked BOOLEAN DEFAULT FALSE,
    UNIQUE(counsellor_id, slot_date, slot_time)
);

-- 5. BOOKINGS TABLE
CREATE TABLE IF NOT EXISTS public.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    counsellor_id UUID NOT NULL REFERENCES public.counsellors(id) ON DELETE CASCADE,
    date_slot TEXT NOT NULL,
    time_slot TEXT NOT NULL,
    status TEXT CHECK (status IN ('UPCOMING', 'COMPLETED', 'CANCELLED')) DEFAULT 'UPCOMING',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. CHAT MESSAGES TABLE
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender TEXT CHECK (sender IN ('USER', 'BOT')) NOT NULL,
    message_text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. USER ACTIVITIES TABLE
CREATE TABLE IF NOT EXISTS public.user_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    activity_type TEXT DEFAULT 'BREATHING_478',
    duration_seconds INT DEFAULT 120,
    gratitude_note TEXT,
    stress_decrease_percent INT DEFAULT 60,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================
-- ROW LEVEL SECURITY (RLS) POLICIES FOR MULTI-ROLE SETUP
-- ========================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_check_ins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.counsellors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.counsellor_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_activities ENABLE ROW LEVEL SECURITY;

-- Profiles Policies
CREATE POLICY "Users can read own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id OR (SELECT role FROM public.profiles WHERE id = auth.uid()) IN ('admin', 'counsellor'));

CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- Daily Check-ins Policies
CREATE POLICY "Users access own checkins" ON public.daily_check_ins
    FOR ALL USING (auth.uid() = user_id OR (SELECT role FROM public.profiles WHERE id = auth.uid()) = 'admin');

-- Counsellors & Slots Policies (Public read for all users, write for admin/counsellors)
CREATE POLICY "Public read counsellors" ON public.counsellors FOR SELECT USING (true);
CREATE POLICY "Public read slots" ON public.counsellor_slots FOR SELECT USING (true);

CREATE POLICY "Admin write counsellors" ON public.counsellors
    FOR ALL USING ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'admin');

CREATE POLICY "Counsellors manage slots" ON public.counsellor_slots
    FOR ALL USING ((SELECT role FROM public.profiles WHERE id = auth.uid()) IN ('admin', 'counsellor'));

-- Bookings Policies
CREATE POLICY "Users manage own bookings" ON public.bookings
    FOR ALL USING (
        auth.uid() = user_id
        OR (SELECT role FROM public.profiles WHERE id = auth.uid()) = 'admin'
        OR (counsellor_id IN (SELECT id FROM public.counsellors WHERE user_id = auth.uid()))
    );

-- Chat Messages Policies
CREATE POLICY "Users access own chats" ON public.chat_messages
    FOR ALL USING (auth.uid() = user_id);

-- User Activities Policies
CREATE POLICY "Users access own activities" ON public.user_activities
    FOR ALL USING (auth.uid() = user_id);

-- ========================================================
-- SEED INITIAL DATA
-- ========================================================

INSERT INTO public.counsellors (id, name, title, specialties, avatar_color_hex, is_available_today, availability_status)
VALUES
('11111111-1111-1111-1111-111111111111', 'Dr. Wanjiru Kamau', 'Clinical Psychologist', 'Anxiety · Burnout · Grief', 13492952, true, 'Available today'),
('22222222-2222-2222-2222-222222222222', 'James Otieno', 'Licensed Counsellor', 'Stress · Relationships · Sleep', 14193383, false, 'Next available: Tomorrow'),
('33333333-3333-3333-3333-333333333333', 'Faith Njeri', 'Trauma-Informed Therapist', 'PTSD · Academic Stress', 15785669, true, 'Available today')
ON CONFLICT (id) DO NOTHING;
