-- Optional extras attached to exactly one service in V1 (e.g. "Gel top coat", "+15 min
-- soak"). May add duration, price, or both. Inactive add-ons are not publicly selectable.
CREATE TABLE service_add_on (
    id                     UUID PRIMARY KEY,
    service_id             UUID         NOT NULL REFERENCES service (id),
    name                   VARCHAR(160) NOT NULL,
    description            VARCHAR(2000),
    added_duration_minutes INTEGER      NOT NULL DEFAULT 0,
    price_cents            INTEGER      NOT NULL DEFAULT 0,
    display_order          INTEGER      NOT NULL DEFAULT 0,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_addon_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_addon_duration CHECK (added_duration_minutes >= 0),
    CONSTRAINT ck_addon_price_cents CHECK (price_cents >= 0)
);

CREATE INDEX ix_addon_service_status ON service_add_on (service_id, status);
