-- ====================================================================
-- PRIME DORAEMON BOT & ADMIN SYSTEM - COMPLETE SUPABASE SQL SCHEMA
-- ====================================================================

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ====================================================================
-- 2. TABLE DEFINITIONS
-- ====================================================================

-- CHANNELS TABLE: Stores mandatory Telegram channels users must join
CREATE TABLE IF NOT EXISTS public.channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    channel_id TEXT NOT NULL, -- Format: -1001234567890 or @username
    invite_link TEXT NOT NULL, -- Permanent or custom invite link
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- CONTENT ITEMS TABLE: Stores all content items and their unique access keys
CREATE TABLE IF NOT EXISTS public.content_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key TEXT NOT NULL UNIQUE, -- E.g. "PD-7KX92M4Q"
    content_type TEXT NOT NULL CHECK (content_type IN ('text', 'photo', 'video', 'document', 'audio', 'voice', 'animation')),
    text_content TEXT, -- For text-only messages
    telegram_file_id TEXT, -- Telegram file_id for Telegram-hosted media
    storage_path TEXT, -- Supabase Storage URL or path for Admin App uploaded media
    caption TEXT, -- Optional caption/text for media
    active BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMPTZ, -- Optional expiration timestamp (NULL = never expires)
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- BOT SETTINGS TABLE: Dynamic bot branding and messages configurable from Admin
CREATE TABLE IF NOT EXISTS public.bot_settings (
    id TEXT PRIMARY KEY DEFAULT 'default',
    bot_name TEXT NOT NULL DEFAULT 'PRIME DORAEMON BOT',
    welcome_message TEXT NOT NULL DEFAULT 'Please join all required channels to continue.',
    key_prompt TEXT NOT NULL DEFAULT 'Paste Your Key',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- DELIVERY LOGS TABLE: Tracks delivered messages to facilitate 15-minute auto-deletion
CREATE TABLE IF NOT EXISTS public.delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telegram_user_id BIGINT NOT NULL,
    content_id UUID REFERENCES public.content_items(id) ON DELETE SET NULL,
    key TEXT,
    delivered_message_ids JSONB NOT NULL DEFAULT '[]'::jsonb, -- Array of Telegram message IDs sent
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    delete_after TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT false
);

-- ====================================================================
-- 3. INDEXES FOR PERFORMANCE
-- ====================================================================
CREATE INDEX IF NOT EXISTS idx_channels_active ON public.channels(active);
CREATE INDEX IF NOT EXISTS idx_content_items_key ON public.content_items(key);
CREATE INDEX IF NOT EXISTS idx_content_items_active ON public.content_items(active);
CREATE INDEX IF NOT EXISTS idx_content_items_expires ON public.content_items(expires_at);
CREATE INDEX IF NOT EXISTS idx_delivery_logs_delete_after ON public.delivery_logs(delete_after);
CREATE INDEX IF NOT EXISTS idx_delivery_logs_user ON public.delivery_logs(telegram_user_id);
CREATE INDEX IF NOT EXISTS idx_delivery_logs_pending ON public.delivery_logs(delete_after) WHERE deleted = false;

-- ====================================================================
-- 4. INITIAL DEFAULT SETTINGS SEED
-- ====================================================================
INSERT INTO public.bot_settings (id, bot_name, welcome_message, key_prompt)
VALUES (
    'default',
    'PRIME DORAEMON BOT',
    'Please join all required channels to continue.',
    'Paste Your Key'
)
ON CONFLICT (id) DO NOTHING;

-- ====================================================================
-- 5. STORAGE BUCKET CONFIGURATION (for Admin App media uploads)
-- ====================================================================
-- Insert bucket if not already present
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'content-files',
    'content-files',
    true,
    52428800, -- 50 MB limit per file
    ARRAY[
        'image/jpeg', 'image/png', 'image/gif', 'image/webp',
        'video/mp4', 'video/mkv', 'video/quicktime',
        'audio/mpeg', 'audio/ogg', 'audio/wav',
        'application/pdf', 'application/zip', 'application/octet-stream'
    ]
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 52428800;

-- ====================================================================
-- 6. ROW LEVEL SECURITY (RLS) POLICIES
-- ====================================================================

-- Enable RLS on all tables
ALTER TABLE public.channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.content_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bot_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.delivery_logs ENABLE ROW LEVEL SECURITY;

-- Note: The Telegram Bot connects using the Supabase Service Role Key on Render,
-- which automatically bypasses RLS for high-performance server-side operations.

-- For the Android Admin App (which connects using Anon Key + Supabase Auth):
-- A. Channels Policies
DROP POLICY IF EXISTS "Allow public read active channels" ON public.channels;
CREATE POLICY "Allow public read active channels"
ON public.channels FOR SELECT
USING (active = true);

DROP POLICY IF EXISTS "Allow authenticated admin full channels access" ON public.channels;
CREATE POLICY "Allow authenticated admin full channels access"
ON public.channels FOR ALL
TO authenticated
USING (true)
WITH CHECK (true);

-- B. Content Items Policies
DROP POLICY IF EXISTS "Allow authenticated admin full content access" ON public.content_items;
CREATE POLICY "Allow authenticated admin full content access"
ON public.content_items FOR ALL
TO authenticated
USING (true)
WITH CHECK (true);

-- Allow public read of active non-expired keys (or handled via service role bot)
DROP POLICY IF EXISTS "Allow public read active non-expired content" ON public.content_items;
CREATE POLICY "Allow public read active non-expired content"
ON public.content_items FOR SELECT
USING (
    active = true 
    AND (expires_at IS NULL OR expires_at > timezone('utc'::text, now()))
);

-- C. Bot Settings Policies
DROP POLICY IF EXISTS "Allow public read bot settings" ON public.bot_settings;
CREATE POLICY "Allow public read bot settings"
ON public.bot_settings FOR SELECT
USING (true);

DROP POLICY IF EXISTS "Allow authenticated admin update bot settings" ON public.bot_settings;
CREATE POLICY "Allow authenticated admin update bot settings"
ON public.bot_settings FOR ALL
TO authenticated
USING (true)
WITH CHECK (true);

-- D. Storage Policies for 'content-files' bucket
DROP POLICY IF EXISTS "Public can view content-files" ON storage.objects;
CREATE POLICY "Public can view content-files"
ON storage.objects FOR SELECT
USING (bucket_id = 'content-files');

DROP POLICY IF EXISTS "Authenticated admins can upload content-files" ON storage.objects;
CREATE POLICY "Authenticated admins can upload content-files"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'content-files');

DROP POLICY IF EXISTS "Authenticated admins can update content-files" ON storage.objects;
CREATE POLICY "Authenticated admins can update content-files"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'content-files');

DROP POLICY IF EXISTS "Authenticated admins can delete content-files" ON storage.objects;
CREATE POLICY "Authenticated admins can delete content-files"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'content-files');

-- ====================================================================
-- 7. HELPER RPC FUNCTIONS
-- ====================================================================

-- Function to get dashboard statistics directly in one query
CREATE OR REPLACE FUNCTION get_admin_dashboard_stats()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    result JSONB;
BEGIN
    SELECT json_build_object(
        'total_keys', (SELECT count(*) FROM public.content_items),
        'active_keys', (SELECT count(*) FROM public.content_items WHERE active = true AND (expires_at IS NULL OR expires_at > timezone('utc'::text, now()))),
        'total_channels', (SELECT count(*) FROM public.channels),
        'active_channels', (SELECT count(*) FROM public.channels WHERE active = true),
        'total_deliveries', (SELECT count(*) FROM public.delivery_logs)
    ) INTO result;
    RETURN result;
END;
$$;
