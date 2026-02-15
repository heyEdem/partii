-- =============================================
-- V1: Initial schema - all tables with BIGSERIAL PKs
-- =============================================

-- 1. Users
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255),
    display_name    VARCHAR(255) NOT NULL,
    provider        VARCHAR(255) NOT NULL,
    provider_id     VARCHAR(255) NOT NULL,
    legal_name      VARCHAR(255),
    bio             VARCHAR(255),
    general_location VARCHAR(255) NOT NULL,
    primary_address  VARCHAR(255) NOT NULL,
    phone_number    VARCHAR(255) NOT NULL,
    dob             DATE,
    account_status  SMALLINT NOT NULL,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin        BOOLEAN NOT NULL DEFAULT FALSE,
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    total_ratings   INT NOT NULL DEFAULT 0,
    average_rating  INT NOT NULL DEFAULT 0,
    events_attended  INT NOT NULL DEFAULT 0,
    events_organized INT NOT NULL DEFAULT 0,
    active_events_count INT NOT NULL DEFAULT 0,
    profile_picture_url VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

-- 2. Events
CREATE TABLE events (
    id                  BIGSERIAL PRIMARY KEY,
    organizer_id        BIGINT NOT NULL REFERENCES users(id),
    title               VARCHAR(100) NOT NULL,
    description         VARCHAR(2000),
    event_type          VARCHAR(20) NOT NULL,
    location_address    VARCHAR(500),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    event_date          TIMESTAMP NOT NULL,
    image_url           VARCHAR(500),
    estimated_budget    NUMERIC(12, 2),
    currency            VARCHAR(3) DEFAULT 'GHS',
    max_attendees       INT NOT NULL DEFAULT 10,
    current_attendees   INT NOT NULL DEFAULT 0,
    age_restriction     INT,
    payment_deadline    TIMESTAMP,
    join_deadline       TIMESTAMP,
    visibility          VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    status              VARCHAR(15) NOT NULL DEFAULT 'DRAFT',
    private_link_code   VARCHAR(10) UNIQUE,
    link_expiration     TIMESTAMP,
    cancellation_reason VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_visibility ON events(visibility);
CREATE INDEX idx_events_event_date ON events(event_date);
CREATE INDEX idx_events_location ON events(latitude, longitude);
CREATE INDEX idx_events_keyset ON events(event_date, id);

-- 3. Event Attendees
CREATE TABLE event_attendees (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT NOT NULL REFERENCES events(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    status          VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    payment_amount  NUMERIC(12, 2) DEFAULT 0,
    payment_status  VARCHAR(10) NOT NULL DEFAULT 'UNPAID',
    amount_paid     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    joined_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at     TIMESTAMP WITH TIME ZONE,
    notes           VARCHAR(500),
    CONSTRAINT uk_event_attendee_event_user UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_attendees_event ON event_attendees(event_id);
CREATE INDEX idx_event_attendees_user ON event_attendees(user_id);
CREATE INDEX idx_event_attendees_status ON event_attendees(status);
CREATE INDEX idx_event_attendees_payment ON event_attendees(payment_status);

-- 4. Contribution Items
CREATE TABLE contribution_items (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT NOT NULL REFERENCES events(id),
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50),
    type            VARCHAR(10) NOT NULL,
    quantity        INT DEFAULT 1,
    time_commitment INT,
    estimated_cost  NUMERIC(12, 2),
    priority        VARCHAR(15) NOT NULL DEFAULT 'NICE_TO_HAVE',
    notes           VARCHAR(500),
    status          VARCHAR(15) NOT NULL DEFAULT 'AVAILABLE',
    assigned_to     BIGINT REFERENCES users(id),
    completed       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at      TIMESTAMP WITH TIME ZONE,
    confirmed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_contributions_event ON contribution_items(event_id);
CREATE INDEX idx_contributions_assigned_to ON contribution_items(assigned_to);
CREATE INDEX idx_contributions_status ON contribution_items(status);
CREATE INDEX idx_contributions_category ON contribution_items(category);

-- 5. Email Verification Tokens
CREATE TABLE email_verification_tokens (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_evt_email ON email_verification_tokens(email);
CREATE INDEX idx_evt_expires_at ON email_verification_tokens(expires_at);

-- 6. Password Reset Tokens
CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_prt_email ON password_reset_tokens(email);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);

-- 7. Refresh Tokens
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       UUID NOT NULL,
    user_id     BIGINT NOT NULL,
    issued_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    family_id   UUID NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE
);

-- 8. User Blocks
CREATE TABLE user_blocks (
    id          BIGSERIAL PRIMARY KEY,
    blocker_id  BIGINT NOT NULL REFERENCES users(id),
    blocked_id  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_user_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_user_blocks_blocked ON user_blocks(blocked_id);
CREATE INDEX idx_user_blocks_created ON user_blocks(created_at);

-- 9. User Reports
CREATE TABLE user_reports (
    id          BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    reported_id BIGINT NOT NULL REFERENCES users(id),
    reason      VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    status      VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    admin_notes VARCHAR(500),
    reviewed_by BIGINT REFERENCES users(id),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_user_reports_reporter ON user_reports(reporter_id);
CREATE INDEX idx_user_reports_reported ON user_reports(reported_id);
CREATE INDEX idx_user_reports_status ON user_reports(status);
CREATE INDEX idx_user_reports_created ON user_reports(created_at);

-- 10. Default Admin User
INSERT INTO users (email, password, display_name, provider, provider_id,
                   general_location, primary_address, phone_number,
                   account_status, is_verified, is_enabled, is_admin,
                   profile_completed, created_at, updated_at)
VALUES ('admin@partii.com', '', 'Admin', 'local', 'admin',
        'System', 'System', '0000000000',
        0, TRUE, TRUE, TRUE,
        TRUE, NOW(), NOW());
