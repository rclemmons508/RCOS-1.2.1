package com.rcsolutions.rcosmobile.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.rcsolutions.rcosmobile.MainActivity
import com.rcsolutions.rcosmobile.R
import com.rcsolutions.rcosmobile.data.ApiResult
import com.rcsolutions.rcosmobile.data.ServerApiClient
import kotlinx.coroutines.tasks.await

private const val TAG = "PushRegistration"
private const val PREFS = "rcos_push"
private const val KEY_TOKEN = "fcm_token"

/**
 * How long one room's alert stays deduped on this device — the same
 * 3h rule as the old app's server-side dedupe
 * (`server/pushNotifications.ts` NOTIFIED_TTL_MS). A redelivered
 * message for the same call inside the window is dropped silently.
 */
const val NOTIFIED_TTL_MS = 3L * 60 * 60 * 1000

/** The FCM token for this device, or null if Firebase has not minted one yet. */
suspend fun currentFcmToken(): String? = try {
    FirebaseMessaging.getInstance().token.await()
} catch (e: Exception) {
    Log.w(TAG, "Could not fetch FCM token", e)
    null
}

fun storeFcmToken(context: Context, token: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_TOKEN, token).apply()
}

fun storedFcmToken(context: Context): String? =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

/**
 * Register this device's FCM token for the active business so the old
 * Railway server's `notifyCallAnswered` sends the
 * receptionist-answered alert to THIS app. Re-sent on every launch
 * and every foregrounding even when the token is unchanged (same as
 * the old app) — a server redeploy wipes the token store, and a stale
 * registration is the silent way the alert stops arriving.
 *
 * Failures are logged only: push registration must never surface as a
 * user-facing error, and it must never block the rest of the launch.
 */
suspend fun registerPushTokenForBusiness(
    context: Context,
    serverApi: ServerApiClient,
    businessId: String,
) {
    val token = currentFcmToken() ?: storedFcmToken(context) ?: run {
        Log.w(TAG, "No FCM token available yet; will retry on next launch")
        return
    }
    storeFcmToken(context, token)
    when (val result = serverApi.registerPushToken(businessId, token)) {
        is ApiResult.Success -> Log.d(TAG, "FCM token registered for business $businessId")
        is ApiResult.HttpError -> Log.w(TAG, "FCM token registration failed (${result.status}): ${result.message}")
        is ApiResult.NetworkError -> Log.w(TAG, "FCM token registration failed: ${result.message}")
    }
}

// ---- Local per-call dedupe (roomName -> epoch ms, pruned on read) ----

private fun notifiedRooms(context: Context): MutableMap<String, Long> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString("notified_rooms", "") ?: ""
    val now = System.currentTimeMillis()
    val out = mutableMapOf<String, Long>()
    for (pair in raw.split(';')) {
        val parts = pair.split(':')
        if (parts.size != 2) continue
        val ts = parts[1].toLongOrNull() ?: continue
        if (now - ts < NOTIFIED_TTL_MS) out[parts[0]] = ts
    }
    return out
}

private fun markRoomNotified(context: Context, roomName: String) {
    val rooms = notifiedRooms(context)
    rooms[roomName] = System.currentTimeMillis()
    val raw = rooms.entries.joinToString(";") { "${it.key}:${it.value}" }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString("notified_rooms", raw).apply()
}

fun alreadyNotifiedForRoom(context: Context, roomName: String): Boolean =
    notifiedRooms(context).containsKey(roomName)

/**
 * Show the call alert: high priority, lock-screen visible
 * (VISIBILITY_PUBLIC, same as the channel), tap opens the Calls tab
 * via the `rcosmobile://calls` deep link. A room already alerted
 * inside the 3h window is dropped silently. Callers should have
 * already created the channel via [ensureCallAlertsChannel]; this is
 * defensive — posting to a missing channel is a no-op on Android 8+.
 */
fun showLiveCallNotification(
    context: Context,
    roomName: String,
    title: String?,
    body: String?,
) {
    if (roomName.isBlank()) return
    if (alreadyNotifiedForRoom(context, roomName)) {
        Log.d(TAG, "Skipping duplicate alert for room $roomName")
        return
    }
    ensureCallAlertsChannel(context)
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("rcosmobile://calls"),
        context,
        MainActivity::class.java,
    ).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pending = PendingIntent.getActivity(
        context,
        roomName.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, CALL_ALERTS_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title ?: "Receptionist answered a call")
        .setContentText(body ?: "The receptionist is talking to a caller. Tap to see the call.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setAutoCancel(true)
        .setContentIntent(pending)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(roomName.hashCode(), notification)
    markRoomNotified(context, roomName)
    Log.d(TAG, "Call alert shown for room $roomName")
}
