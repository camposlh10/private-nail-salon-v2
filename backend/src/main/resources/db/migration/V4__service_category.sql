-- Grouping of services (e.g. Manicure, Pedicure). Archived categories (and their
-- services) are hidden from the public catalog but retained. Case-insensitive name
-- uniqueness is enforced via name_normalized (lowercased, app-maintained) so it works
-- identically on PostgreSQL and the H2 test database without functional indexes.
CREATE TABLE service_category (
    id              UUID PRIMARY KEY,
    business_id     UUID         NOT NULL REFERENCES business_profile (id),
    name            VARCHAR(120) NOT NULL,
    name_normalized VARCHAR(120) NOT NULL,
    slug            VARCHAR(140) NOT NULL UNIQUE,
    description     VARCHAR(4000),
    display_order   INTEGER      NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_category_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_category_name_per_business UNIQUE (business_id, name_normalized)
);

CREATE INDEX ix_category_status_order ON service_category (status, display_order);
