-- SCHOOL DEMO ONLY: example availability for the three seeded counsellors.
-- Run in the Supabase SQL Editor after the existing migrations.
-- These are demonstration times, not confirmed availability of real professionals.
-- Safe to rerun: existing slots, including booked ones, are left unchanged.
BEGIN;
INSERT INTO public.counsellor_slots(counsellor_id, slot_date, slot_time, is_booked)
SELECT counsellor.id,
       (CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Nairobi')::date + day_offset,
       times.slot_time,
       false
FROM public.counsellors AS counsellor
CROSS JOIN generate_series(1, 21) AS days(day_offset)
CROSS JOIN (VALUES ('9:00 AM'), ('11:00 AM'), ('2:00 PM'), ('4:00 PM')) AS times(slot_time)
WHERE counsellor.id IN (
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333'
)
AND EXTRACT(ISODOW FROM ((CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Nairobi')::date + day_offset)) BETWEEN 1 AND 5
-- Do not reopen an existing reservation if its slot row was accidentally removed.
AND NOT EXISTS (
    SELECT 1 FROM public.bookings AS booking
    WHERE booking.counsellor_id = counsellor.id
      AND booking.date_slot = ((CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Nairobi')::date + day_offset)::text
      AND booking.time_slot = times.slot_time
      AND booking.status = 'UPCOMING'
)
ON CONFLICT (counsellor_id, slot_date, slot_time) DO NOTHING;
COMMIT;

SELECT counsellor.name, COUNT(slot.id) AS future_open_slots
FROM public.counsellors AS counsellor
LEFT JOIN public.counsellor_slots AS slot ON slot.counsellor_id = counsellor.id
    AND NOT slot.is_booked
    AND slot.slot_date > (CURRENT_TIMESTAMP AT TIME ZONE 'Africa/Nairobi')::date
GROUP BY counsellor.id, counsellor.name
ORDER BY counsellor.name;
