package com.rcsolutions.rcosmobile

import android.app.Application
import com.rcsolutions.rcosmobile.security.configureAppCheck

/**
 * Audit Phase 1 (finding 3) — App Check was installed only in
 * MainActivity.onCreate, so a process started by FCM
 * (RcosFirebaseMessagingService) never installed it and its
 * Firestore / AI calls went out unattested. Installing here covers
 * every process entry point, including push-started ones.
 *
 * The MainActivity call stays: it writes the debug token from the
 * launch-intent extra into the SDK's local debug store before
 * installing the debug factory. Here there is no intent, so the
 * debug factory reads the secret already stored by a previous
 * Activity launch (the SDK generates and stores it on first use);
 * installing the provider factory a second time from the Activity
 * is harmless.
 */
class RcosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        configureAppCheck(this, null)
    }
}
