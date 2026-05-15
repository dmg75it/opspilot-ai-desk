-- Convert PostgreSQL custom enum columns to VARCHAR for JPA compatibility

ALTER TABLE tickets
    ALTER COLUMN status TYPE VARCHAR(50) USING status::TEXT,
    ALTER COLUMN priority TYPE VARCHAR(50) USING priority::TEXT,
    ALTER COLUMN category TYPE VARCHAR(50) USING category::TEXT;

ALTER TABLE ticket_notes
    ALTER COLUMN visibility TYPE VARCHAR(50) USING visibility::TEXT;

ALTER TABLE chat_messages
    ALTER COLUMN role TYPE VARCHAR(50) USING role::TEXT;
