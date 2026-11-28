package com.rcsolutions.rcosmobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.rcsolutions.rcosmobile.BuildConfig
import com.rcsolutions.rcosmobile.ui.screens.auroraFieldColors
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary

/**
 * 8 fixes Nov 26 (item 8) — the App Check setup card, shared by every
 * place an App Check-protected AI call can fail (onboarding extraction,
 * customer follow-up drafts, quote drafts, direct Gemini chat). The
 * raw error ("Firebase App Check token is invalid.") tells the owner
 * nothing actionable, so it is never shown as a banner: instead this
 * card shows the build's debug token with a Copy button and the exact
 * console steps.
 *
 * The Firebase console registration itself is a separate, one-time,
 * non-code step — this card only makes it doable from the phone.
 * The token is shown here so it can be copied into the console
 * without adb or Logcat; it is read at runtime from the SDK's local
 * debug store (debug builds only, null in release) and is never
 * committed to the repo.
 */
@Composable
fun AppCheckSetupCard(
    debugToken: String?,
    onDismiss: () -> Unit,
    copyButtonTestTag: String = "appcheck_copy_token",
    modifier: Modifier = Modifier,
) {
    // Audit Phase 2 (UI fix 2) — the card shows the build's debug
    // token for the Firebase console. Release builds use the
    // reCAPTCHA provider and have no debug token, so the card must
    // never render there. Gating here covers every call site
    // (MainActivity's dialog and the onboarding screen). Debug
    // behavior is unchanged.
    if (!BuildConfig.DEBUG) return
    val clipboard = LocalClipboardManager.current
    AuroraCard(highlighted = true, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = AuroraRed)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AI setup needed — one-time step",
                style = MaterialTheme.typography.titleMedium,
                color = AuroraTextPrimary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Firebase AI is blocked because this build's debug token isn't registered in the Firebase console. Register it once, then try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary
        )
        if (debugToken != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = debugToken,
                onValueChange = {},
                readOnly = true,
                label = { Text("Debug token — copy into the Firebase console") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                AuroraSecondaryCta(
                    text = "Copy token",
                    testTag = copyButtonTestTag,
                    onClick = { clipboard.setText(AnnotatedString(debugToken)) },
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = AuroraTextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "In the Firebase console: Project Settings → App Check → this Android app → Manage debug tokens → Add debug token → paste this value → Save.",
                style = MaterialTheme.typography.bodySmall,
                color = AuroraTextMuted
            )
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The token appears here after an AI call is attempted. Try the AI action once, then come back to this screen to copy it.",
                style = MaterialTheme.typography.bodyMedium,
                color = AuroraTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = AuroraTextSecondary)
            }
        }
    }
}
