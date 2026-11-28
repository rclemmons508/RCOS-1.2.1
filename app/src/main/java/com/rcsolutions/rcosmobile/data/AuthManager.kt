package com.rcsolutions.rcosmobile.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.rcsolutions.rcosmobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "Auth"

// Login fix (Nov 25): the longest a single Google sign-in attempt may run.
// A hung Credential Manager / Firebase call used to leave the login screen
// stuck with no error at all after the account was picked. The attempt is
// now bounded so a hang always surfaces as an error instead.
private const val SIGN_IN_TIMEOUT_MS = 25_000L

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("Unable to launch Google Sign-In outside an Activity context.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            withTimeout(SIGN_IN_TIMEOUT_MS) {
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type returned by Google Sign-In.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Google Sign-In timed out after ${SIGN_IN_TIMEOUT_MS / 1000} seconds", e)
            onAuthError("Sign-in timed out. Check your connection and try again — if it keeps timing out, the app build may need its sign-in key re-registered (see README).")
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError(signInErrorMessage(e))
        }
    }
}

// Never surface a blank error on the login screen: fall back to the
// exception type so the message is always non-blank and actionable.
private fun signInErrorMessage(e: Exception): String {
    val detail = e.localizedMessage?.takeIf { it.isNotBlank() }
    return if (detail != null) "Google Sign-In failed: $detail"
    else "Google Sign-In failed (${e::class.simpleName ?: "unknown error"}). Please try again."
}

// ---- Email + password auth (Nov 27) ----
// The login page was Google-only. Email/password uses the same Firebase
// Auth project, so the server gate (Firebase ID token) and the
// onboarding flow work exactly as they do for Google users: a brand-new
// email account has no business yet and lands on the same setup flow.
// Sign-up sends a verification email (best-effort, never fails the
// sign-up). There is no sign-in gate — an unverified user can still
// sign in and see their own businesses — but the workspace list's
// memberEmails query is skipped until verified (see
// RcosRepository.observeBusinesses) and joining a company with an
// invite code needs a verified email. Error text is plain
// words only — never a Firebase error code or message.

private fun emailAuthErrorMessage(e: Exception, isSignUp: Boolean): String {
    if (e is FirebaseNetworkException) {
        return "No connection. Check your connection and try again."
    }
    if (e is TimeoutCancellationException) {
        return "Sign-in timed out. Check your connection and try again."
    }
    if (e is FirebaseAuthException) {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "That email doesn't look right."
            "ERROR_WRONG_PASSWORD" -> "Wrong password. Try again, or reset it below."
            "ERROR_USER_NOT_FOUND" ->
                if (isSignUp) "Couldn't create the account. Please try again."
                else "No account with that email. Create one below."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with that email already exists. Sign in instead."
            "ERROR_WEAK_PASSWORD" -> "Choose a stronger password — at least 6 characters."
            "ERROR_USER_DISABLED" -> "That account is disabled. Contact support for help."
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a bit and try again."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email sign-in isn't turned on for this app yet."
            else -> if (isSignUp) "Couldn't create the account. Please try again."
            else "Sign-in failed. Please try again."
        }
    }
    return if (isSignUp) "Couldn't create the account. Please try again."
    else "Sign-in failed. Please try again."
}

fun signInWithEmail(
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            withTimeout(SIGN_IN_TIMEOUT_MS) {
                Firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
                onAuthSuccess()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Email sign-in failed", e)
            onAuthError(emailAuthErrorMessage(e, isSignUp = false))
        }
    }
}

fun signUpWithEmail(
    email: String,
    password: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            withTimeout(SIGN_IN_TIMEOUT_MS) {
                Firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await()
                // Send the verification email (best-effort): the account
                // works without it, but the workspace list's memberEmails
                // query is denied until the email is verified (the rules'
                // callerEmail() is '' for unverified users — see
                // RcosRepository.observeBusinesses), and joining a company
                // with an invite code needs a verified email. A failed
                // send must never fail the sign-up itself, so it is
                // caught here and only logged.
                try {
                    Firebase.auth.currentUser?.sendEmailVerification()?.await()
                } catch (e: Exception) {
                    Log.w(TAG, "Verification email failed to send", e)
                }
                onAuthSuccess()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Email sign-up failed", e)
            onAuthError(emailAuthErrorMessage(e, isSignUp = true))
        }
    }
}

// Forgot password: the confirmation is always the same, whether or
// not an account exists for that email — never reveal which it is.
fun sendPasswordReset(
    email: String,
    onSent: () -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            withTimeout(SIGN_IN_TIMEOUT_MS) {
                Firebase.auth.sendPasswordResetEmail(email.trim()).await()
                onSent()
            }
        } catch (e: Exception) {
            if (e is FirebaseAuthException && e.errorCode == "ERROR_USER_NOT_FOUND") {
                onSent()
            } else {
                Log.w(TAG, "Password reset failed", e)
                onError(
                    when {
                        e is FirebaseNetworkException -> "No connection. Check your connection and try again."
                        e is FirebaseAuthException && e.errorCode == "ERROR_INVALID_EMAIL" ->
                            "That email doesn't look right."
                        else -> "Couldn't send the reset email. Please try again."
                    }
                )
            }
        }
    }
}

// Resend the sign-up verification email. Unlike the send inside
// signUpWithEmail (best-effort, silent), the caller here needs to
// know whether it went out — the join dialog offers this action when
// the user is unverified.
fun resendVerificationEmail(
    onSent: () -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val user = Firebase.auth.currentUser
            if (user == null) {
                onError("Sign in first, then ask for a new verification email.")
                return@launch
            }
            withTimeout(SIGN_IN_TIMEOUT_MS) {
                user.sendEmailVerification().await()
                onSent()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Verification email resend failed", e)
            onError(
                when {
                    e is FirebaseNetworkException -> "No connection. Check your connection and try again."
                    e is TimeoutCancellationException -> "Timed out. Check your connection and try again."
                    else -> "Couldn't send the verification email. Please try again."
                }
            )
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeAuthSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeAuthSignOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}
