-- V6: Create user blocking and reporting tables
-- Moderation and safety features for managing user interactions

-- User blocks table
CREATE TABLE IF NOT EXISTS user_blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_blocks_blocker_blocked UNIQUE (blocker_id, blocked_id),
    CONSTRAINT chk_user_blocks_different CHECK (blocker_id != blocked_id)
);

CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked ON user_blocks(blocked_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_created ON user_blocks(created_at);

-- User reports table
CREATE TABLE IF NOT EXISTS user_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    admin_notes VARCHAR(500),
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT chk_user_reports_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED', 'ON_HOLD')),
    CONSTRAINT chk_user_reports_different CHECK (reporter_id != reported_id)
);

CREATE INDEX IF NOT EXISTS idx_user_reports_reporter ON user_reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_user_reports_reported ON user_reports(reported_id);
CREATE INDEX IF NOT EXISTS idx_user_reports_status ON user_reports(status);
CREATE INDEX IF NOT EXISTS idx_user_reports_created ON user_reports(created_at);

-- Composite index for finding pending reports
CREATE INDEX IF NOT EXISTS idx_user_reports_pending ON user_reports(created_at)
    WHERE status IN ('PENDING', 'UNDER_REVIEW');

-- Comments
COMMENT ON TABLE user_blocks IS 'Tracks blocks between users for preventing interactions';
COMMENT ON TABLE user_reports IS 'User reports submitted for moderation review';
COMMENT ON COLUMN user_reports.status IS 'PENDING, UNDER_REVIEW, RESOLVED, DISMISSED, ON_HOLD';
