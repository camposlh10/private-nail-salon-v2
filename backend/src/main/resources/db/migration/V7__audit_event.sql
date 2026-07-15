-- Append-only record of important catalog changes (who did what to which resource).
-- change_summary is a small human/JSON-ish text blob, not executable content.
CREATE TABLE audit_event (
    id             UUID PRIMARY KEY,
    actor          VARCHAR(320),
    action         VARCHAR(80)  NOT NULL,
    resource_type  VARCHAR(80)  NOT NULL,
    resource_id    VARCHAR(80),
    change_summary VARCHAR(4000),
    occurred_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_audit_resource ON audit_event (resource_type, resource_id);
CREATE INDEX ix_audit_occurred_at ON audit_event (occurred_at);
