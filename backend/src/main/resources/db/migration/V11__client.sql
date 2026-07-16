-- A booking customer. Created (or matched) only after a confirmed booking with a
-- verified phone — never on OTP request — so the phone number is the natural identity:
-- one client per E.164 phone.
CREATE TABLE client (
    id         UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    phone_e164 VARCHAR(20)  NOT NULL UNIQUE,
    email      VARCHAR(320),
    notes      VARCHAR(4000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0
);
