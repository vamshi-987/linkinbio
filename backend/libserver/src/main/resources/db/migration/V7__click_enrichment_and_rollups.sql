-- ===== Enrich the raw click stream =====
-- Existing rows have no user agent to derive these from, so they default to 'unknown' rather than
-- NULL: every breakdown query can then GROUP BY without a COALESCE, and the totals still add up.
ALTER TABLE click_events
    ADD COLUMN referrer_host VARCHAR(255) NOT NULL DEFAULT 'direct',
    ADD COLUMN device_type   VARCHAR(16)  NOT NULL DEFAULT 'unknown',
    ADD COLUMN browser       VARCHAR(32)  NOT NULL DEFAULT 'unknown',
    ADD COLUMN os            VARCHAR(32)  NOT NULL DEFAULT 'unknown',
    ADD COLUMN country       VARCHAR(2)   NOT NULL DEFAULT 'XX';

-- ===== Read-side rollups =====
-- click_events is the write-heavy table: one insert per redirect, never updated. Dashboards read
-- from here instead, so a year of traffic is a few hundred rows per user rather than millions.
CREATE TABLE click_daily_rollups (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    link_id     UUID   NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    day         DATE   NOT NULL,
    country     VARCHAR(2)  NOT NULL,
    device_type VARCHAR(16) NOT NULL,
    clicks      BIGINT NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- The rollup job recomputes a day from scratch and upserts on this key, which is what makes a
-- re-run (retry, redeploy, manual backfill) idempotent instead of double-counting.
CREATE UNIQUE INDEX uq_click_daily_rollups_grain
    ON click_daily_rollups (user_id, link_id, day, country, device_type);

CREATE INDEX idx_click_daily_rollups_user_day ON click_daily_rollups (user_id, day);

-- Backfill everything already recorded so the dashboards are not empty on first deploy.
INSERT INTO click_daily_rollups (user_id, link_id, day, country, device_type, clicks)
SELECT l.user_id, c.link_id, DATE(c.clicked_at), c.country, c.device_type, COUNT(*)
FROM click_events c
JOIN links l ON l.id = c.link_id
GROUP BY l.user_id, c.link_id, DATE(c.clicked_at), c.country, c.device_type;
