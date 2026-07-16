-- A confirmed booking. start_at/end_at are absolute instants; timezone snapshots the
-- business zone at booking time so local wall-clock rendering stays stable even if the
-- salon's timezone setting later changes. Intervals are half-open [start_at, end_at),
-- so back-to-back appointments (10:00-11:00 then 11:00-12:00) never conflict.
-- idempotency_key dedupes browser retries of POST /public/appointments.
CREATE TABLE appointment (
    id              UUID PRIMARY KEY,
    client_id       UUID        NOT NULL REFERENCES client (id),
    status          VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    start_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    timezone        VARCHAR(64) NOT NULL,
    actual_start_at TIMESTAMP WITH TIME ZONE,
    actual_end_at   TIMESTAMP WITH TIME ZONE,
    source          VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    notes           VARCHAR(2000),
    idempotency_key VARCHAR(80) UNIQUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_appointment_status CHECK (status IN (
        'CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED',
        'CANCELLED_BY_CLIENT', 'CANCELLED_BY_OWNER', 'NO_SHOW')),
    CONSTRAINT ck_appointment_source CHECK (source IN ('PUBLIC', 'OWNER')),
    CONSTRAINT ck_appointment_window CHECK (start_at < end_at)
);

CREATE INDEX ix_appointment_range ON appointment (start_at, end_at);
CREATE INDEX ix_appointment_client ON appointment (client_id);

-- Immutable per-line snapshot (service + each add-on) of name, duration and price at
-- booking time. service_id/add_on_id are traceability pointers, deliberately WITHOUT
-- foreign keys: the snapshot must outlive any future catalog cleanup.
CREATE TABLE appointment_item (
    id               UUID PRIMARY KEY,
    appointment_id   UUID         NOT NULL REFERENCES appointment (id),
    item_type        VARCHAR(10)  NOT NULL,
    service_id       UUID,
    add_on_id        UUID,
    name             VARCHAR(160) NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    price_cents      INTEGER      NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_item_type CHECK (item_type IN ('SERVICE', 'ADD_ON')),
    CONSTRAINT ck_item_duration CHECK (duration_minutes >= 0),
    CONSTRAINT ck_item_price CHECK (price_cents >= 0)
);

CREATE INDEX ix_item_appointment ON appointment_item (appointment_id);

-- Append-only history (created, status changes, reschedules...). Rows are never
-- updated or deleted.
CREATE TABLE appointment_event (
    id             UUID        PRIMARY KEY,
    appointment_id UUID        NOT NULL REFERENCES appointment (id),
    event_type     VARCHAR(60) NOT NULL,
    actor          VARCHAR(10) NOT NULL,
    detail         VARCHAR(2000),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_event_actor CHECK (actor IN ('CLIENT', 'OWNER', 'SYSTEM'))
);

CREATE INDEX ix_event_appointment ON appointment_event (appointment_id, created_at);
