-- Local-only seed data, applied by `supabase db reset` / `supabase start`.
--
-- This file is for the LOCAL Docker stack ONLY. It is never pushed to prod (only
-- `supabase/migrations/` is). It recreates the things that, on prod, were set up by hand:
--   1. The storage buckets + RLS policies (mirrors docs/supabase-storage-setup.sql and
--      docs/supabase-avatars-setup.sql, which were pasted into the prod dashboard).
--   2. A couple of confirmed email/password users so the in-app Developer Settings
--      "Login as test user" flow works against the local stack with no network.
--
-- Keep the test-user credentials below in sync with the `chefmate.user.*` entries in
-- local.properties (see docs/supabase-local-development.md).

-- ---------------------------------------------------------------------------
-- Storage: recipe-photos bucket + policies
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'recipe-photos',
  'recipe-photos',
  true,
  5242880, -- 5 MB
  array['image/jpeg', 'image/png', 'image/webp', 'image/heic']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "recipe_photos_public_read" on storage.objects;
create policy "recipe_photos_public_read"
on storage.objects for select
to public
using (bucket_id = 'recipe-photos');

drop policy if exists "recipe_photos_owner_insert" on storage.objects;
create policy "recipe_photos_owner_insert"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'recipe-photos'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "recipe_photos_owner_update" on storage.objects;
create policy "recipe_photos_owner_update"
on storage.objects for update
to authenticated
using (
  bucket_id = 'recipe-photos'
  and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
  bucket_id = 'recipe-photos'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "recipe_photos_owner_delete" on storage.objects;
create policy "recipe_photos_owner_delete"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'recipe-photos'
  and (storage.foldername(name))[1] = auth.uid()::text
);

-- ---------------------------------------------------------------------------
-- Storage: avatars bucket + policies
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'avatars',
  'avatars',
  true,
  5242880, -- 5 MB
  array['image/jpeg', 'image/png', 'image/webp', 'image/heic']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "avatars_public_read" on storage.objects;
create policy "avatars_public_read"
on storage.objects for select
to public
using (bucket_id = 'avatars');

drop policy if exists "avatars_owner_insert" on storage.objects;
create policy "avatars_owner_insert"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "avatars_owner_update" on storage.objects;
create policy "avatars_owner_update"
on storage.objects for update
to authenticated
using (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = auth.uid()::text
);

drop policy if exists "avatars_owner_delete" on storage.objects;
create policy "avatars_owner_delete"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = auth.uid()::text
);

-- ---------------------------------------------------------------------------
-- Auth: pre-baked, email-confirmed test users
-- ---------------------------------------------------------------------------
-- Inserts directly into GoTrue's tables with a bcrypt-hashed password and a matching
-- `auth.identities` row (required for email/password login on current GoTrue). Emails are
-- pre-confirmed (email_confirmed_at = now()) so no verification step is needed.
do $$
declare
  test_users text[][] := array[
    array['alice@chefmate.test', 'password123'],
    array['bob@chefmate.test', 'password123']
  ];
  u text[];
  uid uuid;
begin
  foreach u slice 1 in array test_users loop
    -- Skip if a user with this email already exists (keeps the seed idempotent).
    if exists (select 1 from auth.users where email = u[1]) then
      continue;
    end if;

    uid := gen_random_uuid();

    insert into auth.users (
      instance_id, id, aud, role, email, encrypted_password,
      email_confirmed_at, created_at, updated_at,
      raw_app_meta_data, raw_user_meta_data, is_super_admin
    ) values (
      '00000000-0000-0000-0000-000000000000', uid, 'authenticated', 'authenticated',
      u[1], extensions.crypt(u[2], extensions.gen_salt('bf')),
      now(), now(), now(),
      '{"provider":"email","providers":["email"]}'::jsonb, '{}'::jsonb, false
    );

    insert into auth.identities (
      id, user_id, provider_id, identity_data, provider,
      last_sign_in_at, created_at, updated_at
    ) values (
      gen_random_uuid(), uid, uid::text,
      jsonb_build_object('sub', uid::text, 'email', u[1]), 'email',
      now(), now(), now()
    );
  end loop;
end $$;
