package com.rcsolutions.rcosmobile.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.model.AutonomyMode
import com.rcsolutions.rcosmobile.model.OnboardingExtraction
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.ui.components.AuroraBackground
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.components.RcSolutionsBrandLockup
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    defaultOwnerName: String,
    extractionState: UiState<OnboardingExtraction>?,
    isSaving: Boolean,
    canCancel: Boolean,
    onAnalyzeInput: (String, Bitmap?) -> Unit,
    onAnalyzeDocument: (Uri, String?, String?) -> Unit,
    onClearExtraction: () -> Unit,
    // App Check setup card: true when the last AI call was rejected
    // because this build's debug token isn't registered in the console.
    appCheckError: Boolean,
    onDismissAppCheck: () -> Unit,
    // The debug token to register (null until the SDK generates one).
    debugAppCheckToken: String?,
    // Label of the document the last extraction came from, if any.
    onboardingDocLabel: String?,
    onSaveWorkspace: (
        name: String,
        industryId: String,
        services: List<String>,
        serviceArea: String,
        businessHours: String,
        pricingApproach: String,
        brandTone: String,
        autonomyMode: AutonomyMode,
        dollarThreshold: Double,
        emergencyAvailable: Boolean,
        knowledgeText: String,
        contactName: String,
        contactPhone: String,
        contactEmail: String,
        teamSize: String,
        servesResidential: Boolean,
        servesCommercial: Boolean
    ) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var step by remember { mutableIntStateOf(1) }
    var docOrDescription by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedDocLabel by remember { mutableStateOf<String?>(null) }

    var businessName by remember { mutableStateOf("") }
    var selectedIndustryId by remember { mutableStateOf("general_small_business") }
    var servicesText by remember { mutableStateOf(RcosDefaults.findPreset("general_small_business").defaultServices.joinToString(", ")) }
    var serviceArea by remember { mutableStateOf("") }
    var businessHours by remember { mutableStateOf("Mon–Sat 7:00 AM – 6:00 PM") }
    var pricingApproach by remember { mutableStateOf("Written quote upfront with clear scope") }
    var brandTone by remember { mutableStateOf("Friendly, punctual, and safety-focused") }
    var emergencyAvailable by remember { mutableStateOf(true) }
    // Which document the visible sections were filled from. Captured
    // locally on extraction success because the ViewModel clears its
    // label when the extraction state is cleared right after.
    var filledFromDoc by remember { mutableStateOf<String?>(null) }
    // Contact + team sections — filled by the AI extraction when the
    // document states them, otherwise typed in by hand.
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var teamSize by remember { mutableStateOf("") }
    var servesResidential by remember { mutableStateOf(true) }
    var servesCommercial by remember { mutableStateOf(false) }
    var autonomyMode by remember { mutableStateOf(AutonomyMode.SUPERVISED) }
    var dollarThreshold by remember { mutableDoubleStateOf(250.0) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedBitmap = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            }.getOrNull()
        }
    }

    // Any company document: PDF, Word (.docx), text, CSV, or image.
    // The AI reads it and fills the sections below — only with what
    // the document actually states.
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val displayName = runCatching {
                context.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
            attachedDocLabel = displayName ?: "Document"
            onAnalyzeDocument(uri, context.contentResolver.getType(uri), displayName)
        }
    }

    LaunchedEffect(extractionState) {
        if (extractionState is UiState.Success) {
            val ext = extractionState.data
            if (ext.businessName.isNotBlank()) businessName = ext.businessName
            if (ext.industryId.isNotBlank()) selectedIndustryId = ext.industryId
            if (ext.services.isNotEmpty()) servicesText = ext.services.joinToString(", ")
            if (ext.serviceArea.isNotBlank()) serviceArea = ext.serviceArea
            if (ext.businessHours.isNotBlank()) businessHours = ext.businessHours
            if (ext.pricingApproach.isNotBlank()) pricingApproach = ext.pricingApproach
            if (ext.brandTone.isNotBlank()) brandTone = ext.brandTone
            emergencyAvailable = ext.emergencyAvailable
            // Contact + team: fill only what the document stated.
            if (ext.contactName.isNotBlank()) contactName = ext.contactName
            if (ext.contactPhone.isNotBlank()) contactPhone = ext.contactPhone
            if (ext.contactEmail.isNotBlank()) contactEmail = ext.contactEmail
            if (ext.teamSize.isNotBlank()) teamSize = ext.teamSize
            servesResidential = ext.servesResidential
            servesCommercial = ext.servesCommercial
            filledFromDoc = onboardingDocLabel
            step = 2
            onClearExtraction()
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RcSolutionsBrandLockup(compact = true)
                if (canCancel) {
                    TextButton(onClick = onCancel) {
                        Text("Close", color = AuroraTextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Step $step of 3",
                style = MaterialTheme.typography.labelMedium,
                color = AuroraEmerald
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = AuroraEmerald,
                trackColor = AuroraBorder
            )
            Spacer(Modifier.height(16.dp))

            when (step) {
                1 -> {
                    Text(
                        text = "Set up your company",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AuroraTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Welcome, ${defaultOwnerName.ifBlank { "Owner" }}. Upload any company document or paste your details and let Gemini fill in the sections — or skip straight to picking your trade and type everything in by hand.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraTextSecondary
                    )
                    Spacer(Modifier.height(16.dp))

                    // App Check setup card — shown only when the AI call
                    // was rejected because this build's debug token was
                    // never registered in the Firebase console. The token
                    // is shown here so it can be copied into the console
                    // without adb or Logcat; manual entry still works.
                    if (appCheckError) {
                        AuroraCard(highlighted = true) {
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
                                text = "Firebase AI is blocked because this build's debug token isn't registered in the Firebase console. Register it once, then tap \"Extract Details with AI\" again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuroraTextSecondary
                            )
                            if (debugAppCheckToken != null) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = debugAppCheckToken,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Debug token — copy into the Firebase console") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = auroraFieldColors()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AuroraSecondaryCta(
                                        text = "Copy token",
                                        testTag = "onboarding_copy_appcheck_token",
                                        onClick = { clipboard.setText(AnnotatedString(debugAppCheckToken)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onDismissAppCheck) {
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
                                    text = "The token appears here after an AI call is attempted. Tap \"Extract Details with AI\" once, then come back to this screen to copy it.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuroraTextSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = onDismissAppCheck) {
                                    Text("Dismiss", color = AuroraTextSecondary)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    AuroraCard(highlighted = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = AuroraEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Smart Company Scan (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                color = AuroraTextPrimary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = docOrDescription,
                            onValueChange = { docOrDescription = it },
                            label = { Text("Paste company bio, rates, or services") },
                            placeholder = { Text("e.g. RC Exterior Care in Greenville, SC — driveway washing, soft house wash, roof treatment...") },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_ai_input"),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuroraSecondaryCta(
                                text = if (attachedBitmap != null) "Flyer / Photo Attached" else "Attach Flyer Photo",
                                testTag = "onboarding_attach_photo",
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            AuroraSecondaryCta(
                                text = "Upload Document",
                                testTag = "onboarding_attach_document",
                                onClick = {
                                    documentPickerLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "text/plain",
                                            "text/csv",
                                            "image/*"
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (attachedDocLabel != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Document attached: $attachedDocLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuroraEmerald
                            )
                        }
                        if (extractionState is UiState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = extractionState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuroraRed
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        AuroraPrimaryCta(
                            text = if (extractionState is UiState.Loading) "Analyzing with Gemini..." else "Extract Details with AI",
                            icon = Icons.Outlined.AutoAwesome,
                            testTag = "onboarding_analyze_button",
                            enabled = extractionState !is UiState.Loading && (docOrDescription.isNotBlank() || attachedBitmap != null),
                            onClick = { onAnalyzeInput(docOrDescription, attachedBitmap) }
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    AuroraSecondaryCta(
                        text = "Skip for now — Pick Trade Manually",
                        testTag = "onboarding_skip_ai",
                        onClick = { step = 2 }
                    )
                }

                2 -> {
                    Text(
                        text = "Review business profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AuroraTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Confirm or edit your details before launching. Anything the document didn't state is left blank for you to fill in — nothing is guessed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraTextSecondary
                    )
                    if (filledFromDoc != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Filled in from $filledFromDoc — check anything the document didn't state.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraEmerald
                        )
                    }
                    Spacer(Modifier.height(14.dp))

                    // ---- Business ----
                    Text("Business", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                    Spacer(Modifier.height(8.dp))
                    AuroraCard {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Trade / Industry", style = MaterialTheme.typography.labelMedium, color = AuroraTextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RcosDefaults.INDUSTRY_PRESETS.forEach { preset ->
                                val selected = preset.id == selectedIndustryId
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedIndustryId = preset.id
                                        servicesText = preset.defaultServices.joinToString(", ")
                                        brandTone = preset.recommendedTone
                                    },
                                    label = { Text(preset.name, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AuroraEmerald,
                                        selectedLabelColor = AuroraOnEmerald,
                                        containerColor = AuroraCardElevated,
                                        labelColor = AuroraTextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = AuroraBorder
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = servicesText,
                            onValueChange = { servicesText = it },
                            label = { Text("Services Offered (comma-separated)") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = serviceArea,
                            onValueChange = { serviceArea = it },
                            label = { Text("City / Service Area (enables live weather)") },
                            placeholder = { Text("e.g. Greenville, SC") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_city_input"),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = businessHours,
                            onValueChange = { businessHours = it },
                            label = { Text("Operating Hours") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = auroraFieldColors()
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ---- Contact ----
                    Text("Contact", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                    Spacer(Modifier.height(8.dp))
                    AuroraCard {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Contact Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_contact_name_input"),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Business Phone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_contact_phone_input"),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Business Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_contact_email_input"),
                            colors = auroraFieldColors()
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ---- Prices & rates ----
                    Text("Prices & Rates", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                    Spacer(Modifier.height(8.dp))
                    AuroraCard {
                        OutlinedTextField(
                            value = pricingApproach,
                            onValueChange = { pricingApproach = it },
                            label = { Text("Pricing Approach / Rates") },
                            placeholder = { Text("e.g. $85/hr, flat $250 per visit") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_pricing_input"),
                            colors = auroraFieldColors()
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ---- Team & coverage ----
                    Text("Team & Coverage", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                    Spacer(Modifier.height(8.dp))
                    AuroraCard {
                        OutlinedTextField(
                            value = teamSize,
                            onValueChange = { teamSize = it },
                            label = { Text("Team Size") },
                            placeholder = { Text("e.g. Just me, 3 crew, 12 staff") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("onboarding_team_size_input"),
                            colors = auroraFieldColors()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Serves Residential", style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                                Text("Homeowners and households.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                            }
                            Switch(
                                checked = servesResidential,
                                onCheckedChange = { servesResidential = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuroraOnEmerald,
                                    checkedTrackColor = AuroraEmerald
                                )
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Serves Commercial", style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                                Text("Businesses and properties.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                            }
                            Switch(
                                checked = servesCommercial,
                                onCheckedChange = { servesCommercial = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuroraOnEmerald,
                                    checkedTrackColor = AuroraEmerald
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AuroraSecondaryCta(
                            text = "Back",
                            testTag = "onboarding_back_1",
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        AuroraPrimaryCta(
                            text = "Next: Safety Rules",
                            testTag = "onboarding_next_3",
                            enabled = businessName.isNotBlank(),
                            onClick = { step = 3 },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }

                else -> {
                    Text(
                        text = "AI safety & approval limit",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AuroraTextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Nothing outbound sends without your sign-off above your limit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuroraTextSecondary
                    )
                    Spacer(Modifier.height(14.dp))

                    AuroraCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Supervised Approval Mode", style = MaterialTheme.typography.titleMedium, color = AuroraTextPrimary)
                                Text(
                                    "Recommended: review quotes, outbound messages, and invoices before sending.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AuroraTextSecondary
                                )
                            }
                            Switch(
                                checked = autonomyMode == AutonomyMode.SUPERVISED,
                                onCheckedChange = {
                                    autonomyMode = if (it) AutonomyMode.SUPERVISED else AutonomyMode.AUTONOMOUS
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuroraOnEmerald,
                                    checkedTrackColor = AuroraEmerald
                                )
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Owner Approval Required Above: $${dollarThreshold.roundToInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            color = AuroraEmerald
                        )
                        Slider(
                            value = dollarThreshold.toFloat(),
                            onValueChange = { dollarThreshold = ((it / 50f).roundToInt() * 50).toDouble() },
                            valueRange = 50f..1500f,
                            colors = SliderDefaults.colors(
                                thumbColor = AuroraEmerald,
                                activeTrackColor = AuroraEmerald
                            )
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("24/7 Emergency Call Dispatch", style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                                Text("Flag urgent leaks, outages, or safety calls first.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextMuted)
                            }
                            Switch(
                                checked = emergencyAvailable,
                                onCheckedChange = { emergencyAvailable = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AuroraOnEmerald,
                                    checkedTrackColor = AuroraEmerald
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AuroraSecondaryCta(
                            text = "Back",
                            testTag = "onboarding_back_2",
                            onClick = { step = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        AuroraPrimaryCta(
                            text = if (isSaving) "Saving..." else "Launch Workspace",
                            testTag = "onboarding_finish_button",
                            enabled = !isSaving && businessName.isNotBlank(),
                            onClick = {
                                val parsedServices = servicesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                onSaveWorkspace(
                                    businessName,
                                    selectedIndustryId,
                                    parsedServices,
                                    serviceArea,
                                    businessHours,
                                    pricingApproach,
                                    brandTone,
                                    autonomyMode,
                                    dollarThreshold,
                                    emergencyAvailable,
                                    docOrDescription,
                                    contactName,
                                    contactPhone,
                                    contactEmail,
                                    teamSize,
                                    servesResidential,
                                    servesCommercial
                                )
                            },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }

            if (extractionState is UiState.Loading || isSaving) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AuroraEmerald, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing securely...", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
                }
            }
        }
    }
}

@Composable
fun auroraFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AuroraTextPrimary,
    unfocusedTextColor = AuroraTextPrimary,
    focusedBorderColor = AuroraEmerald,
    unfocusedBorderColor = AuroraBorder,
    focusedLabelColor = AuroraEmerald,
    unfocusedLabelColor = AuroraTextSecondary,
    cursorColor = AuroraEmerald,
    focusedPlaceholderColor = AuroraTextMuted,
    unfocusedPlaceholderColor = AuroraTextMuted
)
