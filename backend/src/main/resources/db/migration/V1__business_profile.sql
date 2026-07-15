-- The salon is a single business. This table holds at most one row; `single_row`
-- (always TRUE, UNIQUE) enforces the singleton so public endpoints can resolve the
-- business internally without ever accepting a businessId.
CREATE TABLE business_profile (
    id                               UUID PRIMARY KEY,
    single_row                       BOOLEAN NOT NULL DEFAULT TRUE UNIQUE,
    name                             VARCHAR(200) NOT NULL,
    slug                             VARCHAR(120) NOT NULL UNIQUE,
    phone                            VARCHAR(40),
    email                            VARCHAR(320),
    timezone                         VARCHAR(64)  NOT NULL,
    currency                         VARCHAR(3)   NOT NULL DEFAULT 'USD',
    address                          VARCHAR(500),
    -- Informational "we may start 5-10 min late" notice, shown on public pages.
    -- Does NOT alter service duration or availability.
    appointment_start_window_minutes INTEGER      NOT NULL DEFAULT 10,
    appointment_start_notice         VARCHAR(1000),
    created_at                       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                       TIMESTAMP WITH TIME ZONE NOT NULL,
    version                          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_business_single_row CHECK (single_row = TRUE),
    CONSTRAINT ck_business_start_window CHECK (appointment_start_window_minutes >= 0)
);
