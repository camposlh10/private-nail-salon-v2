-- Uploaded images live in object storage; only their metadata + storage key are in
-- PostgreSQL (never the binary). status tracks the upload lifecycle so we can reject
-- orphaned/unreferenced uploads.
CREATE TABLE media_asset (
    id           UUID PRIMARY KEY,
    storage_key  VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size    BIGINT       NOT NULL,
    width        INTEGER,
    height       INTEGER,
    alt_text     VARCHAR(300),
    status       VARCHAR(20)  NOT NULL DEFAULT 'READY',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_media_status CHECK (status IN ('PENDING', 'READY', 'DELETED')),
    CONSTRAINT ck_media_file_size CHECK (file_size > 0)
);
