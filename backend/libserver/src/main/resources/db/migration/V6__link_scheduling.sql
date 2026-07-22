-- Scheduled visibility: a link may go live at visible_from and stop being served at visible_until.
-- Both are nullable and independent — NULL means "no bound on that side", so existing rows keep
-- behaving exactly as before (always visible while is_active).
ALTER TABLE links
    ADD COLUMN visible_from  TIMESTAMP,
    ADD COLUMN visible_until TIMESTAMP,
    ADD COLUMN thumbnail_url VARCHAR(500),
    -- The storage object behind thumbnail_url, so a replacement upload can delete the old one.
    ADD COLUMN thumbnail_key VARCHAR(255);

-- The public profile query filters on user + active + both bounds; this index keeps that lookup
-- from degrading into a scan once a user accumulates links.
CREATE INDEX idx_links_visibility ON links (user_id, is_active, visible_from, visible_until);
