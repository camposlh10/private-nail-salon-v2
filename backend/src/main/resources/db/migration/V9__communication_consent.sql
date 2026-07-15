-- Contact permission per phone/channel/purpose with the disclosure version shown at
-- capture time. Keyed by E.164 phone: clients are only created after a confirmed
-- booking, but consent is recorded during the booking flow itself.
CREATE TABLE communication_consent (
    id                 UUID PRIMARY KEY,
    phone              VARCHAR(20) NOT NULL,
    channel            VARCHAR(10) NOT NULL,
    purpose            VARCHAR(20) NOT NULL,
    granted            BOOLEAN     NOT NULL,
    disclosure_version VARCHAR(40) NOT NULL,
    captured_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at         TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_consent_channel CHECK (channel IN ('SMS', 'EMAIL')),
    CONSTRAINT ck_consent_purpose CHECK (purpose IN ('TRANSACTIONAL', 'MARKETING')),
    CONSTRAINT uq_consent_phone_channel_purpose UNIQUE (phone, channel, purpose)
);
