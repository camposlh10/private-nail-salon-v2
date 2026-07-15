-- One-time owner password-reset tokens. Only the SHA-256 hash of the token is stored;
-- the raw token leaves the system exactly once (in the reset email/log).
CREATE TABLE password_reset_token (
    id            UUID PRIMARY KEY,
    owner_user_id UUID        NOT NULL REFERENCES owner_user (id),
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at       TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_reset_token_owner ON password_reset_token (owner_user_id);
