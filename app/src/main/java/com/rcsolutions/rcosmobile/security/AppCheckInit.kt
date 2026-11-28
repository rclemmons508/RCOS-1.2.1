package com.rcsolutions.rcosmobile.security

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.rcsolutions.rcosmobile.BuildConfig

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

/**
 * Set when the release reCAPTCHA site key is missing from
 * FirebaseOptions (see the release branch of [configureAppCheck]).
 * Read by MainViewModel at startup so the app surfaces a visible
 * in-app message instead of failing every enforced call silently.
 * Release-only: debug builds use the debug provider and never set it.
 */
@Volatile
var releaseAppCheckKeyMissing: Boolean = false
    private set

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit ensures secret exists before provider factory initializes.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // Audit Phase 2 (finding 4) — in firebase-appcheck-recaptcha
            // 19.0.0 (pinned in libs.versions.toml) the no-arg factory
            // finds the site key itself, from FirebaseOptions (the
            // google-services.json value, via the google-services
            // plugin) — NOT from the recaptcha_site_key string
            // resource and NOT from any argument the app passes.
            // Evidence: Firebase PR #8457 removed getRecaptchaSiteKey
            // from FirebaseOptions in 19.2.0 and now requires the key
            // passed explicitly via getInstance(String); the 19.2.0
            // release notes say the same, and the current API
            // reference shows only getInstance(String). The explicit
            // call is NOT used here: the app pins 19.0.0, where the
            // String overload's existence could not be verified
            // without a build — see the README for the 19.2.0
            // follow-up. A missing/blank key used to fail silently
            // (Log.e only) while every enforced call then failed with
            // no visible reason. Now it is recorded in
            // releaseAppCheckKeyMissing so the app surfaces it, and
            // the factory is not installed keyless.
            val optionsKey = runCatching {
                FirebaseApp.getInstance().options.recaptchaSiteKey
            }.getOrNull()?.takeIf { it.isNotBlank() }
            if (optionsKey.isNullOrBlank()) {
                Log.e(
                    TAG,
                    "Release App Check site key is missing from FirebaseOptions " +
                        "(google-services.json) — the reCAPTCHA provider cannot be " +
                        "installed and every enforced Firebase call will fail. " +
                        "Register the site key in the Firebase console and re-download google-services.json."
                )
                releaseAppCheckKeyMissing = true
            } else {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

/**
 * The debug secret this build's App Check tokens are signed with.
 * Debug builds only. The Firebase console must have this exact value
 * registered (Project Settings → App Check → the Android app →
 * Manage debug tokens → Add debug token) or Firebase AI Logic, which
 * is Basic Enforced, rejects every token with "Firebase App Check
 * token is invalid." The SDK generates the secret on first use and
 * stores it here, so it only exists after an AI call has been
 * attempted once — null before that.
 *
 * The secret is shown in-app (debug builds only, to the signed-in
 * owner) so it can be copied into the console without adb/Logcat. It
 * is a debug-only token: it grants App Check tokens for this debug
 * build alone and is never a production credential. It is never
 * written to Firestore, never sent to the server, and never committed.
 */
fun readDebugSecret(context: Context): String? {
    if (!BuildConfig.DEBUG) return null
    return runCatching {
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        ).getString(DEBUG_SECRET_KEY, null)?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
