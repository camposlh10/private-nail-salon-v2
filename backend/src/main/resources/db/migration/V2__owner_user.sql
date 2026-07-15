-- Salon owner(s) who sign into the CRM. Deliberately separate from the (future)
-- Client model: owners authenticate with email/password + server-side session,
-- clients are phone-verified booking records. Never merge the two.
CREATE TABLE owner_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_owner_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);
