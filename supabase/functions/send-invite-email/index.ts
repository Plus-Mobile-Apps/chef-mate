// Supabase Edge Function: send-invite-email
//
// Emails a collaboration invitee when they're invited to a shared grocery list, recipe book, or
// family. Invites are plain INSERTs into `grocery_list_members` / `recipe_book_members` /
// `family_members` with status='pending', done directly by every client (iOS/Android/Desktop/Web).
// A database trigger (see supabase/migrations/20260707_invite_email_notification.sql, extended for
// families by 20260813_add_families.sql) fires `pg_net` at this function on each new pending
// invite, so the notification is client-agnostic and needs no app-side code.
//
// Auth: callers must present `Authorization: Bearer <INVITE_HOOK_SECRET>`. Only the DB trigger
// should reach this — it must NOT be invokable with an ordinary user JWT. Set INVITE_HOOK_SECRET
// to a long random value and store the same value in Vault for the trigger (see the migration).
//
// Body (sent by the trigger):
//   { kind: "grocery" | "recipe_book" | "family", memberId, parentId, invitedEmail, invitedBy, role }
// `invitedBy` is present for grocery and family invites; recipe-book invites fall back to the book
// owner.
//
// Deploy:  supabase functions deploy send-invite-email
// Secrets: supabase secrets set RESEND_API_KEY=<key> INVITE_HOOK_SECRET=<random>
//          (SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY are injected automatically by Supabase.)
//          RESEND_FROM and APP_INVITE_URL are optional overrides for the sender / call-to-action.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { APP_ICON_BASE64, APP_ICON_CONTENT_ID, APP_ICON_FILENAME } from "./icon.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const DEFAULT_FROM = "Chef Mate <noreply@plusmobileapps.com>";
const DEFAULT_APP_URL = "https://chefmate.plusmobileapps.com";

type InviteKind = "grocery" | "recipe_book" | "family";

interface InvitePayload {
  kind: InviteKind;
  memberId: string;
  parentId: string;
  invitedEmail: string;
  invitedBy?: string | null;
  role?: string | null;
}

// Per-kind copy and the parent table to resolve the name/owner from. All three parents expose
// `name` and `owner_id`, so one lookup shape covers them.
const KINDS: Record<InviteKind, { table: string; label: string; fallbackName: string }> = {
  grocery: { table: "grocery_lists", label: "grocery list", fallbackName: "a grocery list" },
  recipe_book: { table: "recipe_books", label: "recipe book", fallbackName: "a recipe book" },
  family: { table: "families", label: "family", fallbackName: "a family" },
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const hookSecret = Deno.env.get("INVITE_HOOK_SECRET");
    if (!hookSecret) {
      return json({ error: "INVITE_HOOK_SECRET is not configured" }, 500);
    }
    const authHeader = req.headers.get("Authorization");
    if (authHeader !== `Bearer ${hookSecret}`) {
      return json({ error: "Unauthorized" }, 401);
    }

    const resendApiKey = Deno.env.get("RESEND_API_KEY");
    if (!resendApiKey) {
      return json({ error: "RESEND_API_KEY is not configured" }, 500);
    }

    const payload = (await req.json()) as InvitePayload;
    if (!payload?.kind || !payload?.parentId || !payload?.invitedEmail) {
      return json({ error: "Missing kind, parentId, or invitedEmail" }, 400);
    }

    const kind = KINDS[payload.kind];
    if (!kind) {
      return json({ error: `Unknown kind: ${payload.kind}` }, 400);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const admin = createClient(supabaseUrl, serviceRoleKey);

    // Resolve the parent list/book/family: its display name and owner (the inviter fallback).
    const { data: parent, error: parentError } = await admin
      .from(kind.table)
      .select("name, owner_id")
      .eq("id", payload.parentId)
      .single();

    if (parentError || !parent) {
      return json(
        { error: `Could not load ${kind.table}: ${parentError?.message ?? "not found"}` },
        404,
      );
    }

    const listName = (parent.name as string) || kind.fallbackName;
    const kindLabel = kind.label;

    // Inviter: `invited_by` when present (grocery), else the parent owner (recipe books).
    const inviterId = payload.invitedBy ?? (parent.owner_id as string | null);
    const inviterName = await resolveInviterName(admin, inviterId);

    const baseUrl = Deno.env.get("APP_INVITE_URL") ?? DEFAULT_APP_URL;
    // Deep link straight to the in-app Notifications screen via Universal Link / App Link. The app
    // verifies chefmate.plusmobileapps.com, so this opens the app when installed and falls back to
    // the web page otherwise. Trailing slash trimmed so we don't emit a double slash.
    const appUrl = `${baseUrl.replace(/\/+$/, "")}/notifications`;
    const from = Deno.env.get("RESEND_FROM") ?? DEFAULT_FROM;
    const subject = `${inviterName} invited you to “${listName}” on Chef Mate`;

    const resendResponse = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${resendApiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from,
        to: payload.invitedEmail,
        subject,
        html: htmlBody(inviterName, listName, kindLabel, appUrl),
        text: textBody(inviterName, listName, kindLabel, appUrl),
        // Inline the app icon as a CID attachment. Email clients (Gmail included)
        // block `data:` image URIs but render `cid:` inline attachments, so the icon
        // ships with the message rather than being hotlinked from a web host.
        attachments: [
          {
            filename: APP_ICON_FILENAME,
            content: APP_ICON_BASE64,
            content_id: APP_ICON_CONTENT_ID,
            content_type: "image/png",
          },
        ],
      }),
    });

    if (!resendResponse.ok) {
      const detail = await resendResponse.text();
      return json({ error: `Resend failed (${resendResponse.status}): ${detail}` }, 502);
    }

    return new Response(null, { status: 204, headers: corsHeaders });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});

// Inviter's display name from auth.users metadata, falling back to their email, then "Someone".
async function resolveInviterName(
  admin: ReturnType<typeof createClient>,
  inviterId: string | null,
): Promise<string> {
  if (!inviterId) return "Someone";
  try {
    const { data, error } = await admin.auth.admin.getUserById(inviterId);
    if (error || !data?.user) return "Someone";
    const name = data.user.user_metadata?.name;
    if (typeof name === "string" && name.trim().length > 0) return name.trim();
    return data.user.email ?? "Someone";
  } catch (_) {
    return "Someone";
  }
}

function htmlBody(inviter: string, listName: string, kindLabel: string, appUrl: string): string {
  return `
    <div style="font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; color: #1a1a1a;">
      <img src="cid:${APP_ICON_CONTENT_ID}" alt="Chef Mate" width="64" height="64"
        style="width: 64px; height: 64px; border-radius: 14px; display: block; margin-bottom: 16px;" />
      <h2 style="margin-bottom: 8px;">You’ve been invited to collaborate</h2>
      <p><strong>${escapeHtml(inviter)}</strong> invited you to the ${kindLabel}
        <strong>“${escapeHtml(listName)}”</strong> on Chef Mate.</p>
      <p>Open Chef Mate with this email address to accept the invite.</p>
      <p style="margin: 24px 0;">
        <a href="${appUrl}" style="background: #2e7d32; color: #fff; padding: 12px 20px; border-radius: 8px; text-decoration: none; display: inline-block;">Open Chef Mate</a>
      </p>
      <p style="color: #777; font-size: 13px;">If you weren’t expecting this, you can safely ignore this email.</p>
    </div>`;
}

function textBody(inviter: string, listName: string, kindLabel: string, appUrl: string): string {
  return [
    `${inviter} invited you to the ${kindLabel} “${listName}” on Chef Mate.`,
    ``,
    `Open Chef Mate with this email address to accept the invite: ${appUrl}`,
    ``,
    `If you weren’t expecting this, you can safely ignore this email.`,
  ].join("\n");
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
