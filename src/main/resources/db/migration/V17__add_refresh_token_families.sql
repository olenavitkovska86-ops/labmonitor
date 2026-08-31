ALTER TABLE refresh_tokens
    ADD COLUMN family_id VARCHAR(36) NULL AFTER token_hash;

UPDATE refresh_tokens
SET family_id = UUID()
WHERE family_id IS NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN family_id VARCHAR(36) NOT NULL;

CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
