-- Adds the `browser_bookmarks` table so sites a user stars in the in-app browser
-- sync across devices and show on every device's browser landing screen.
--
-- Mirrors the `grocery_autocomplete_items` sync scaffolding: `client_id` is
-- UNIQUE so the client-side upsert can rely on `onConflict = "client_id"` to
-- dedup creates that get retried after a transient network failure.
--
-- Per-user URL uniqueness is intentionally NOT enforced here (unlike the
-- client's local unique index). The remote upsert keys on `client_id`, so a
-- server-side URL conflict would fail the best-effort push and strand the row.
-- Cross-device duplicates are instead collapsed locally on pull, where the
-- repository adopts a matching URL onto the incoming remote row.

CREATE TABLE IF NOT EXISTS browser_bookmarks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    client_id TEXT UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_browser_bookmarks_owner
    ON browser_bookmarks(owner_id);

ALTER TABLE browser_bookmarks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own browser bookmarks" ON browser_bookmarks
    FOR SELECT USING (auth.uid() = owner_id);

CREATE POLICY "Users can insert own browser bookmarks" ON browser_bookmarks
    FOR INSERT WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "Users can update own browser bookmarks" ON browser_bookmarks
    FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "Users can delete own browser bookmarks" ON browser_bookmarks
    FOR DELETE USING (auth.uid() = owner_id);
