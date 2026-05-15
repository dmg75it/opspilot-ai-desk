CREATE TYPE ticket_status AS ENUM ('NEW','IN_PROGRESS','WAITING_FOR_CUSTOMER','RESOLVED','CLOSED');
CREATE TYPE ticket_priority AS ENUM ('LOW','MEDIUM','HIGH','CRITICAL');
CREATE TYPE ticket_category AS ENUM ('DELIVERY','PICKUP','DOCUMENTATION','CUSTOMER','SYSTEM','OTHER');

CREATE TABLE tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_ref VARCHAR(100),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    status ticket_status NOT NULL DEFAULT 'NEW',
    priority ticket_priority NOT NULL,
    category ticket_category NOT NULL,
    assigned_to_id UUID REFERENCES users(id),
    created_by_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_tickets_external_ref
    ON tickets (external_ref)
    WHERE external_ref IS NOT NULL;
