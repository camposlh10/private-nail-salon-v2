# Flyway migrations

Versioned schema for PostgreSQL (`dev`/`prod`) and the H2 test database. SQL is kept
portable across both:

- Application-assigned `UUID` PKs (no DB uuid-generation function).
- Money as integer **cents**; durations as integer **minutes**; timestamps `TIMESTAMP WITH TIME ZONE`.
- Status enums as `VARCHAR` + `CHECK (... IN (...))` rather than native PostgreSQL enum types.
- Case-insensitive uniqueness via an app-maintained `*_normalized` column + plain `UNIQUE`
  (avoids functional indexes H2 can't run).

Dev/demo seed data is **not** a migration (migrations run in prod too) — it lives in a
`@Profile("dev")` seeder component so prod stays untouched.

| Version | Table |
|---|---|
| V1 | business_profile (singleton) |
| V2 | owner_user |
| V3 | media_asset |
| V4 | service_category |
| V5 | service |
| V6 | service_add_on |
| V7 | audit_event |
| V8 | password_reset_token |
| V9 | communication_consent |
| V10 | communication_outbox |
