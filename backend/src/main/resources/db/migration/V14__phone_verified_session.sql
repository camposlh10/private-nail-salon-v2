-- Short-lived proof that a phone number passed OTP verification. The browser holds an
-- opaque token in an HttpOnly cookie; ONLY the SHA-256 hash is stored here, so a DB
-- leak never yields usable sessions. Single-use: booking sets consumed_at.
CREATE TABLE phone_verified_session (
    id           UUID PRIMARY KEY,
    phone_e164   VARCHAR(20) NOT NULL,
    token_hash   VARCHAR(64) NOT NULL UNIQUE,
    slot_hold_id UUID REFERENCES slot_hold (id),
    expires_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at  TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX ix_verified_session_phone ON phone_verified_session (phone_e164, expires_at);
