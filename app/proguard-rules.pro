-keepattributes *Annotation*
-keepattributes Signature
# Audit Phase 1 — kotlinx.serialization decodes the app's own
# @Serializable types with reified decodeFromString<T>; keep their
# generated serializers reachable. The model package was already
# kept wholesale; the data package holds the other serializable
# response types (OutboundStatus, DialResponse, ChatReply, ...).
-keep class com.rcsolutions.rcosmobile.data.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
-keep @androidx.annotation.Keep class * { *; }
-keep class com.rcsolutions.rcosmobile.model.** { *; }
# NOTE: LiveKit and Firebase ship their own consumer ProGuard rules
# in their AARs; no library-specific keeps are added here. The
# release build type is NOT exercised by CI (debug only) — verify
# the release APK on a phone before shipping it.
