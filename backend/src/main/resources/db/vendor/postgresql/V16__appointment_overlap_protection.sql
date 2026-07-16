-- Database-level last line of defense against double booking: no two appointments in a
-- blocking status may overlap in time. tstzrange defaults to half-open [start, end), so
-- back-to-back appointments are allowed. Cancelled/no-show appointments free the slot.
-- PostgreSQL-only (GiST exclusion constraint); the H2 test profile relies on the
-- application's serialized availability check instead (see db/vendor/h2).
ALTER TABLE appointment
    ADD CONSTRAINT ex_appointment_no_overlap
    EXCLUDE USING gist (tstzrange(start_at, end_at) WITH &&)
    WHERE (status IN ('CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED'));
