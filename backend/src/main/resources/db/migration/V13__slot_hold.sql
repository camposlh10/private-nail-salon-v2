-- Temporary reservation while the customer completes checkout. A hold blocks
-- availability while status = ACTIVE and expires_at is in the future (10 minutes);
-- expiry needs no background job — expired holds simply stop matching conflict
-- queries. add_on_ids is a comma-separated UUID snapshot of the selection.
CREATE TABLE slot_hold (
    id         UUID PRIMARY KEY,
    service_id UUID        NOT NULL REFERENCES service (id),
    add_on_ids VARCHAR(1000),
    start_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_hold_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED')),
    CONSTRAINT ck_hold_window CHECK (start_at < end_at)
);

CREATE INDEX ix_hold_conflict ON slot_hold (status, expires_at, start_at);
