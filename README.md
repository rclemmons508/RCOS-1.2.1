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

## Phase 2 — Server connection (Railway)

The app talks to the live server at `https://rcos-mobile-app-production.up.railway.app` (Railway project `rcos-voice-agent`). Base URL comes from `BuildConfig.RCOS_API_BASE_URL` (see `app/build.gradle.kts`); override it with the `rcosApiBaseUrl` Gradle property (`-PrcosApiBaseUrl=...` or `local.properties`). No API key, LiveKit secret, Vonage key, or Firebase key is in the code or in git.

### Endpoints reused (copied from `RCOS-MOBILE-APP` `server.ts` — nothing invented)

| Call | Method & path | Auth | Request / response |
| --- | --- | --- | --- |
| Server health | `GET /api/health` | Public | → `{status, timestamp}` |
| Firebase config | `GET /api/firebase-config` | Public | → `{telephonyProvider, projectId, firestoreDatabaseId, storageBucket}` |
| Diagnostics scan | `GET /api/diagnostics/scan` | Signed-in user (server answers 401 unsigned — verified live) | → `{overall, score, checks[{id, label, status, detail}], scannedAt}` |
| AI chat | `POST /api/chat` | Signed-in user; 403 when the caller is not on the business | `{messages[{role, content}], roleId, modelTier, businessId}` → `{role, content, modelUsed, roleId, timestamp}`. When Gemini is down the server still answers HTTP 200 with its own fallback text and `modelUsed: "rcos-offline-engine"` — the app surfaces that as a fallback, never as a live answer. |
| Gmail token | `GET /api/business/{id}/gmail-token` | Signed-in user, verified identity only | → `{accessToken, email}`. 404 = Gmail not connected for this user yet (a normal state, not an error). The token is held in memory only, never written to disk or Firestore by the app. |
| Gmail OAuth start | `POST /api/business/{id}/gmail/oauth-start` | Signed-in user, verified identity only | → `{url, state, expiresAt}`. Tokens live server-side in Firestore (`serverState` doc, AES-256-GCM envelope when `TOKEN_ENCRYPTION_KEY` is set) — never in a server local file. |

Auth on every gated call is the signed-in user's Firebase ID token (`Authorization: Bearer <token>`, same as the old app's `apiFetch`). An unsigned call gets 401 from the server; the client never bypasses the gate and never sends a token for anyone but the signed-in user.

### What Phase 2 adds

- `data/ServerApiClient.kt` — `HttpURLConnection` client (no new dependency). Every call returns a typed `ApiResult` (`Success` / `HttpError(status, message)` / `NetworkError`); failures are surfaced, never faked as success. 15-second timeout, matching the old app.
- `data/ConnectivityMonitor.kt` — real connectivity from `ConnectivityManager` (`ONLINE` / `OFFLINE` / `UNKNOWN`). Going offline posts the existing banner once ("You're offline — showing your saved data") and coming back online re-probes the server automatically. There is no fixed ONLINE light anywhere.
- Firestore offline cache is now set explicitly in `RcosRepository` (`isPersistenceEnabled = true`); the app keeps working from the disk cache while offline. `MainViewModel.refreshServerAndData()` is the single entry point for pull-to-refresh gestures (reload Firestore + re-probe the server).
- `MainViewModel` exposes `connectionState`, `serverStatus`, `diagnosticsState` (`runDiagnostics()`), `gmailConnection` (`checkGmailConnection()`), `gmailOAuthStart` (`startGmailOAuth()`), and `serverChatState` (`sendChatViaServer(...)` — transport only; the Assistant page UI wiring is Phase 3).

### Known limits (Phase 2, not yet fixed)

- Pull-to-refresh **gestures** on screens are not wired yet — the backing (`refreshServerAndData()`) is ready; gesture wiring lands with the Phase 3 UI work. Loading placeholders and retry already exist (`UiState.Loading` + retry buttons).
- The diagnostics **screen** (Settings → AI Agents → Run System Scan) is Phase 7; Phase 2 only provides the `runDiagnostics()` transport and `diagnosticsState`. The server scan itself is verified live (health + firebase-config answered; diagnostics answers 401 unsigned, as designed).
- `POST /api/chat` and the Gmail calls need a signed-in user and have not been exercised end-to-end from the phone yet — that is the Phase 2 phone test below, not a build check.
- No visual file changed in Phase 2. The offline signal reuses the existing banner/snackbar; no new badge, color, font, or layout was added.

### Phone test (Phase 2, after merge + install of the main APK)

1. Sign in → the app probes the server (no error banner while online).
2. Turn on airplane mode → banner "You're offline — showing your saved data", and Jobs/Customers still show your cached data.
3. Turn airplane mode off → the app re-probes the server automatically, no manual refresh needed.
4. Pull to refresh (or use the retry action) → data reloads and the server status refreshes; if the server is unreachable you get a clear error, never a fake success.


## Login fix — stable debug keystore (Nov 25)

**What broke:** every APK built by the Build APK workflow was signed with a brand-new debug keystore generated on the runner, so its SHA-1 was never registered in Firebase. Google Sign-In rejected the unregistered caller: after picking a Google account the login screen just stayed put with no error, on every CI-built APK.

**The fix:**
- One stable debug keystore is now used for every build. The workflow restores it from the `DEBUG_KEYSTORE_BASE64` repository secret (base64 of the keystore file) instead of generating a fresh one. The keystore itself is never committed (`debug.keystore` is gitignored); it uses the standard debug values — store password `android`, alias `androiddebugkey`, key password `android` — so no extra secrets are needed.
- Its SHA-1 fingerprint is `A1:45:78:62:94:CF:2A:BF:95:87:6B:06:E5:66:80:6E:13:B1:EB:FA` (SHA-256 `12:71:32:EB:01:9E:EF:DE:37:D3:F4:0C:57:02:D7:98:C8:E1:2C:70:77:FD:76:8B:85:91:10:8F:21:70:83:6A`). **That SHA-1 must be registered in Firebase Console → Project settings → the Android app (`com.rcsolutions.rcosmobile`) → SHA certificate fingerprints → Add fingerprint.** Until it is registered, CI-built APKs still cannot sign in — the only fingerprint registered before this fix (`99:b3:3d:da…`) belongs to a different keystore.
- `onGoogleSignInClicked` (`data/AuthManager.kt`) now bounds sign-in to 25 seconds: a hung sign-in shows an error on the login screen instead of hanging silently, and the error message is never blank.
- Rotating the keystore later means: generate a new one, replace the `DEBUG_KEYSTORE_BASE64` secret, and register the new SHA-1 in Firebase the same way.

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
