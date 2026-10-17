# Email Verification Guide for Chef Mate

## Overview

This guide explains how email verification works in Chef Mate. Verification uses a **one-time code (OTP)** that Supabase emails to the user and that the user types into the app — there is no magic-link / deep-link callback. The same OTP screen also powers passwordless ("Email me a code") sign-in and the email-change confirmation used when an anonymous user upgrades to a real account.

## The Problem

When a user signs up with Supabase, by default:
1. Supabase creates a session immediately, even if email confirmation is required
2. The app sees this session and considers the user authenticated
3. The user needs to verify their email, but they're already "logged in"

## The Solution Implemented

The app handles email verification by:

1. **Checking email confirmation status** after sign-up
2. **Signing out unverified users** immediately after registration
3. **Routing the user to an in-app OTP screen** where they enter the code from the email
4. **Verifying the code with Supabase** (`verifyEmailOtp`), which creates the real session and signs the user in

### Code Structure

#### 1. Auth State (`AuthState.kt`)
- `AwaitingEmailVerification` state containing the user's email
- This state is observable by any part of the app (e.g. the Settings banner)

#### 2. Repository Layer (`AuthenticationRepository.kt` & `SupabaseAuthenticationRepository.kt`)
- `SignUpResult` sealed class:
  - `Success`: User is authenticated (no email verification required)
  - `AwaitingEmailVerification`: User needs to verify their email with a sign-up code
  - `AwaitingEmailChange`: An anonymous user was upgraded in place via `updateUser { email; password }`; Supabase sends a *Change Email Address* confirmation instead of *Confirm Signup*
  - `UserAlreadyExists`: Supabase returned a user with no identities (email already registered)
- `OtpFlow` enum — `SignUp`, `PasswordlessSignIn`, `EmailChange` — selects the Supabase token type:
  - `SignUp` → `OtpType.Email.SIGNUP`
  - `PasswordlessSignIn` → `OtpType.Email.EMAIL`
  - `EmailChange` → `OtpType.Email.EMAIL_CHANGE`
- After a normal sign-up, checks if `emailConfirmedAt` is null; if so, signs out, sets state to `AwaitingEmailVerification`, and returns `AwaitingEmailVerification`
- OTP methods:
  - `sendSignInOtp(email)` — sends a passwordless sign-in code (`signInWith(OTP)`)
  - `verifyEmailOtp(email, token, flow)` — calls `auth.verifyEmailOtp(type, email, token)`; on success the Supabase session status flips `AuthState` to `Authenticated`
  - `resendOtp(email, flow)` — `resendEmail(SIGNUP / EMAIL_CHANGE)`, or re-sends the sign-in OTP for `PasswordlessSignIn`

#### 3. Auth UI Layer (`AuthenticationViewModel.kt`, `AuthenticationBlocImpl.kt` & `AuthenticationBloc.kt`)
- Outputs for each OTP entry point:
  - `EmailVerificationRequired(email)` — sign-up needs a code
  - `EmailChangeRequired(email)` — anonymous-account upgrade needs an email-change code
  - `PasswordlessOtpSent(email)` — "Email me a code" sign-in
- `UserAlreadyExists` is surfaced as an inline error on the sign-up form

#### 4. OTP Screen (`OtpBloc.kt`, `OtpScreen.kt`, `OtpBlocImpl.kt` & `OtpViewModel.kt`)
- `OtpBloc.Props(email, flow)` drives the screen title (sign-up / passwordless / email change) and which token type is verified
- Accepts an **8-digit** numeric code (`OtpViewModel.OTP_CODE_LENGTH = 8`); validates required / length before calling the repository
- Maps Supabase errors to messages for expired, invalid, and rate-limited codes
- "Resend code" button with a 30-second cooldown (`RESEND_COUNTDOWN_SECONDS`)
- Outputs `Verified` on success and `Cancelled` on back

#### 5. Settings State Management (`SettingsViewModel.kt` & `SettingsBlocImpl.kt`)
- `SettingsViewModel` observes the auth state and tracks `emailAwaitingVerification`
- `SettingsBlocImpl` maps the email to a `TextData` message using `createEmailVerificationMessage()`
- Message is exposed through the `verificationMessage` field in the model

#### 6. Settings UI (`SettingsScreen.kt` & `SettingsTextData.kt`)
- `EmailVerificationMessage` composable displays a colored banner
- Shown above the sign-in/sign-up buttons while in the unverified state (e.g. the user backed out of the OTP screen)
- `createEmailVerificationMessage()` creates the localized message from the `email_verification_required` string resource

#### 7. Navigation Layer (`RootBlocImpl.kt`)
- `handleAuthenticationOutput` pushes `Configuration.OtpVerification(email, flow)` for `EmailVerificationRequired` (`OtpFlow.SignUp`), `EmailChangeRequired` (`OtpFlow.EmailChange`), and `PasswordlessOtpSent` (`OtpFlow.PasswordlessSignIn`)
- `handleOtpOutput`: `Verified` → `finishAuthentication()` (pops the auth + OTP screens, or completes onboarding if sign-in started there); `Cancelled` → pops back

## Current Flow

1. User enters email and password in sign-up form
2. App calls Supabase sign-up
3. Supabase creates account and sends a verification email containing a code
4. App checks if email is confirmed
5. If not confirmed:
   - App signs out the user immediately
   - Sets auth state to `AwaitingEmailVerification` with the user's email
   - Navigates to the OTP screen ("Verify your email")
6. User enters the 8-digit code from the email (or taps "Resend code")
7. App calls `verifyEmailOtp(email, code, OtpFlow.SignUp)`
8. Supabase confirms the email and issues a session; the user is signed in and the auth screens close

Anonymous-account upgrade and passwordless sign-in follow the same OTP screen with `OtpFlow.EmailChange` and `OtpFlow.PasswordlessSignIn` respectively.

## Development Options

### Disable Email Verification (Development Only)

If you want to disable email verification for development:

1. Go to Supabase Dashboard
2. Navigate to Authentication → Settings
3. Find "Enable email confirmations"
4. Toggle it OFF

**⚠️ Warning:** This is NOT recommended for production as it allows unverified emails to access your app.

### Auto-Confirm Emails in Development

Set up Supabase to auto-confirm emails for specific domains (like test emails):

1. Go to Supabase Dashboard
2. Navigate to Authentication → Settings
3. Add your test domain to "Auto-confirm email domains"

## Testing the Current Implementation

1. Sign up with a new email
2. App should navigate to the "Verify your email" code screen
3. Check your email for the verification code
4. Enter a wrong or short code (should show an inline error)
5. Enter the correct code (should sign you in and close the auth screens)
6. Optionally tap "Resend code" and confirm the 30-second cooldown

## Supabase Configuration Notes

### Email Templates

The app expects a **code**, not a link. In Supabase Dashboard → Authentication → Email Templates, make sure these templates include the `{{ .Token }}` variable:
- **Confirm signup** — sign-up verification (`OtpFlow.SignUp`)
- **Magic Link** — passwordless sign-in code (`OtpFlow.PasswordlessSignIn`)
- **Change Email Address** — anonymous-account upgrade (`OtpFlow.EmailChange`)

Templates that only contain `{{ .ConfirmationURL }}` will send a link the app can't consume.

### OTP Length

The OTP screen requires exactly 8 digits. Set the email OTP length in Supabase (Authentication → Providers → Email → "Email OTP Length") to 8 so it matches `OtpViewModel.OTP_CODE_LENGTH`.

### Redirect URLs

No auth redirect URL or app deep link is needed for verification — `SupabaseModule.kt` installs `Auth` with no `scheme`/`host`, and the app does not handle an auth callback. [DEEP_LINKING_SETUP.md](DEEP_LINKING_SETUP.md) covers platform deep links (App Links, Universal Links, URL schemes) for in-app navigation only.

## Architecture Benefits

This implementation follows clean architecture principles:

1. **Separation of Concerns**: Auth state is managed in the data layer, UI reacts to it
2. **Reusability**: One OTP screen serves sign-up, passwordless sign-in, and email change via `OtpFlow`
3. **Testability**: Each layer can be tested independently
4. **Single Source of Truth**: Auth state is the single source for verification status
5. **Reactive**: UI automatically updates when auth state changes

## Related Files

### Auth Layer
- `client/auth/data/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/data/AuthState.kt`
- `client/auth/data/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/data/AuthenticationRepository.kt` (also defines `SignUpResult` and `OtpFlow`)
- `client/auth/data/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/data/impl/SupabaseAuthenticationRepository.kt`
- `client/auth/data/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/data/impl/SupabaseModule.kt`
- `client/auth/ui/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/impl/AuthenticationViewModel.kt`
- `client/auth/ui/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/impl/AuthenticationBlocImpl.kt`
- `client/auth/ui/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/AuthenticationBloc.kt`

### OTP Screen
- `client/auth/ui/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/otp/OtpBloc.kt`
- `client/auth/ui/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/otp/OtpScreen.kt`
- `client/auth/ui/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/impl/otp/OtpBlocImpl.kt`
- `client/auth/ui/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/ui/impl/otp/OtpViewModel.kt`

### Settings Layer
- `client/settings/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/settings/impl/SettingsViewModel.kt`
- `client/settings/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/settings/impl/SettingsBlocImpl.kt`
- `client/settings/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/settings/SettingsBloc.kt`
- `client/settings/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/settings/ui/SettingsScreen.kt`
- `client/settings/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/settings/SettingsTextData.kt`
- `client/settings/public/src/commonMain/composeResources/values/strings.xml`

### Navigation Layer
- `client/root/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/root/RootBlocImpl.kt`
