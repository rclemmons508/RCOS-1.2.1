package com.rcsolutions.rcosmobile.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.util.CustomClassMapper
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

// Legacy docs written by the old React app store createdAt/updatedAt as
// ISO-8601 STRINGS (e.g. "2026-11-17T16:15:42.517Z"), while the 1.2.1
// models declare them as @ServerTimestamp Timestamp?. Firestore's mapper
// throws on String -> Timestamp, which broke workspace sync entirely
// ("Could not deserialize object ... found in field 'createdAt'").
// These are the ONLY Timestamp-typed fields any model declares, so
// normalizing just these two top-level keys covers every model class.
// BusinessMember has no timestamp field, so members[].joinedAt (a string
// on live docs) is still ignored via @IgnoreExtraProperties, as before.
//
// NOTE: this parses ISO-8601 by hand instead of java.time.Instant.
// minSdk is 24 and the build has no core-library desugaring, so
// Instant.parse would compile but crash on API 24/25 at runtime. The
// regex + Calendar parser below is API-1 safe and handles the shapes
// the old app writes (optional fraction, Z or numeric offset).
private val LEGACY_TIMESTAMP_FIELDS = setOf("createdAt", "updatedAt")

private val ISO_8601_REGEX =
    Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$""")

/** Parses an ISO-8601 string to a [Timestamp]; null when it does not parse. */
fun parseIso8601ToTimestamp(text: String): Timestamp? {
    val match = ISO_8601_REGEX.matchEntire(text.trim()) ?: return null
    val (year, month, day, hour, minute, second, fraction, zone) = match.destructured
    return try {
        val millis = fraction.padEnd(3, '0').take(3).toInt()
        val offsetMillis = when {
            zone.isEmpty() || zone == "Z" -> 0L
            else -> {
                val sign = if (zone.startsWith("-")) -1L else 1L
                val digits = zone.drop(1).replace(":", "")
                sign * ((digits.substring(0, 2).toLong() * 60) + digits.substring(2, 4).toLong()) * 60_000L
            }
        }
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            set(
                year.toInt(), month.toInt() - 1, day.toInt(),
                hour.toInt(), minute.toInt(), second.toInt()
            )
            set(Calendar.MILLISECOND, millis)
        }
        Timestamp(Date(calendar.timeInMillis - offsetMillis))
    } catch (e: Exception) {
        null
    }
}

/**
 * Converts one raw Firestore value to a [Timestamp], tolerating the shapes
 * legacy docs actually carry. Returns null — the field is nullable in
 * every model — instead of throwing, so one odd value can never break a
 * whole sync.
 */
fun parseLegacyTimestamp(raw: Any?): Timestamp? {
    return when (raw) {
        null -> null
        is Timestamp -> raw
        is Number -> Timestamp(Date(raw.toLong()))
        is String -> {
            val text = raw.trim()
            if (text.isEmpty()) null else parseIso8601ToTimestamp(text)
        }
        else -> null
    }
}

/**
 * Same deserialization as DocumentSnapshot.toObject(..., ESTIMATE), but
 * first rewrites ISO-string createdAt/updatedAt values to real Timestamps.
 * Reads only — nothing here writes to Firestore, and writes elsewhere in
 * the repo still use FieldValue.serverTimestamp() (the published rules
 * require real timestamps on create).
 */
fun <T> DocumentSnapshot.toModel(clazz: Class<T>): T? {
    val data = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toMutableMap() ?: return null
    for (key in LEGACY_TIMESTAMP_FIELDS) {
        if (data.containsKey(key)) {
            data[key] = parseLegacyTimestamp(data[key])
        }
    }
    return CustomClassMapper.convertToCustomClass(data, clazz, this.reference)
}

/** Query-wide version of [DocumentSnapshot.toModel]. */
fun <T> QuerySnapshot.toModels(clazz: Class<T>): List<T> =
    documents.mapNotNull { it.toModel(clazz) }
