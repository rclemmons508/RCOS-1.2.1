package com.rcsolutions.rcosmobile.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

private const val TAG = "RcosFcmService"

/**
 * 8 fixes Nov 26 (item 1) — FCM push alerts for receptionist-answered
 * calls. The old Railway server sends the alert once this device's
 * token is registered (POST /api/push/register, same `pushTokens`
 * shape as the old app); no server change was needed.
 *
 * Delivery: the server's message carries BOTH a notification payload
 * and data (`type: live-call`, `roomName`, `businessId`,
 * `deepLink: rcosmobile://calls`). When the app is backgrounded the
 * system shows the notification payload itself; when foregrounded,
 * [onMessageReceived] fires and the app shows it. Either way exactly
 * one alert appears per call, and the local 3h dedupe drops any
 * redelivery.
 *
 * The token itself is registered by [registerPushTokenForBusiness]
 * (MainViewModel, on launch and on foregrounding) because only the
 * app layer knows the active business — the service just stores a
 * refreshed token so the next launch picks it up.
 */
class RcosFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        storeFcmToken(this, token)
        Log.d(TAG, "FCM token refreshed; will register on next launch")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "live-call") {
            Log.d(TAG, "Ignoring FCM message of type '${data["type"]}'")
            return
        }
        val roomName = data["roomName"].orEmpty()
        if (roomName.isBlank()) {
            Log.w(TAG, "live-call message without a roomName; dropping")
            return
        }
        // The server's notification payload already names the caller
        // ("The receptionist is talking to {caller}. Tap to listen
        // in."); fall back to a plain title/body only if it is absent.
        showLiveCallNotification(
            context = this,
            roomName = roomName,
            title = message.notification?.title,
            body = message.notification?.body,
        )
    }
}
