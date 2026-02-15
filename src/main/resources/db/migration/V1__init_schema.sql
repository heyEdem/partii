-- V1: Initialize database schema matching Hibernate entities
-- Creates all tables for the Partii event collaboration platform

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL,
    profile_picture_url VARCHAR(255),
    bio VARCHAR(255),
    legal_name VARCHAR(255),
    dob DATE,
    general_location VARCHAR(255) NOT NULL,
    primary_address VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    is_verified BOOLEAN NOT NULL,
    is_enabled BOOLEAN NOT NULL,
    is_admin BOOLEAN NOT NULL,
    profile_completed BOOLEAN NOT NULL,
    account_status SMALLINT NOT NULL,
    events_organized INTEGER NOT NULL,
    events_attended INTEGER NOT NULL,
    active_events_count INTEGER NOT NULL,
    total_ratings INTEGER NOT NULL,
    average_rating INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT users_account_status_check CHECK ((account_status >= 0) AND (account_status <= 3))
);

-- Events table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    event_type VARCHAR(20) NOT NULL,
    location_address VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    event_date TIMESTAMP NOT NULL,
    image_url VARCHAR(500),
    estimated_budget NUMERIC(12, 2),
    currency VARCHAR(3),
    max_attendees INTEGER NOT NULL,
    current_attendees INTEGER NOT NULL,
    age_restriction INTEGER,
    payment_deadline TIMESTAMP,
    join_deadline TIMESTAMP,
    visibility VARCHAR(10) NOT NULL,
    status VARCHAR(15) NOT NULL,
    private_link_code VARCHAR(10) UNIQUE,
    link_expiration TIMESTAMP,
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT events_visibility_check CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT events_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'FULL', 'PAST', 'CANCELLED', 'ARCHIVED')),
    CONSTRAINT events_event_type_check CHECK (event_type IN ('PARTY', 'DINNER', 'TRIP', 'SPORTS', 'GAME_NIGHT', 'CONCERT', 'FESTIVAL', 'BIRTHDAY', 'WEDDING', 'GRADUATION', 'NETWORKING', 'WORKSHOP', 'OTHER')),
    CONSTRAINT events_max_attendees_check CHECK ((max_attendees <= 10000) AND (max_attendees >= 2)),
    CONSTRAINT events_current_attendees_check CHECK (current_attendees >= 0),
    CONSTRAINT events_age_restriction_check CHECK (age_restriction >= 0)
);

CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_status ON events(status);
CREATE INDEX idx_events_visibility ON events(visibility);
CREATE INDEX idx_events_event_date ON events(event_date);
CREATE INDEX idx_events_location ON events(latitude, longitude);

-- Event Attendees table
CREATE TABLE event_attendees (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(15) NOT NULL,
    payment_amount NUMERIC(12, 2),
    payment_status VARCHAR(10) NOT NULL,
    amount_paid NUMERIC(12, 2) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at TIMESTAMP WITH TIME ZONE,
    notes VARCHAR(500),
    CONSTRAINT uk_event_attendee_event_user UNIQUE (event_id, user_id),
    CONSTRAINT event_attendees_status_check CHECK (status IN ('PENDING', 'APPROVED', 'WAITLIST', 'DECLINED', 'REMOVED')),
    CONSTRAINT event_attendees_payment_status_check CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID'))
);

CREATE INDEX idx_event_attendees_event ON event_attendees(event_id);
CREATE INDEX idx_event_attendees_user ON event_attendees(user_id);
CREATE INDEX idx_event_attendees_status ON event_attendees(status);
CREATE INDEX idx_event_attendees_payment ON event_attendees(payment_status);

-- Contribution Items table
CREATE TABLE contribution_items (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    type VARCHAR(10) NOT NULL,
    quantity INTEGER,
    time_commitment INTEGER,
    estimated_cost NUMERIC(12, 2),
    priority VARCHAR(15) NOT NULL,
    notes VARCHAR(500),
    status VARCHAR(15) NOT NULL,
    assigned_to BIGINT REFERENCES users(id),
    completed BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT contribution_items_type_check CHECK (type IN ('MATERIAL', 'SERVICE')),
    CONSTRAINT contribution_items_priority_check CHECK (priority IN ('MUST_HAVE', 'NICE_TO_HAVE')),
    CONSTRAINT contribution_items_status_check CHECK (status IN ('AVAILABLE', 'CLAIMED', 'CONFIRMED')),
    CONSTRAINT contribution_items_quantity_check CHECK (quantity >= 1),
    CONSTRAINT contribution_items_time_commitment_check CHECK (time_commitment >= 0)
);

CREATE INDEX idx_contributions_event ON contribution_items(event_id);
CREATE INDEX idx_contributions_status ON contribution_items(status);
CREATE INDEX idx_contributions_assigned_to ON contribution_items(assigned_to);
CREATE INDEX idx_contributions_category ON contribution_items(category);

-- Refresh Tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token UUID NOT NULL,
    user_id BIGINT NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    family_id UUID NOT NULL,
    revoked BOOLEAN NOT NULL
);

-- Email Verification Tokens table
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_evt_email ON email_verification_tokens(email);
CREATE INDEX idx_evt_expires_at ON email_verification_tokens(expires_at);

-- Password Reset Tokens table
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_prt_email ON password_reset_tokens(email);
CREATE INDEX idx_prt_expires_at ON password_reset_tokens(expires_at);

-- User Blocks table
CREATE TABLE user_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL REFERENCES users(id),
    blocked_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_user_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX idx_user_blocks_blocked ON user_blocks(blocked_id);
CREATE INDEX idx_user_blocks_created ON user_blocks(created_at);

-- User Reports table
CREATE TABLE user_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    reported_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(15) NOT NULL,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    admin_notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_reports_status_check CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED', 'ON_HOLD'))
);

CREATE INDEX idx_user_reports_reporter ON user_reports(reporter_id);
CREATE INDEX idx_user_reports_reported ON user_reports(reported_id);
CREATE INDEX idx_user_reports_status ON user_reports(status);
CREATE INDEX idx_user_reports_created ON user_reports(created_at);
