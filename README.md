# RC Solutions (RCOS Mobile)

Native **Android** application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**, using the **Aurora One** dark-emerald design system.

## Architecture & Tech Stack

- **UI & Navigation:** Jetpack Compose (Material 3), `@Serializable` type-safe Navigation Compose, adaptive layouts, and Material Icons Extended.
- **Architecture:** MVVM with `MainViewModel`, `StateFlow` / `collectAsStateWithLifecycle`, and sealed `UiState` wrappers.
- **Authentication:** Google Sign-In via Jetpack Credential Manager (`GetGoogleIdOption` / `GetSignInWithGoogleOption`) backed by Firebase Authentication (`com.google.firebase:firebase-auth`).
- **Cloud Database & Sync:** Firebase Cloud Firestore (`com.google.firebase:firebase-firestore`) using a named database (`firestore_database_id`) and zero-trust owner/member security rules (`firestore.rules`).
- **AI Assistant:** Firebase AI Logic (`Firebase.ai`, `gemini-3.8-flash` and `gemini-3.5-flash-lite`) protected by Firebase App Check (`DebugAppCheckProviderFactory` in debug builds, `RecaptchaAppCheckProviderFactory` in release builds).
- **Local Security & Preferences:** AndroidX Biometric lock prompt and Jetpack DataStore Preferences.

## Screens

1. **Welcome & Google Sign-In:** Branded hero entry and Credential Manager authentication.
2. **Progressive Onboarding:** Step-by-step workspace setup with AI business profile extraction and skip-for-now support.
3. **Home Dashboard:** Weather/status header, focus banner, 3×2 Quick Actions grid (including Customers, Schedule, Quote drafting, and Approvals), and live Recent Activity feed.
4. **AI Assistant:** Unified conversational workspace with prompt chips, approval queue cards, attachment/camera/voice support, and team messaging.
5. **Calls & Voicemails:** AI Receptionist toggle, call KPI counters, filter pills (`All`, `Active`, `Missed`, `Voicemail`), outbound dialer CTA, and full call log.
6. **Tasks:** Filterable field task list (`Today`, `This Week`, `Completed`) with interactive completion toggles and task creation.
7. **Customers & Schedule:** Searchable CRM directory with status filters and customer detail inspection, plus weekly calendar strip and appointment management.
8. **Settings & Account:** Business profile editor, AI receptionist & autonomy controls, notifications, integrations, biometric lock, and sign-out.

## Local Build & Verification

```bash
# Build debug APK
gradle :app:assembleDebug

# Run local Firestore security rules tests (with Firestore Emulator running on 127.0.0.1:8085)
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
