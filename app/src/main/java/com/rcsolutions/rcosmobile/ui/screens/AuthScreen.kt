package com.rcsolutions.rcosmobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.rcsolutions.rcosmobile.R
import com.rcsolutions.rcosmobile.data.onGoogleSignInClicked
import com.rcsolutions.rcosmobile.data.sendPasswordReset
import com.rcsolutions.rcosmobile.data.signInWithEmail
import com.rcsolutions.rcosmobile.data.signUpWithEmail
import com.rcsolutions.rcosmobile.ui.components.RcSolutionsBrandLockup
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardSurface
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraObsidian
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Email + password sign-in (Nov 27) — the page was Google-only.
    // Same Firebase Auth project, so a brand-new email account lands
    // on the same onboarding/setup flow Google users get.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    fun emailLooksValid(value: String): Boolean {
        val at = value.indexOf('@')
        return at > 0 && value.indexOf('.', at) > at + 1
    }

    fun submitEmailAuth() {
        errorMessage = null
        infoMessage = null
        val cleanEmail = email.trim()
        when {
            cleanEmail.isBlank() || password.isBlank() -> {
                errorMessage = "Enter your email and password."
                return
            }
            !emailLooksValid(cleanEmail) -> {
                errorMessage = "That email doesn't look right."
                return
            }
            isSignUp && password.length < 6 -> {
                errorMessage = "Choose a password with at least 6 characters."
                return
            }
        }
        isLoading = true
        val onError: (String) -> Unit = { msg ->
            isLoading = false
            errorMessage = msg
        }
        val onSuccess: () -> Unit = {
            isLoading = false
            onAuthSuccess()
        }
        if (isSignUp) {
            signUpWithEmail(cleanEmail, password, onSuccess, onError, scope)
        } else {
            signInWithEmail(cleanEmail, password, onSuccess, onError, scope)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraObsidian)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_welcome_hero),
            contentDescription = "RC Solutions service fleet backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Vertical gradient overlay matching Screen 8 contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to AuroraObsidian.copy(alpha = 0.92f),
                            0.42f to AuroraObsidian.copy(alpha = 0.65f),
                            0.74f to AuroraObsidian.copy(alpha = 0.30f),
                            1.0f to AuroraObsidian.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                // Email fields sit low on the screen: keep them above
                // the keyboard when it opens (same fix as the
                // Assistant text box). Scrollable as a fallback so the
                // taller login form never clips on a small screen —
                // the layout is identical when everything fits.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                RcSolutionsBrandLockup(compact = false)
                Spacer(Modifier.height(34.dp))
                Text(
                    text = "More\nThan Service.\nA Higher Standard.",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 31.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AuroraTextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = AuroraRed.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AuroraRed.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextPrimary,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (infoMessage != null) {
                    Surface(
                        color = AuroraEmerald.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AuroraEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = infoMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextPrimary,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = { isLoading = false }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraEmerald,
                        contentColor = AuroraOnEmerald,
                        disabledContainerColor = AuroraCardSurface,
                        disabledContentColor = AuroraTextSecondary
                    ),
                    border = BorderStroke(1.dp, AuroraBorder)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AuroraOnEmerald,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Signing in...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0B1A12)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "or continue with email",
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("you@yourcompany.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_sign_in_field"),
                    colors = auroraFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_password_field"),
                    colors = auroraFieldColors()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { submitEmailAuth() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("email_auth_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraEmerald,
                        contentColor = AuroraOnEmerald,
                        disabledContainerColor = AuroraCardSurface,
                        disabledContentColor = AuroraTextSecondary
                    ),
                    border = BorderStroke(1.dp, AuroraBorder)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = AuroraOnEmerald,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Please wait...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Text(
                            text = if (isSignUp) "Create account" else "Sign in with email",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            errorMessage = null
                            infoMessage = null
                            isSignUp = !isSignUp
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isSignUp) "Have an account? Sign in"
                            else "New here? Create an account",
                            color = AuroraEmerald,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    TextButton(
                        onClick = {
                            errorMessage = null
                            infoMessage = null
                            val cleanEmail = email.trim()
                            if (cleanEmail.isBlank() || !emailLooksValid(cleanEmail)) {
                                errorMessage = "Enter your email above first."
                                return@TextButton
                            }
                            isLoading = true
                            sendPasswordReset(
                                email = cleanEmail,
                                onSent = {
                                    isLoading = false
                                    infoMessage = "Check your email for the reset link."
                                },
                                onError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope
                            )
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = AuroraEmerald,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "BUILDING CLEANER\nBRIGHTER TOMORROWS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AuroraTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
