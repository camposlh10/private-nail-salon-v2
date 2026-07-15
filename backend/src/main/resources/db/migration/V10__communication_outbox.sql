-- Transactional outbox for reliable appointment messages: business transactions insert
-- rows here atomically; a background worker (future booking milestone) claims PENDING
-- rows and sends them through the SMS/email gateway.
CREATE TABLE communication_outbox (
    id                  UUID PRIMARY KEY,
    message_type        VARCHAR(60)   NOT NULL,
    channel             VARCHAR(10)   NOT NULL DEFAULT 'SMS',
    recipient           VARCHAR(320)  NOT NULL,
    payload             VARCHAR(4000) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    attempts            INTEGER       NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMP WITH TIME ZONE,
    provider_message_id VARCHAR(80),
    last_error          VARCHAR(1000),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_channel CHECK (channel IN ('SMS', 'EMAIL')),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX ix_outbox_claim ON communication_outbox (status, next_attempt_at);
