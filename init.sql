-- ============================================
-- HAI-API 数据库初始化脚本
-- ============================================

-- 创建数据库
-- CREATE DATABASE haiapi;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    status VARCHAR(20) DEFAULT 'active',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('super_admin', 'org_admin', 'user')),
    CONSTRAINT chk_user_status CHECK (status IN ('active', 'disabled'))
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- ============================================
-- 2. 渠道表
-- ============================================
CREATE TABLE IF NOT EXISTS channels (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    key_encrypted TEXT NOT NULL,
    base_url VARCHAR(500),
    models JSONB DEFAULT '[]',
    priority INT DEFAULT 0,
    weight INT DEFAULT 100,
    is_enabled BOOLEAN DEFAULT true,
    status VARCHAR(20) DEFAULT 'active',
    last_test_at TIMESTAMP,
    last_success_at TIMESTAMP,
    avg_response_time INT DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 100.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channels_type ON channels(type);
CREATE INDEX IF NOT EXISTS idx_channels_enabled ON channels(is_enabled);
CREATE INDEX IF NOT EXISTS idx_channels_priority ON channels(priority DESC);

-- ============================================
-- 3. 令牌表
-- ============================================
CREATE TABLE IF NOT EXISTS tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID REFERENCES users(id),
    allowed_models JSONB DEFAULT '["*"]',
    denied_models JSONB DEFAULT '[]',
    allowed_ips JSONB DEFAULT '[]',
    rate_limit JSONB DEFAULT '{"requests_per_minute": 60}',
    quota_limit BIGINT DEFAULT 0,
    quota_used BIGINT DEFAULT 0,
    unlimited BOOLEAN DEFAULT false,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    is_enabled BOOLEAN DEFAULT true,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tokens_key_hash ON tokens(key_hash);
CREATE INDEX IF NOT EXISTS idx_tokens_user ON tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_tokens_expires ON tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_tokens_status ON tokens(status);

-- ============================================
-- 4. 用量日志表
-- ============================================
CREATE TABLE IF NOT EXISTS usage_logs (
    id BIGSERIAL PRIMARY KEY,
    token_id UUID NOT NULL REFERENCES tokens(id),
    channel_id INT REFERENCES channels(id),
    model VARCHAR(100) NOT NULL,
    input_tokens INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    response_time_ms INT DEFAULT 0,
    status_code INT,
    error_type VARCHAR(50),
    ip_address INET,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usage_token ON usage_logs(token_id);
CREATE INDEX IF NOT EXISTS idx_usage_channel ON usage_logs(channel_id);
CREATE INDEX IF NOT EXISTS idx_usage_created ON usage_logs(created_at DESC);

-- ============================================
-- 5. 审计日志表
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    details JSONB,
    ip_address INET,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at DESC);

-- ============================================
-- 插入初始数据
-- ============================================
INSERT INTO users (username, password_hash, email, role, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@haiapi.com', 'super_admin', 'active')
ON CONFLICT (username) DO NOTHING;

INSERT INTO users (username, password_hash, email, role, status)
VALUES ('test', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'test@haiapi.com', 'user', 'active')
ON CONFLICT (username) DO NOTHING;

-- ============================================
-- 创建更新时间戳触发器
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_channels_updated_at ON channels;
CREATE TRIGGER update_channels_updated_at BEFORE UPDATE ON channels FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_tokens_updated_at ON tokens;
CREATE TRIGGER update_tokens_updated_at BEFORE UPDATE ON tokens FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 权限设置
-- ============================================
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO haiapi;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO haiapi;

\echo 'Database initialization completed successfully!'
