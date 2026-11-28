# RC Solutions (RCOS Mobile)

Native **Android** application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**, using the **Aurora One** dark-emerald design system.

## Architecture & Tech Stack

- **UI & Navigation:** Jetpack Compose (Material 3), `@Serializable` type-safe Navigation Compose, adaptive layouts, and Material Icons Extended.
- **Architecture:** MVVM with `MainViewModel`, `StateFlow` / `collectAsStateWithLifecycle`, and sealed `UiState` wrappers.
- **Authentication:** Google Sign-In via Jetpack Credential Manager (`GetGoogleIdOption` / `GetSignInWithGoogleOption`) backed by Firebase Authentication (`com.google.firebase:firebase-auth`). Biometric lock is disabled.
- **Cloud Database & Sync:** Firebase Cloud Firestore (`com.google.firebase:firebase-firestore`) targeting the verified enterprise database (`ai-studio-rcosmobileapp-c0c46f2b-8d0f-43b8-978d-70fb08614967`, `firestore_database_id`) on Firebase project `rcos-mobile`, with zero-trust owner/member security rules (`firestore.rules`).
- **AI Assistant:** text chat goes through the Railway server (`POST /api/chat`, Gemini `gemini-3.8-flash` / `gemini-3.5-flash-lite` / `gemini-3.1-pro-preview` by tier); live voice is a LiveKit room (`livekit-android` 2.30.0) joined from a server-minted token, with the same Gemini realtime agent as the phone receptionist. Direct Firebase AI Logic (`Firebase.ai`) is still used only for photo analysis, which has no server endpoint. Protected by Firebase App Check (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
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
- (Superseded in Phase 3: the Gemini Live fallback in the deleted `LiveVoiceManager` is gone — live voice is LiveKit now.)

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
- `MainViewModel` exposes `connectionState`, `serverStatus`, `diagnosticsState` (`runDiagnostics()`), `gmailConnection` (`checkGmailConnection()`), `gmailOAuthStart` (`startGmailOAuth()`), and `serverChatState` (`sendChatViaServer(...)` — transport; wired to the Assistant page in Phase 3).

### Known limits (Phase 2, not yet fixed)

- Pull-to-refresh **gestures** on screens are not wired yet — the backing (`refreshServerAndData()`) is ready; gesture wiring did not land in Phase 3 (out of scope) and is still pending. Loading placeholders and retry already exist (`UiState.Loading` + retry buttons).
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

## Phase 3 — AI Assistant page (text chat, documents, live voice)

- **Text chat is the server now:** the Assistant page sends through `POST /api/chat` (`MainViewModel.sendAiMessage` → `ServerApiClient.postChat`, last 24 messages, `roleId` = selected employee as-is, `modelTier` from the task profile: FAST → `fast`, DEEP_REASONING → `complex`, everything else → `general`). The reply is saved with the server's `modelUsed`; when the server answers with its offline fallback (`modelUsed: "rcos-offline-engine"`) the reply is still saved but a banner says so — it is never presented as a live answer. On an HTTP/network error the chat shows the error honestly; the app never silently falls back to direct Gemini for text.
- **Documents (both purposes):** Attach on the Assistant page uploads PDF / Word / text / CSV (10 MB max) to `POST /api/assistant/documents` (60s timeout, same as the old app) and the server extracts the text + client details. The documents list (`GET /api/assistant/documents`, newest first) shows name, size, and extracted contact details; each row can be attached to the next chat message (the server injects the document as grounded context and never invents details from it), turned into a job draft ("Create a job for {client} about {scope}" sent as a chat message — Morgan drafts, nothing is created until confirmed), or added as a customer (two-tap confirm, uses the extracted name/phone/email). Delete is two-tap confirm (`DELETE /api/assistant/documents/{id}`). Documents are server-side only — no Firestore rules change was needed.
- **Live voice is LiveKit now:** the Talk / Go Live button mints a room token from `POST /api/voice/token` (persona from the selected employee: executive_assistant → `executive`, ai_receptionist → `receptionist`, sales → `sales`, operations → `operations`, customer_service → `support`, technician → `technician`, finance → `finance`, marketing → `marketing`, anything else → `executive`), joins the room with `livekit-android` 2.30.0, enables the mic, and waits up to 25s for the agent to join — the same LiveKit + Gemini realtime engine as the phone receptionist. Voice starts on the normal earpiece (`AudioManager.MODE_IN_COMMUNICATION`, speakerphone off) with an on-screen speakerphone toggle in the live voice banner. The old `LiveVoiceManager` (direct Firebase AI Live, `gemini-3.8-live`) is deleted. The Calls screen's live-receptionist button uses the same path with persona `receptionist`.
- **What changed under the hood:** `livekit-android` 2.30.0 (JitPack repository added for its `audioswitch` dependency), `MODIFY_AUDIO_SETTINGS` permission, new `data/LiveKitVoiceManager.kt`, `ServerApiClient` gains voice-token + document endpoints and a per-call timeout override, `MainViewModel` gains document state + actions and the LiveKit voice flow. No visual file changed layout, text, colors, fonts, sizes, spacing, or icons, except two functional additions in the existing Aurora style: the Documents section on the Assistant page (visible only when documents exist / an upload is running / there is a document error) and the speakerphone toggle in the live voice banner.

### Known limits (Phase 3, not yet fixed)
- Photos still go through direct Firebase AI Logic (`AiRepository`) — `POST /api/chat` is text-only and has no photo endpoint, so the split is documented, not hidden. Photo analysis uses the same per-business system prompt.
- The voice "nudge" chips are gone: the LiveKit voice-agent worker only listens to audio — it has no text-injection/data channel (verified in the worker source) — so a tap would have been silently ignored. Speak to the agent, or send the question in text chat.
- There is no in-chat job-confirmation card yet: "Job" from a document sends Morgan a draft request as a chat message; creating the job from that draft is Phase 5 money-loop work.
- If the voice agent drops mid-call, the banner stays LIVE until you tap End — reconnect by starting a new session.
- Pull-to-refresh gestures are still unwired (Phase 2 limit stands).
- `POST /api/chat`, the document endpoints, and `POST /api/voice/token` have not been exercised end-to-end from the phone yet — that is the Phase 3 phone test below, not a build check.

### Phone test (Phase 3, after merge + install of the main APK)
1. Sign in → open the AI Assistant page → send one text message → get a real reply from the server (the reply shows the server's model id).
2. Attach a document (PDF or Word, under 10 MB) → it appears in the Documents section with extracted details → ask a question about it → the answer uses the document. Tap Attach on a document, then ask a follow-up — the reply stays grounded in it.
3. From a document with a client name, tap Job → Morgan drafts a job in chat; tap Client → two-tap confirm → the customer appears in Customers.
4. Tap Go Live → the session starts on the earpiece (not the speakerphone) → hold a 30-second conversation → tap the Speakerphone toggle to confirm it flips → tap End.


## Hotfix — accept ISO-string timestamps on live business docs

- **The bug:** after the Firestore rules publish fixed `PERMISSION_DENIED`, workspace sync failed with "Could not deserialize object. Failed to convert value of type java.lang.String to Timestamp (found in field 'createdAt')". The live business doc (`biz_1794932142514`, written by the old React app) stores `createdAt`/`updatedAt` as ISO-8601 strings (e.g. `"2026-11-17T16:15:42.517Z"`), while the 1.2.1 models declare them as `@ServerTimestamp Timestamp?` — Firestore's mapper throws on String → Timestamp.
- **The fix (reads only, no live-data writes):** new `data/FirestoreMapping.kt` — `DocumentSnapshot.toModel` / `QuerySnapshot.toModels` deserialize exactly like `toObject(..., ESTIMATE)` (via `CustomClassMapper`, the same mapper `toObject` uses internally) but first rewrite `createdAt`/`updatedAt` string values to real `Timestamp`s (hand-rolled ISO-8601 parser — `java.time` is not used because minSdk is 24 and the build has no core-library desugaring, so `Instant.parse` would crash on API 24/25; an unparseable value becomes `null` instead of crashing the sync). `RcosRepository` routes all three deserialization sites through it (generic list loader, `getUserProfile`, weekly dispatch jobs). These are the only `Timestamp`-typed fields any model declares, so the two keys cover every model class; `BusinessMember` has no timestamp field, so `members[].joinedAt` (a string on live docs) is still ignored as before.
- **Writes unchanged:** creates/updates still send `FieldValue.serverTimestamp()` — the published rules require real timestamps on create.

### Phone test (hotfix, after merge + install of the main APK)
1. Sign in → the workspace loads RC Solutions (no more deserialize error) → Jobs and Customers show honest empty states.
2. Sign out and back in → the workspace still loads (the string timestamps parse the same way every time).
- **Follow-up fix (PR #6):** Build APK run #13 failed only on a compile error at `FirestoreMapping.kt:93` — `CustomClassMapper.convertToCustomClass` expects a `DocumentReference` as its third argument, not the `DocumentSnapshot`; one-line fix: `this` → `this.reference`.
- **Follow-up fix (PR #7):** after the timestamp fix, sync failed again with "'id' was found from document businesses/biz_1794092161535, cannot apply @DocumentId on this property for class BusinessAccount". The old business doc (`biz_1794092161535`, pre-Nov-17, still in the DB alongside the live `biz_1794932142514`) stores an `id` field in its data; every model declares `id` as `@DocumentId`, and the mapper throws when the data also contains it. `FirestoreMapping.toModel` now drops the `id` key before mapping — the id always comes from the document reference, never the data. Reads only; the old doc is left untouched. Selection check: `MainViewModel` picks the stored `activeBusinessId` when set, otherwise the most recently updated business (`RcosRepository` sorts by `updatedAt` desc) — so on a fresh install the live Nov-17 business should win; if the stale doc was ever touched more recently, the app would open it instead, and switching is a manual `switchBusiness` — that selection follow-up is deliberately NOT in this PR.

## Hotfix — rules: `memberUids is list` guard denied every array-contains list query

- **The bug:** after PR #7 fixed the mapping errors, workspace sync went back to `PERMISSION_DENIED: Missing or insufficient permissions`. The app's `observeBusinesses` runs two list queries and fails if either fails: Q1 `businesses where ownerId == uid` was allowed, but Q2 `businesses where memberUids array-contains uid` was denied by the rules themselves — proven in the local Firestore emulator against the repo's own rules (firebase-tools 15.37.0, seeded with live-shaped docs, signed in as Ryan). The same guard denied the jobs and customers list queries too (Q3/Q4).
- **The cause (one line, `firestore.rules:68`):** `isMemberData` guarded the membership check with `data.memberUids is list && request.auth.uid in data.memberUids`. Firestore's query checker can prove `uid in memberUids` from a `whereArrayContains("memberUids", uid)` filter, but it cannot derive the `is list` type check from that filter, so the whole `&&` is unprovable and the query is denied — even though the owner branch would have passed a single-doc read. The guard sat in `isMemberArray` → `isMemberData`, which every `allow list` in the file uses, so all 13 subcollection observers were affected.
- **The fix (rules only, no app change, no new APK needed for the sync):** line 68 is now `request.auth.uid in data.get('memberUids', [])` — a missing field evaluates to `uid in []` → false and falls through cleanly to the `memberEmails` branch, instead of erroring. Verified in the emulator with clean restarts: Q1/Q2/Q3/Q4 all 200, single-doc GETs on the live and old business docs 200, and GET on an orphan doc (different owner) still 403 — access for other owners stays closed.
- **Deploy note:** the rules file in git is not live until published. After this PR merges, publish the rules from main with the Firebase Rules API (Cloud Shell, byte-exact from main — the console editor paste does not work from the cloud browser), then tap Retry Connection in the app. No APK rebuild is needed for a rules-only change.

### Phone test (rules fix, after merge + rules publish — no new APK needed)
1. Sign in → the workspace loads RC Solutions (no more PERMISSION_DENIED) → Jobs and Customers show honest empty states.
2. Sign out and back in → the workspace still loads.

## Rework — onboarding, workspaces, team chat, dialpad, agent editing, notifications (Nov 26)

One PR for all seven items from Ryan's Nov 26 screenshots. The exact-design lock is lifted for these named screens only — the Aurora dark-emerald language (colors, fonts, radii) is kept everywhere; the rework is layout/structure, not a new theme. No mock/demo data anywhere; removals leave real empty states. **Rebuild rule (Ryan, Nov 26): this app must function exactly like the original RCOS Mobile app, only with the updated look — where 1.2.1 and the original differ, the original's behavior wins.**

- **1. App Check fix:** the onboarding screen showed red "Firebase App Check token is invalid." and "Extract Details with AI" failed. Root cause is server-side, not code: Firebase AI Logic is Basic Enforced and this build's debug token was never registered in the console, so every token is rejected. The code change: `AiRepository` detects an App Check rejection and the onboarding screen shows a setup card instead of the raw error — with the debug token itself, read from the SDK's debug store (`AppCheckInit.readDebugSecret`, debug builds only) and a Copy button — so it can be pasted into the console without adb/Logcat. No token is ever committed. **Console step (one time, after install):** Firebase console → Project Settings → App Check → the Android app (`com.rcsolutions.rcosmobile`) → Manage debug tokens → Add debug token → paste the token shown in the app → Save → tap Extract again. Alternative (not recommended): set Firebase AI Logic enforcement to Monitor.
- **2. Onboarding rebuild:** step 1 keeps the paste box and flyer photo, and adds an "Upload Document" picker (PDF, Word `.docx`, text, CSV, image — new `data/DocumentTextExtractor.kt`: PDF pages render via `PdfRenderer`, `.docx` text is read from `word/document.xml`; older `.doc` and unknown types get a plain message, never a silent failure). The AI fills the sections from what the document actually states; missing fields stay empty, never invented. Step 2 is now sections — Business (name, trade, services, service area, hours), Contact (name, phone, email), Prices & Rates, Team & Coverage (team size, residential/commercial switches) — every section editable by hand. "Skip for now — Pick Trade Manually" still works. New fields are stored on the business doc (`contactPhone`, `teamSize`, `servesResidential`, `servesCommercial`); the extraction schema in `AiRepository` gained the same fields.
- **3. Workspace switcher:** lists every workspace the owner owns or is a member of (the existing `observeBusinesses` query — no other owner's business can appear), each row tappable to switch, no delete button, nothing hidden. Same-named workspaces are distinguished by real data only — trade + created date (e.g. "Landscaping · Created Nov 17, 2026") via `workspaceSubtitle` in `AccountScreen.kt`. The whole Business Profile dialog scrolls, so a long list can't overflow it. Single-workspace case (the current final state: one live "RC Solutions" workspace): one row under a "Current Workspace:" header with a ✓, never an empty list; zero workspaces shows a real empty state ("No workspaces yet — create one below to get started."). The switcher is count-agnostic — if a test workspace is added later, it lists and switches the same way.
- **4. Team chat as a real thread:** the "Connected Backends" text block and the bare "Message team..." box are gone. `TeamChatThread` (full-screen dialog in `AccountScreen.kt`) shows message bubbles (own vs others), sender names, timestamps, an input + Send button, auto-scroll to the latest message, and a real empty state ("No team messages yet"). Still wired to the same `teamMessages` source and `sendTeamMessage`.
- **5. Real dialpad:** `CallsScreen` now has a numeric dialpad above the bottom CTAs (0-9, *, #, backspace, green call button, number display). The call button opens the existing Dial & Log dialog prefilled with the dialed number — name and note stay editable there — and the same log + `ACTION_DIAL` path runs. "Make a Call" still opens the blank dialog.
- **6. AI agent editing + toggle glitch:** tapping an agent row in AI Team & Automation opens `AgentEditScreen` (full-screen): what the agent does, its permissions (8-item catalog in `RcosDefaults.AGENT_PERMISSIONS` with per-agent defaults), thresholds (act-on-its-own under $ / escalate above $, money-moves-need-approval switch), and custom instructions. Saved per agent as `agentSettings.<employeeId>` on the business doc (`MainViewModel.updateAgentSettings`); an unedited agent reads the defaults. **Rules change:** `firestore.rules` adds `'agentSettings'` to the businesses update allowlist, with a matching test in `firestore.test.js` — publish the rules after this PR merges or agent-settings saves will be denied. The Support Assistant toggle glitch was the AI Team dialog overflowing (13+ rows in a fixed column, bottom rows rendered under the button bar): the dialog content now scrolls.
- **7. Notifications toggle fix:** diagnosis confirmed in code — the manifest had no `POST_NOTIFICATIONS` and no notification channel existed anywhere, so the phone's Settings toggle had nothing to grant. Fix: the permission is declared in `AndroidManifest.xml`, `notifications/CallAlertNotifications.kt` creates the `rcos_calls` channel (same id as the old app, high importance, lock-screen visible) on launch, and Account → Notifications shows the real grant state with an Enable button (runtime request on Android 13+; opens the app's notification settings if the request was denied). **Explicitly NOT in this PR:** the full push-delivery path (receptionist answers → an alert arrives). This app has no FCM, no server, and no LiveKit webhook — nothing was invented or faked. That is a separate follow-up (Ryan approved it for after this PR).

## Server business re-sync — fixes "You are not on this team" (403)

The Railway server keeps its own business store (local JSON, wiped on every
redeploy) and its login gate refuses any businessId the caller is not a member
of. The app never registered its business on the server, so after a redeploy
live voice, Assistant chat, and Documents all answered 403 "You are not on
this team" even though Firestore had the business with Ryan as owner. This
ports the old app's re-sync (RCOS-MOBILE-APP PR #58): when the active business
is first observed or switches, and again every time the app returns to the
foreground, the app re-saves the business to the server (`POST /api/businesses`
with the full record — ownerId, members roster with the owner entry,
contactEmail). The server answers 200 for an owner re-saving their own
business, 201 for a new one, 409 only if someone else owns that id. A GET
first (`GET /api/businesses`, caller-scoped) skips the POST when the server
already has it. The sync is silent and best-effort: a failure only logs, never
blocks the app or shows an error — the gate surfaces the real error if a call
is attempted.

- App side: `ServerApiClient` (`listServerBusinesses`, `registerBusiness`,
  `BusinessSyncRequest` + `BusinessAccount.toBusinessSyncRequest`),
  `MainViewModel` (`syncBusinessToServer`, `onAppForegrounded`, in-flight
  dedupe per business), `MainActivity` (ON_RESUME hook in the shell).
- No server changes (different repo). No Firestore changes. The gate error
  text comes from the server — the app never invents a team-state message.

### Phone test (re-sync, after merge + install of the main APK)
1. Install the new build, open the app, wait a few seconds (the sync runs on load).
2. Calls → Talk Live with AI Receptionist → the live voice session starts (no 403).
3. AI → type a message → the Assistant answers (no "You are not on this team").
4. AI → Documents → the document list loads (no "You are not on this team").
5. Background the app, wait, reopen it → repeat 2–4 (the foreground re-sync covers a redeploy wipe while backgrounded).

### Phone test (rework, after merge + install of the main APK)
1. Onboarding: create a new workspace → upload a company PDF/flyer → the sections fill from the document (blank where the document says nothing) → edit a field by hand → Launch Workspace. If the App Check card appears, copy the token into the Firebase console (step above) and retry extraction.
2. Account → Business Profile → the workspace list shows each workspace with its trade + created date → tap one → the app switches to it.
3. Account → Team Chat → send a message → it appears as your bubble with a timestamp; the empty state shows when there are no messages.
4. Calls → dial a number on the pad → tap the green call button → the Dial & Log dialog opens with the number prefilled → add a name/note → Dial & Log places the call and logs it.
5. Account → AI Team & Automation → scroll to the bottom (no doubled/overlapping toggle) → tap Support Assistant → change a permission and a threshold → Save → reopen it → the changes stuck.
6. Phone Settings → Apps → RCOS Mobile → Notifications → the toggle turns on and stays on. Account → Notifications → "Phone Notifications" reads On.
7. After merge, publish the Firestore rules from main (same Rules API flow as the PR #8 fix) so agent-settings saves pass.

## LiveKit in-app dialer — dial out from the app over LiveKit SIP (Nov 26)

The original app's dialer (RCOS-MOBILE-APP PR #75), ported to 1.2.1. Calls → dial a
number on the pad → Dial & Log → the call happens **in the app** over LiveKit SIP (no
phone dialer), earpiece by default, speakerphone toggle, DTMF pad during the call, 50s
no-answer watchdog, hangup. The call is logged as outbound only when it actually
connected for > 0 seconds.

- Server side (unchanged, already live on Railway): `POST /api/calls/dial` (creates the
  room, dials the number in as a SIP participant on the stored Vonage outbound trunk
  `ST_hSBykFyskkmi`, returns a publish-capable token; NO agent is dispatched into an
  outbound room), `POST /api/calls/dial/{room}/hangup` (deletes the room),
  `GET /api/calls/outbound-status`. Outbound calls show +1 747 313 5541 as caller ID.
  Vonage balance was $0.70 with an "active paid account required" notice — top up
  Vonage first if the test call fails.
- App side: `data/OutboundCallManager.kt` (dial/join/watchdog/DTMF/earpiece/hangup),
  `ui/screens/OutboundCallOverlay.kt` (full-screen in-call UI), `ServerApiClient`
  (`fetchOutboundStatus`, `dialOutbound` 30s timeout, `hangupOutbound`),
  `MainViewModel` (`startOutboundCall` / `endOutboundCall` /
  `toggleOutboundSpeakerphone` / `toggleOutboundMute` / `sendOutboundDtmf` /
  `refreshOutboundStatus`, logs the call on end), `CallsScreen` (Dial & Log confirm
  starts the in-app call; honest setup/error text under the dialpad), `MainActivity`
  (overlay rendered above the Scaffold).
- The receptionist live voice (Talk Live) and the Assistant live voice are untouched —
  an outbound call and a live voice session cannot run at the same time (each refuses
  while the other is active).
- Call-row tap and voicemail "Call Back" still open the phone dialer (ACTION_DIAL) —
  out of scope for this PR; a follow-up can route them through the in-app dialer too.

### Phone test (dialer, after merge + install of the main APK)
1. Calls → dial your own number on the pad → tap the green call button → add a name →
   Dial & Log → the in-call screen opens in the app (not the phone dialer), you hear
   ringing in the earpiece, your other phone rings showing +1 747 313 5541.
2. Answer on the other phone → talk both ways → the timer runs → tap the keypad →
   press 1 → the digit shows in the sent-digits record.
3. End the call → the call appears in the call log as outbound with the real duration
   (a call that never connected logs nothing).
4. Dial a bad number (e.g. 123) → an honest error, no fake connected state. (Trunk not
   configured → the amber setup note under the dialpad.)
5. Talk Live with the AI Receptionist still works after the dialer build, and starting
   live voice during an outbound call is refused with a plain message.

## Assistant keyboard fix — input stays above the keyboard

The AI Assistant's message box was hidden behind the soft keyboard while
typing — the screen's root layout never consumed the IME insets (the manifest
already had `adjustResize`). Fix: `.imePadding()` on the Assistant screen's
root Column (same pattern as AccountScreen), so the input bar and the
Voice/Camera/Attach pills sit above the keyboard. Phone test: AI → tap the
message box → type — the text you are typing stays visible above the keyboard.

## All-eight fixes — 470 calls alert, visible call log, richer customers, chat refresh/delete, business profile, team add, personal profile, App Check card (Nov 26)

Ryan installed the PR #11 build and reported 8 things at once ("All in one build"):
calls to +1 470 596 9505 never showed up and never alerted; the call log was buried in a
dialog and looked bad; the customer card was bare and the follow-up button text clipped;
the AI chat never refreshed and threads couldn't be deleted; the business profile was
thin (no industry change, no jobs/services); team chat had no way to add anyone; his own
personal profile with contact info was gone; the raw "Firebase App Check token is
invalid." banner still appeared. All 8 are in this one PR, one build.

1. **470-number calls show up + alert (app-side only, no server change).**
   FCM added (`firebase-messaging` via the BoM; `google-services.json` is restored by CI
   from the same `GOOGLE_SERVICES_JSON` secret). The token is registered on launch and on
   every foreground return via `POST /api/push/register` — the exact contract the old app
   uses (`src/lib/pushNotifications.ts`), so the old Railway server's `notifyCallAnswered`
   (PR #88) sends the alert to this app's token with no server change. `notifications/
   RcosFirebaseMessagingService` handles the `live-call` message: high-priority,
   lock-screen-visible (`rcos_calls` channel, `VISIBILITY_PUBLIC`), tap opens Calls via
   the `rcosmobile://calls` deep link (MainActivity is `singleTop` + `onNewIntent`),
   deduped per room for 3h (same rule as the server). The Calls screen polls
   `GET /api/calls/live` every 6s while visible and shows a "Live now" card while the
   receptionist is on a call. The server's finished-call log (`GET /api/phone/calls`) is
   merged into the app's log — it is the ONLY record of receptionist-answered calls
   (the server writes them to its own store, not Firestore), which is why they never
   appeared. The two sources are disjoint (the app's dialer logs outbound calls to
   Firestore only), so no dedupe is needed. If the server can't be reached, the screen
   says so honestly — never a fake live call or log entry.
2. **Call log visible on the Calls screen.** The "Full Call & Receptionist Log" dialog
   and its button are gone. The merged log (newest first, caller, status, time, duration,
   receptionist summary when recorded) sits directly on the Calls screen under a "Call
   Log" header with the receptionist status line (Active 24/7) and totals; the metrics
   card counts the merged list.
3. **Richer customer profile.** The detail is a full screen now (not a cramped dialog):
   editable phone/email/address, a notes field, owner-defined custom fields (label +
   value, add/remove, stored as `customFields` on the customer doc), and the customer's
   jobs/history when the data exists (real empty state otherwise). Call/text/email
   actions stay. The follow-up button is a full-width two-line button ("AI Draft
   Follow-Up" / "Queues in Approvals for your review") — the old label can no longer
   clip.
4. **AI chat refresh + delete threads.** A refresh button in the Assistant header
   re-subscribes the chat listener (also bumped after every send, so a just-sent message
   shows immediately), and a delete button asks first, then removes that employee's
   messages from Firestore (`RcosRepository.deleteChatThread`, batch delete, owner-only
   per the rules). Never claims a message was sent until the server confirms it.
5. **Business profile, properly.** Industry picker (all 13 presets from
   `RcosDefaults.INDUSTRY_PRESETS` — picking one resets services to that trade's
   defaults), a service catalog editor (name + price per service, add/remove, stored as
   `serviceCatalog` on the business doc; `services` stays in sync with the catalog names
   so job options follow it), and a fixed "Teach Your Team" text: the stored generic
   SaaS copy is replaced on open with a description built from THIS business's own
   name/industry/area/hours/services, with a "Reset to my business details" button; a
   hand-written description is never overwritten. Company Name, Service Area/City,
   Operating Hours stay editable.
6. **Team chat: add members.** The thread shows the team roster and an "+ Add Member"
   button (name + email/phone). The invite joins the business roster with no UID
   (`members` + `memberEmails`); the server re-sync tolerates a blank userId. Owner-only.
7. **Personal profile.** Account → My Profile: an editable name/phone/email, stored
   per-user on `/users/{uid}` only — never on the business record, so a member can never
   overwrite company contact info by saving their own details (the Oct 25 bug).
8. **App Check failure surfacing.** The shared `AppCheckSetupCard` (extracted from the
   PR #9 onboarding card — same token + Copy button + console steps) is now shown
   everywhere an App Check-protected AI call can fail: follow-up drafts, quote drafts,
   and direct Gemini chat set a general notice and the shell shows the card in a dialog
   instead of the raw grey banner. The Firebase console registration is still a
   separate one-time step (not code).

- Rules: `serviceCatalog` added to the business update allowlist (+ `isValidBusiness`
  shape check); `customFields` added to the customer update allowlist (+
  `isValidCustomer` shape check). **After merge, publish the rules from main (same
  Rules API flow as PR #8/#9) or the catalog/custom-field saves will be denied.**
  `firestore.test.js` covers both new fields (owner can save them; a ghost field on a
  customer update is still rejected).
- Verification: static review only (no JDK/Android SDK on this VM, no Gradle build) —
  brace/paren balance checked on every touched file, call sites grepped, duplicate
  declarations checked, rules tests added but not run (emulator needs Java). CI is the
  only build proof.

### Phone test (all-eight fixes, after merge + install of the main APK)
1. 470-number call: from another phone, call +1 470 596 9505 → the phone wakes with the
   alert ("Receptionist answered a call") → tap it → the Calls tab opens → while the
   receptionist is talking, a "Live now" card shows the caller → after hangup, the call
   appears in the call log with the receptionist's summary. (Locked-phone test is the
   proof for the alert.)
2. Calls → the log is on the screen (no dialog) with the receptionist status line and
   totals; a receptionist call from step 1 is in it.
3. Customers → open a customer → edit phone/notes, add a custom field (e.g. Gate code /
   0420) → Save → reopen → the field stuck; the follow-up button label is not clipped;
   the customer's jobs show if any exist.
4. AI → send a message → it appears without tapping anything; tap the refresh icon →
   the thread reloads; tap the delete icon → confirm → the conversation is gone.
5. Account → Business Profile → Change the industry (e.g. to Cleaning) → services reset
   to that trade's defaults → add a service with a price → Save → start a job → the job
   options follow the new services; the "Teach Your Team" text describes RC Solutions,
   not a generic SaaS product.
6. Account → Integrations & Team Chat → the roster shows the team → + Add Member → add
   a name + email → they appear in the roster.
7. Account → My Profile → edit name/phone/email → Save → the business's company contact
   info is untouched (check Business Profile — its contact fields are unchanged).
8. Trigger an AI call that used to show the raw App Check banner (e.g. an AI follow-up
   draft on a customer) → the setup card with the debug token shows instead; copy the
   token into the Firebase console once, retry, and the banner is gone for good.

## Phase 1 audit fixes — findings 1, 2, 3, 5 + release hardening (Nov 27)

Source: the 8-page audit report on the 1.2.1 Debug APK
(`RCOS_Mobile_1.2.1_Debug_APK___Audit_Report_0_pvxk.pdf`). The 4 High
findings were verified against this exact code before fixing; findings
4, 6–15 are NOT in this PR (Phase 2/3 in the report).

- Finding 1 (roster corruption, High) — `toBusinessSyncRequest`
  built the "owner" entry from the CURRENT user, so a non-owner member
  got an owner entry with the real owner's uid but the member's own
  email/name, then POSTed it. Fixed twice: `syncBusinessToServer`
  now re-registers ONLY when the current user is the owner (a member
  skips silently; an ownerless record still syncs, preserving the
  adopt repair), and the mapper itself only synthesizes the owner
  entry when the current user is the owner.
- Finding 2 (stale ViewModel after sign-out, High) — the MainViewModel
  is `viewModel(key = uid)` in the Activity's store and survived
  sign-out, so signing back in as the same user reused stale state
  and listeners. Fixed: the sign-out path now clears the Activity's
  `viewModelStore` in `onSignOutComplete` (after Firebase sign-out),
  so the next sign-in builds a fresh ViewModel. Normal (non-sign-out)
  flow untouched.
- Finding 3 (App Check missing in push-started processes, High) —
  `configureAppCheck` ran only in `MainActivity.onCreate`, so a
  process started by FCM never installed it. Fixed: new
  `RcosApplication : Application` installs App Check in `onCreate`
  (registered as `android:name` on `<application>`). The MainActivity
  call stays — it writes the debug token from the launch-intent extra
  into the SDK's local debug store first; in a push-started process
  there is no intent, so the debug factory reads the secret already
  stored by a previous Activity launch.
- Finding 5 (no token refresh on 401, High) — `idToken()` used
  `getIdToken(false)` and `request()` never retried. Fixed: on a 401
  response the request retries ONCE with a freshly minted token
  (`getIdToken(true)`); a second 401 is returned honestly. Only
  authenticated calls with a token retry; public calls and the
  "not signed in" early returns do not.
- Release build type — `release` in `app/build.gradle.kts` already
  had `minifyEnabled` + `shrinkResources` + the proguard files; this
  PR adds explicit `isDebuggable = false` and extends
  `proguard-rules.pro` (`-keepattributes Signature` and a keep for
  `com.rcsolutions.rcosmobile.data.**`, whose @Serializable response
  types are decoded with reified `decodeFromString<T>`). No release
  signing config is added — CI builds debug only.
- Manifest hardening — `allowBackup="false"` (was true). New
  `res/xml/network_security_config.xml` blocks cleartext app-wide.
  No certificate pinning: Railway serves Let's Encrypt certificates
  that rotate, so a leaf pin would break the app on rotation.
- Unused permissions/services — `FOREGROUND_SERVICE_MEDIA_PROJECTION`
  and LiveKit's `ScreenCaptureService`
  (`io.livekit.android.room.track.screencapture.ScreenCaptureService`)
  arrive merged from the livekit-android v2.30.0 library manifest and
  exist only for screen sharing, which this app never uses (verified:
  no `setScreenShareEnabled` call anywhere; confirmed the exact
  names against the library's own manifest). Removed with
  `tools:node="remove"`. CAMERA also arrives from LiveKit but is
  KEPT — the AI Assistant's camera pill uses `TakePicturePreview`,
  so the audit's "unused" claim does not hold for this app.
  `FOREGROUND_SERVICE` (also from LiveKit) is left in place — a
  normal permission, and removing it would risk any LiveKit
  foreground-service path.
- Hide outbound calling until configured — the dialpad, the bottom
  "Make a Call" button, and the empty-state "+ Make a Call" action
  are hidden when the outbound-status check reports the trunk
  unconfigured, instead of showing the Vonage setup error to end
  users. Unknown status (never fetched / fetch failed) fails OPEN:
  the dialer stays visible. Ryan's trunk IS configured, so the
  dialer stays visible for him. The call-back rows are NOT gated —
  they open the phone's own dialer (ACTION_DIAL), which does not use
  the trunk. The ViewModel's dial-time status check stays as a
  backstop.

### Console steps owed after merge (not code — do NOT attempt in a PR)

1. Firebase console → Project Settings → General → the Android app
   (`com.rcsolutions.rcosmobile`) → restrict the API key by Android
   app (package + SHA-1).
2. Firebase console → Project Settings → App Check → enforce App
   Check on Firestore and on Firebase AI Logic (currently Monitoring /
   Basic Enforced respectively — confirm the enforcement state before
   flipping anything, and register the debug token first or the debug
   builds will be rejected).

### Phone test (Phase 1, after merge + install of the main APK)

1. Sign out → sign back in as the same user → the app shows fresh
   state (no stale jobs/customers/chat from before sign-out); the
   business, customers, and call log load normally.
2. Lock the phone → call +1 470 596 9505 from another phone → the
   alert still wakes the phone (proves the Application App Check
   install did not break push).
3. Calls → the dialpad and "Make a Call" are visible and the dialer
   still works for Ryan (trunk configured); dial his own number →
   in-app call screen, other phone rings showing +1 747 313 5541.
4. Release build type exists in `app/build.gradle.kts` but is NOT
   CI-tested (CI builds debug only) — do not ship a release APK
   from this PR without a phone test of a release build first.

## Phase 2 audit fixes (client) — findings 4, 6, 7, 8, 9, 10 + dialpad collapse + two UI fixes (Nov 27)

Client half of the audit report's Phase 2. The backend half (Firestore
roles, server-side money enforcement, durable server storage, own
LiveKit token endpoint, call duration cap) is a separate batch in the
server repo and is NOT in this PR. Phase 1 (findings 1, 2, 3, 5 +
release hardening) is already on main — not redone here.

- Finding 4 — release App Check site key fails loudly now
  (`security/AppCheckInit.kt`, `ui/MainViewModel.kt`). The report's
  question, answered: **does `RecaptchaAppCheckProviderFactory`
  find the site key itself? Yes — in the pinned SDK version it reads
  it from `FirebaseOptions` (the google-services.json value, via the
  google-services plugin), NOT from the app's `recaptcha_site_key`
  string resource and NOT from any argument the app passes.** Evidence:
  Firebase PR #8457 (July 2026, landed in firebase-appcheck-recaptcha
  19.2.0) — "Removed `getRecaptchaSiteKey` from `FirebaseOptions` and
  deprecated `RecaptchaAppCheckProviderFactory.getInstance()` methods.
  Site keys must now be provided explicitly via
  `RecaptchaAppCheckProviderFactory.getInstance(String)`" — and "While
  the API existed, the support from the backend didn't." The 19.2.0
  release notes say the same ("Initialization now requires the
  reCAPTCHA site key to be passed as a parameter"), and the current
  Firebase Android API reference shows only `getInstance(String
  siteKey)` — no no-arg overload. The google-services plugin 4.5.0
  added processing of reCAPTCHA site keys in google-services.json;
  4.6.0 removed that support. The app pins
  `firebase-appcheck-recaptcha:19.0.0` (libs.versions.toml:44), so on
  this version the no-arg factory's only source is FirebaseOptions.
  What changed: the release branch now checks
  `FirebaseApp.getInstance().options.recaptchaSiteKey` — the key the
  factory actually uses. If it is missing/blank, the factory is NOT
  installed keyless; the failure is logged loudly and recorded in
  `releaseAppCheckKeyMissing`, which MainViewModel surfaces once at
  startup as a banner ("AI features are unavailable in this build —
  the release App Check key is missing. This needs a fixed build.")
  instead of letting every enforced call fail with no visible reason.
  If the key is present, the install is exactly as before. The old
  resource read is gone — the `recaptcha_site_key` string still sits
  in `firebase_applet_config.xml` but the SDK never reads it, so a
  present resource no longer proves anything. Verified: static review
  only — no Gradle build on this machine, and the release install path
  itself (valid key → factory installed) is unchanged code. Follow-up
  NOT done here: bump `firebase-appcheck-recaptcha` to 19.2.0+ and
  pass the key explicitly via `getInstance(siteKey)`. That is the
  real fix (it stops depending on the FirebaseOptions path whose
  backend support never worked), but the String overload's existence
  in the pinned 19.0.0 could not be verified without a build, and a
  version bump without a compile check was not worth the risk — do
  it with a build in hand.
- Finding 6 — chat timeout (`data/ServerApiClient.kt`). The `/api/chat`
  call now passes `timeoutMsOverride = 60_000` (same as document
  uploads). The 15 s default is unchanged for every other call, and
  the dial (30 s) and documents (60 s) overrides are untouched.
- Finding 7 — upload memory (`data/ServerApiClient.kt`). `executeRequest`
  now calls `setFixedLengthStreamingMode(bodyBytes.size)` before
  writing the body, so HttpURLConnection streams the request instead
  of buffering the whole body in memory first. Same endpoint, same
  JSON fields, same auth, same response handling. Honest limit: the
  base64 string and the JSON string still exist in memory from the
  caller — this removes the connection's second full copy, not the
  caller's. A fully streaming base64 encoder is a bigger change and
  is not in this PR.
- Finding 8 — deep link re-fires on rotation (`MainActivity.kt`).
  `intent.data` is cleared once consumed in BOTH places it is read:
  `onCreate` (the audit's site) and `onNewIntent`. The onNewIntent
  clear matters: without it, a deep link that arrived while the app
  was running stays in the stored intent, and the next rotation
  re-reads it in onCreate and sends the user back to Calls. A fresh
  deep link still works — cold start (launch intent) and while running
  (new intent via onNewIntent, singleTop). One narrow race remains:
  if the activity is recreated in the milliseconds between a deep
  link arriving and the shell consuming it, the link is lost (the
  value lives in the old activity instance) — far better than
  re-firing on every rotation.
- Finding 9 — NavController above the early returns (`MainActivity.kt`).
  `rememberNavController()` moved above the loading / error /
  onboarding early returns in `AuthenticatedAuroraShell`, so
  switching business or retrying no longer drops the back stack and
  open dialogs. No navigation behavior changed otherwise.
- Finding 10 — blocking HTTP ignores cancellation
  (`data/ServerApiClient.kt`). The blocking I/O in `executeRequest`
  now runs in `runInterruptible`, `CancellationException` is rethrown
  explicitly (never swallowed as a network error), and the IOException
  / Exception catches call `ensureActive()` first — a cancelled
  blocking read surfaces as an InterruptedIOException once the thread
  is interrupted, and is rethrown as a cancellation instead of a
  network error. No OkHttp, no new dependency. The 6 s call polling
  stops promptly when the caller leaves Calls.
- Dialpad collapsed by default (Ryan, Nov 27 — "the dialer is in the
  way of everything"). `ui/screens/CallsScreen.kt`: the full numeric
  DialpadCard no longer renders permanently below the call log. A
  compact "Dialpad" affordance (keypad icon + label, the screen's
  secondary-button style) sits in its place; tapping it expands the
  DialpadCard inline, and the same affordance ("Hide dialpad") closes
  it again. Keys, backspace, and the call button work exactly as
  before when open. The `outboundDialingAvailable` gate is unchanged
  — no affordance at all when the trunk reports unconfigured. "Make a
  Call", the log, the metrics card, the live voice card, and the Dial
  & Log dialog are untouched. State is per-screen (`remember` only);
  leaving the screen resets it to hidden. Supporting change:
  `AuroraSecondaryCta` gained an optional `icon: ImageVector? = null`
  parameter (same pattern as AuroraPrimaryCta); existing call sites
  pass nothing and render unchanged.
- UI fix 1 — content width cap (`MainActivity.kt`). The NavHost
  modifier was `.fillMaxSize().widthIn(max = 760.dp)` — fillMaxSize
  took the full incoming constraints, so the 760 dp cap never applied
  on wide screens. Reordered to `.widthIn(max = 760.dp).fillMaxSize()`
  (widthIn first narrows the constraints, then fillMaxSize fills
  within them). The 760 dp value is unchanged. No other
  `.fillMaxSize().widthIn(...)` chain exists in the file (the only
  other widthIn is a standalone `widthIn(max = 420.dp)` on a card —
  not the same bug).
- UI fix 2 — App Check setup card in release builds
  (`ui/components/AppCheckSetupCard.kt`). The card shows the build's
  debug token for the Firebase console; release builds use the
  reCAPTCHA provider and have no debug token, so the card must never
  render there. Gated inside the shared component
  (`if (!BuildConfig.DEBUG) return`) — that covers every call site
  (MainActivity's dialog and the onboarding screen) in one place.
  Debug behavior is unchanged. Known edge: in release, an App Check
  rejection still opens MainActivity's dialog, which now renders
  empty (dismissable, no token shown) — a blank dialog is better
  than a setup step that cannot be done, and Finding 4's banner
  covers the missing-key case specifically.

Phone tests (debug build, unless noted):
1. AI Assistant → send a message that takes a while → it answers
   instead of failing at 15 s (finding 6).
2. AI Assistant → upload a large document (several MB) → it uploads
   and answers (finding 7).
3. Tap a push notification / deep link to Calls → rotate the phone →
   you stay where you were, not bounced back to Calls (finding 8).
4. Calls → tap a notification to open the app, then switch business
   in the switcher → back stack and open dialogs survive (finding 9).
5. Calls → leave the screen while the live-call poll is running →
   polling stops (no network activity after leaving) (finding 10).
6. Calls → the log fills the screen, no keypad visible; tap Dialpad
   → keypad appears, keys work, call button opens the dialog
   prefilled; tap "Hide dialpad" → it hides; leave and come back →
   hidden again (dialpad collapse).
7. On a wide screen / tablet → content caps at 760 dp wide, centered
   (UI fix 1).
8. Release build ONLY: open any screen that would show the App Check
   setup card → it never appears (UI fix 2). Not provable on the
   debug build — mark as release-only. If a release build is ever
   built with the site key missing from google-services.json, the
   banner "AI features are unavailable in this build — the release
   App Check key is missing." appears once at startup (finding 4).

## Industry presets, email login, invite codes (Nov 27)

Three items from the owner's brief, one PR + one build. No visual
redesign — same screens, same design, the missing pieces filled in.

### 1. All 40 industry presets
The new app shipped with 13 industry presets; the old app has 40.
The 27 missing ones are ported from the old app's `presets.ts` into
`RcosDefaults.kt` (`INDUSTRY_PRESETS`), same shape (id, name,
description, default services, tone, technician role, suggested
employees, first job pack). That includes `it_services` — "IT Services
& Managed Technology (MSP)" — the owner's own industry, which the
old app had and the new one didn't. The 13 existing presets are
untouched, and the picker UI is unchanged — only the list grew. The
onboarding document upload + AI industry detection (already in the
PR #15 build) can now actually pick the owner's industry: before
this, the AI had no IT preset to choose from.

### 2. Email + password login and reset password
The login page was Google-only. It now also takes email + password:
sign in, create an account, and "Forgot password?" (sends a reset
email). Same Firebase Auth project, so a brand-new email account
lands on the same onboarding/setup flow Google users get, and the
server gate (Firebase ID token) works for any provider — no server
change. Error text is plain words only, never a Firebase code:
wrong password, no account with that email, email already in use,
weak password, invalid email. The reset confirmation is always the
same ("Check your email for the reset link") whether or not an
account exists for that email. Signing up sends a verification
email (best-effort — a failed send never fails the sign-up). There
is no sign-in gate: an unverified user can still sign in and see
their own businesses. But the workspace list's `memberEmails` query
is skipped until the email is verified (the rules' `callerEmail()`
is '' for unverified users, so the query would be denied and poison
the whole list — see `RcosRepository.observeBusinesses`), and joining
a company with an invite code needs a verified email: the join
dialog says so in plain words and offers "Resend verification
email", and the join itself reloads the user first so a link tapped
just now in the inbox is picked up. The Google button is exactly as
it was.

### 3. Invite codes for employees
The server already had the full invite flow (create / join / revoke);
the app only had the roster "Add Member" (name + email, no code).
Client-only wiring now:
- Owner side (Account → Team Chat → "Invite by Code", owner-only):
  generate a code (optionally locked to one email), see it big with
  a Copy button, list pending invites with Revoke.
- Employee side (setup screen → "Join a company with an invite
  code"): enter the code + name + email → the server adds them to
  the roster → the app re-reads workspaces and the joined business
  appears in the switcher, closing setup on its own. A wrong or
  expired code shows the server's plain-word error.
- The existing "Add Member" (name + email, blank UID, shows
  "Invited") is untouched — the code flow is separate.

How the join becomes visible (read this before testing): the
server's join only adds a roster entry — it never records the
member's Firebase UID — so the `memberUids` query cannot surface a
joined business. Two client-side pieces close that gap: when the
owner creates an invite for an email, the app also records that
email in the business's `memberEmails` map (owner-only update, same
as Add Member), and the business list has a third query for
`memberEmails.<email> == "member"`. That third query is only issued
for a verified email: for an unverified user the rules' `callerEmail()`
is '', the query would be denied, and the denial would poison the
whole list — so it is skipped and the invited list is simply empty
until they verify. Joining itself also needs a verified email (the
join reloads the user first, then refuses to post while unverified).
An open invite (no email) needs the owner to add the member's email
separately (Add Member) before the business appears for them.
Consequence to know: an invitee whose email the owner recorded can
see the business shell in their switcher before using the code — but
with empty lists (jobs, customers, calls all key off `memberUids`)
and server features gated until they join. The clean fix is a small
server change on `POST /api/team/join` (record `memberEmails[email]`
— and the caller's UID in `memberUids` — from the verified token);
then the client-side recording at invite creation can go away. That
server change is a future improvement, not part of this batch.

Phone tests (debug build):
1. Onboarding → industry picker lists 40 trades, including "IT
   Services & Managed Technology (MSP)"; picking it resets services
   to its defaults.
2. Onboarding → upload a company document for an IT business →
   the AI picks the IT industry and fills the sections.
3. Login → sign in with an email + password that has no account →
   plain "No account with that email. Create one below."; create
   the account → a verification email is sent → lands on setup like
   a Google user.
3a. Verification (email sign-up): do NOT verify, create a company
    of your own → your own businesses still load, and there is no
    "Failed to load business workspaces" error. Then verify via the
    emailed link, join a company with a code → the company appears.
    In the join dialog while unverified, the plain-words message
    shows with "Resend verification email" → a new link arrives.
4. Login → wrong password → plain "Wrong password..."; "Forgot
   password?" → "Check your email for the reset link." (same message
   for an email with no account).
5. Owner: Account → Team Chat → "Invite by Code" → make a code for
   a real email → code shows + Copy works → pending list shows it →
   Revoke removes it. Non-owners don't see the button.
6. Employee (second account, that email): setup → "Join a company
   with an invite code" → enter the code → the business appears in
   the switcher and setup closes. Wrong code → plain error.
