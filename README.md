# RC Solutions (RCOS Mobile)

Native **Android** application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**, using the **Aurora One** dark-emerald design system.

## Architecture & Tech Stack

- **UI & Navigation:** Jetpack Compose (Material 3), `@Serializable` type-safe Navigation Compose, adaptive layouts, and Material Icons Extended.
- **Architecture:** MVVM with `MainViewModel`, `StateFlow` / `collectAsStateWithLifecycle`, and sealed `UiState` wrappers.
- **Authentication:** Google Sign-In via Jetpack Credential Manager (`GetGoogleIdOption` / `GetSignInWithGoogleOption`) backed by Firebase Authentication (`com.google.firebase:firebase-auth`). Biometric lock is disabled.
- **Cloud Database & Sync:** Firebase Cloud Firestore (`com.google.firebase:firebase-firestore`) targeting the verified enterprise database (`ai-studio-rcosmobileapp-c0c46f2b-8d0f-43b8-978d-70fb08614967`, `firestore_database_id`) on Firebase project `rcos-mobile`, with zero-trust owner/member security rules (`firestore.rules`).
- **AI Assistant:** Firebase AI Logic (`Firebase.ai`, `gemini-3.8-flash` and `gemini-3.5-flash-lite`; Gemini Live audio via `gemini-3.8-live`) protected by Firebase App Check (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Local Preferences:** Jetpack DataStore Preferences (`rcos_preferences`).

## Phase 1 — Auth, business & Firestore security

- **Sign-in:** Google Sign-In only. Fresh installs always start at the login gate (`AuthScreen`); there is no demo login, no bypass, and no pre-filled account. A signed-in session persists across restarts (normal Firebase Auth), and sign-out clears credentials and the active business.
- **Business lookup:** the app finds a user's businesses two ways — `ownerId == uid` and `memberUids array-contains uid` — and merges them. This matters because the live business `biz_1794932142514` was written by the old app with `ownerId` + a `members` roster + `memberEmails` and **no `memberUids`**; a `memberUids`-only lookup returns nothing for it. New businesses created by onboarding write **both** shapes (`memberUids` + `members[]` + `memberEmails` + `documentContactInfo`), so the old app, the server, and this app can all read the same business.
- **Membership (firestore.rules):** a caller is a member of a business if ANY of these hold — Firebase UID == `ownerId`, UID in `memberUids`, or verified Google email in `memberEmails` / `contactEmail` / `ownerEmail`. Only owners can update or delete the business doc, and company contact fields (`contactName` / `contactPhone` / `contactEmail` / `ownerEmail`) are owner-only for that reason. Rules tests in `firestore.test.js` cover the live-schema business, member-via-email, and cross-user isolation.
- **Per-user profiles:** phone + contact email live on the per-person record `users/{uid}` (`UserProfile`), readable/writable only by that user. A member saving their own profile can never overwrite company contact info. Onboarding creates the profile for the new owner automatically.
- **Job categories follow the business:** `MainViewModel.jobCategories` is the business's own services first, then its industry preset's defaults — never a hardcoded trade list. A business with no services and no preset gets an honestly empty list.
- **Invoices & Quotes:** `businesses/{businessId}/invoices` and `/quotes` (money loop: Quote → Job → Invoice → Paid, no Stripe — status only). Members can create and update status; only the owner can delete.
- **Empty states are real:** the live business currently has no jobs or customers in Firestore, so Jobs/Customers show honest empty states with a first action — no seed or demo data anywhere in the app. (The word "seed" appears only in `firestore.test.js`, which seeds the local emulator for rules tests.)

### Known limits (Phase 1, not yet fixed)
- A non-owner member can read the business doc and write docs they create (their UID is stamped into `memberUids` on create), but they do **not** see other members' subcollection docs in list queries — those queries filter by `memberUids`, and the old app's jobs carry no `memberUids`. The owner sees everything.
- Legacy subcollection docs written by the old app (no `memberUids`) cannot be updated through the 1.2.1 rules validators, which require it. There are currently no such docs under the live business.
- Gemini Live fallback `gemini-3.1-flash-live-preview` in `LiveVoiceManager` is unverified (primary `gemini-3.8-live` is verified); it is only tried after three failed attempts on the primary, and a failure surfaces honestly.


## Screens

1. **Welcome & Google Sign-In:** Branded hero entry and Credential Manager authentication (fresh installs always start at the login gate).
2. **Progressive Onboarding:** Step-by-step workspace setup with AI business profile extraction and skip-for-now support.
3. **Home Dashboard:** Weather/status header, focus banner, 3×2 Quick Actions grid (including Customers, Schedule, Quote drafting, and Approvals), live System & Dispatch Feed, and Weekly Dispatch Trends chart.
4. **AI Assistant:** Unified conversational workspace with prompt chips, approval queue cards, attachment/camera/voice support, and team messaging.
5. **Calls & Voicemails:** AI Receptionist toggle, call KPI counters, filter pills (`All`, `Active`, `Missed`, `Voicemail`), outbound dialer CTA, and full call log.
6. **Tasks:** Filterable field task list (`Today`, `This Week`, `Completed`) with interactive completion toggles and task creation.
7. **Customers & Schedule:** Searchable CRM directory with status filters and customer detail inspection, plus weekly calendar strip and appointment management.
8. **Settings & Account:** Business profile editor, AI receptionist & autonomy controls, notifications, integrations, account security, and sign-out.

## Firebase Config & Local Build

1. Place your downloaded `google-services.json` for Firebase project `rcos-mobile` (`com.rcsolutions.rcosmobile`) at `app/google-services.json` (gitignored by `.gitignore`).

```bash
# Build debug APK
gradle :app:assembleDebug

# Run local Firestore security rules tests (with Firestore Emulator running on 127.0.0.1:8085)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
