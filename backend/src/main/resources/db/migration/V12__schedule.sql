-- Recurring weekly opening hours. Multiple rows per day allow split shifts
-- (e.g. 09:00-12:00 and 13:00-18:00). end_time is exclusive: intervals are [start, end).
CREATE TABLE weekly_availability (
    id          UUID PRIMARY KEY,
    day_of_week INTEGER NOT NULL,
    start_time  TIME    NOT NULL,
    end_time    TIME    NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT  NOT NULL DEFAULT 0,
    -- ISO-8601: 1 = Monday .. 7 = Sunday (matches java.time.DayOfWeek).
    CONSTRAINT ck_weekly_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_weekly_window CHECK (start_time < end_time)
);

CREATE INDEX ix_weekly_day ON weekly_availability (day_of_week);

-- Per-date exception: either fully closed, or special hours that REPLACE the weekly
-- hours for that date (they do not merge).
CREATE TABLE availability_override (
    id            UUID PRIMARY KEY,
    override_date DATE    NOT NULL UNIQUE,
    closed        BOOLEAN NOT NULL,
    start_time    TIME,
    end_time      TIME,
    reason        VARCHAR(500),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT  NOT NULL DEFAULT 0,
    CONSTRAINT ck_override_shape CHECK (
        (closed = TRUE  AND start_time IS NULL     AND end_time IS NULL)
     OR (closed = FALSE AND start_time IS NOT NULL AND end_time IS NOT NULL
         AND start_time < end_time)
    )
);

-- Owner-blocked time (break, errand, buffer). Absolute instants so blocks are
-- unambiguous across DST transitions. Interval is [start_at, end_at).
CREATE TABLE blocked_time (
    id         UUID PRIMARY KEY,
    start_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    reason     VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_block_window CHECK (start_at < end_at)
);

CREATE INDEX ix_block_range ON blocked_time (start_at, end_at);
