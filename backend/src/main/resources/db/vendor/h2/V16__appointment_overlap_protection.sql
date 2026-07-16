-- H2 counterpart of db/vendor/postgresql/V16: H2 has no GiST exclusion constraints,
-- so overlap protection under H2 (test profile) comes solely from the application's
-- serialized availability check (pessimistic lock on the business_profile row).
-- Intentionally a no-op so both vendors share the same Flyway version history.
SELECT 1;
