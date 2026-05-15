-- Seed users. Passwords are BCrypt-hashed (cost 10).
-- admin123  -> generated at app startup by DataInitializer if not present
-- operator123 -> generated at app startup by DataInitializer if not present
-- This migration creates placeholders; DataInitializer overwrites with real hashes on first run.
-- We use a sentinel hash here so the row exists but cannot be logged in with:
INSERT INTO users (id, email, password_hash, full_name, role) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'admin@example.com',
     '$2a$10$PLACEHOLDER_WILL_BE_REPLACED_BY_INITIALIZER_00000000000',
     'Admin User',
     'ADMIN'),
    ('00000000-0000-0000-0000-000000000002',
     'operator@example.com',
     '$2a$10$PLACEHOLDER_WILL_BE_REPLACED_BY_INITIALIZER_00000000000',
     'Operator User',
     'OPERATOR')
ON CONFLICT (email) DO NOTHING;
