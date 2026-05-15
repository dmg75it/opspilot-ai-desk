CREATE TYPE message_role AS ENUM ('SYSTEM','USER','ASSISTANT');

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role message_role NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(100),
    prompt_tokens INT,
    completion_tokens INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT
);
