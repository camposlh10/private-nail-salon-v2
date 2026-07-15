-- A bookable salon service. Prices are integer cents (never floating point).
-- price_type FREE => price_cents must be 0; FIXED/STARTING_AT => price_cents > 0.
-- Only status=ACTIVE AND online_bookable=TRUE services appear in the public catalog.
CREATE TABLE service (
    id                       UUID PRIMARY KEY,
    category_id              UUID         NOT NULL REFERENCES service_category (id),
    name                     VARCHAR(160) NOT NULL,
    slug                     VARCHAR(180) NOT NULL UNIQUE,
    description              VARCHAR(4000),
    duration_minutes         INTEGER      NOT NULL,
    price_type               VARCHAR(20)  NOT NULL,
    price_cents              INTEGER      NOT NULL DEFAULT 0,
    online_bookable          BOOLEAN      NOT NULL DEFAULT TRUE,
    hidden_from_new_clients  BOOLEAN      NOT NULL DEFAULT FALSE,
    image_id                 UUID         REFERENCES media_asset (id),
    display_order            INTEGER      NOT NULL DEFAULT 0,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_service_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_service_price_type CHECK (price_type IN ('FIXED', 'STARTING_AT', 'FREE')),
    CONSTRAINT ck_service_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_service_price_cents CHECK (price_cents >= 0),
    CONSTRAINT ck_service_free_is_zero CHECK (price_type <> 'FREE' OR price_cents = 0),
    CONSTRAINT ck_service_paid_is_positive CHECK (price_type = 'FREE' OR price_cents > 0)
);

CREATE INDEX ix_service_category_status_order ON service (category_id, status, display_order);
