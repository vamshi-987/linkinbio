CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(30) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    bio VARCHAR(280),
    avatar_url VARCHAR(500),
    theme VARCHAR(30) NOT NULL DEFAULT 'default',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE click_events (
    id BIGSERIAL PRIMARY KEY,
    link_id UUID NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    clicked_at TIMESTAMP NOT NULL DEFAULT now(),
    referrer VARCHAR(255)
);

CREATE INDEX idx_links_user_id ON links(user_id);
CREATE INDEX idx_click_events_link_id_clicked_at ON click_events(link_id, clicked_at);