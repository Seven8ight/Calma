-- Run only against a disposable local test database with the Calma migrations.
-- All test records are rolled back.
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO auth.users(id,email,raw_user_meta_data) VALUES
('aaaaaaaa-0000-0000-0000-000000000091','activity-a@example.test','{"name":"Activity test A"}'),
('bbbbbbbb-0000-0000-0000-000000000092','activity-b@example.test','{"name":"Activity test B"}');
SET LOCAL ROLE authenticated;
SELECT set_config('request.jwt.claim.sub','aaaaaaaa-0000-0000-0000-000000000091',true);
INSERT INTO public.user_activities(id,user_id,activity_type,duration_seconds,gratitude_note,stress_decrease_percent)
VALUES ('cccccccc-0000-0000-0000-000000000093',auth.uid(),'BREATHING_478',120,'My reflection',NULL)
ON CONFLICT(id) DO UPDATE SET gratitude_note=excluded.gratitude_note;
-- Simulate a retry after a response was lost.
INSERT INTO public.user_activities(id,user_id,activity_type,duration_seconds,gratitude_note,stress_decrease_percent)
VALUES ('cccccccc-0000-0000-0000-000000000093',auth.uid(),'BREATHING_478',120,'My reflection',NULL)
ON CONFLICT(id) DO UPDATE SET gratitude_note=excluded.gratitude_note;
INSERT INTO public.user_activities(id,user_id,activity_type,duration_seconds,gratitude_note,stress_decrease_percent)
VALUES ('dddddddd-0000-0000-0000-000000000094',auth.uid(),'GRATITUDE_JOURNAL',0,'My family',0);
DO $$ BEGIN
    ASSERT (SELECT count(*)=2 FROM public.user_activities WHERE user_id=auth.uid()), 'Retry must not create a duplicate';
    ASSERT (SELECT stress_decrease_percent IS NULL FROM public.user_activities WHERE id='cccccccc-0000-0000-0000-000000000093'), 'No answer must remain null';
    ASSERT (SELECT duration_seconds=0 FROM public.user_activities WHERE id='dddddddd-0000-0000-0000-000000000094'), 'Journaling must not invent breathing time';
END $$;
SELECT set_config('request.jwt.claim.sub','bbbbbbbb-0000-0000-0000-000000000092',true);
DO $$ BEGIN
    ASSERT (SELECT count(*)=0 FROM public.user_activities), 'Another account must not see saved notes';
    BEGIN
        INSERT INTO public.user_activities(id,user_id,activity_type,duration_seconds)
        VALUES ('cccccccc-0000-0000-0000-000000000093',auth.uid(),'GRATITUDE_JOURNAL',0)
        ON CONFLICT(id) DO UPDATE SET user_id=excluded.user_id;
        RAISE EXCEPTION 'Another account overwrote a saved activity';
    EXCEPTION WHEN insufficient_privilege THEN NULL;
    END;
END $$;
ROLLBACK;
SELECT 'PASS: activity saving, retry, journal duration, optional stress and account isolation' AS result;
