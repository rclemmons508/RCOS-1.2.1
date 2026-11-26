package com.rcsolutions.rcosmobile.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Call-alert notifications (item 7 of the Nov 26 rework).
 *
 * Diagnosis: the phone's Settings → Apps → RCOS Mobile →
 * Notifications toggle would not stay on because the app never
 * declared POST_NOTIFICATIONS and never created a notification
 * channel — the system had nothing to grant. The manifest now
 * declares the permission, this channel is created on launch, and
 * the Account → Notifications screen offers the runtime request.
 *
 * Channel id matches the old RCOS-MOBILE-APP (`rcos_calls`) so the
 * name and behaviour stay familiar. This file creates the channel
 * only — it sends no notifications and adds no push path. The full
 * push-delivery path (receptionist answers → an alert arrives) needs
 * FCM + a server/webhook this app does not have; that is a separate
 * follow-up, not this fix.
 */
const val CALL_ALERTS_CHANNEL_ID = "rcos_calls"

fun ensureCallAlertsChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(CALL_ALERTS_CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        CALL_ALERTS_CHANNEL_ID,
        "Call Alerts",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Alerts when the AI receptionist answers a call."
        enableVibration(true)
        enableLights(true)
        // Call alerts must be visible on the lock screen.
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    }
    manager.createNotificationChannel(channel)
}

/** True when the system notification permission is currently granted. */
fun areNotificationsEnabled(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return false
    return manager.areNotificationsEnabled()
}

/** Intent that opens this app's notification settings screen, for
 * the case where the runtime request was denied permanently. */
fun appNotificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
