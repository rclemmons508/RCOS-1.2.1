# RC Solutions (RCOS Mobile)

Native **Android** application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**, using the **Aurora One** dark-emerald design system.

## Architecture & Tech Stack

- **UI & Navigation:** Jetpack Compose (Material 3), `@Serializable` type-safe Navigation Compose, adaptive layouts, and Material Icons Extended.
- **Architecture:** MVVM with `MainViewModel`, `StateFlow` / `collectAsStateWithLifecycle`, and sealed `UiState` wrappers.
- **Authentication:** Google Sign-In via Jetpack Credential Manager (`GetGoogleIdOption` / `GetSignInWithGoogleOption`) backed by Firebase Authentication (`com.google.firebase:firebase-auth`). Biometric lock is disabled.
- **Cloud Database & Sync:** Firebase Cloud Firestore (`com.google.firebase:firebase-firestore`) targeting the verified enterprise database (`ai-studio-rcosmobileapp-c0c46f2b-8d0f-43b8-978d-70fb08614967`, `firestore_database_id`) on Firebase project `rcos-mobile`, with zero-trust owner/member security rules (`firestore.rules`).
- **AI Assistant:** Firebase AI Logic (`Firebase.ai`, `gemini-3.8-flash` and `gemini-3.5-flash-lite`; Gemini Live audio via `gemini-3.8-live`) protected by Firebase App Check (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Local Preferences:** Jetpack DataStore Preferences (`rcos_preferences`).

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
