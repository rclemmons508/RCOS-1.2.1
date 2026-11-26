package com.rcsolutions.rcosmobile.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rcsolutions.rcosmobile.data.AssistantDocument
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.model.AiTaskProfile
import com.rcsolutions.rcosmobile.model.ChatMessage
import com.rcsolutions.rcosmobile.model.LiveVoiceStatus
import com.rcsolutions.rcosmobile.model.LiveVoiceUiState
import com.rcsolutions.rcosmobile.ui.AiConsoleState
import com.rcsolutions.rcosmobile.ui.components.AcousticWaveOrb
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardSurface
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraRed
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import java.util.Locale

@Composable
fun AiAssistantScreen(
    aiState: AiConsoleState,
    liveVoiceState: LiveVoiceUiState,
    messages: List<ChatMessage>,
    documents: List<AssistantDocument>,
    attachedDocumentIds: Set<String>,
    isUploadingDocument: Boolean,
    documentError: String?,
    isSpeakerphone: Boolean,
    onSelectEmployee: (String) -> Unit,
    onSelectTaskProfile: (AiTaskProfile) -> Unit,
    onSendMessage: (prompt: String, docText: String, bitmap: Bitmap?) -> Unit,
    onStartLiveVoice: () -> Unit,
    onStopLiveVoice: () -> Unit,
    onUploadDocument: (name: String, mimeType: String, fileBase64: String) -> Unit,
    onToggleAttachDocument: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    onCreateJobFromDocument: (AssistantDocument) -> Unit,
    onAddClientFromDocument: (AssistantDocument) -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onDismissLiveVoiceError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedDocName by remember { mutableStateOf("") }
    var attachedDocText by remember { mutableStateOf("") }
    var micPermissionError by remember { mutableStateOf<String?>(null) }
    var localUploadError by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionError = null
            onStartLiveVoice()
        } else {
            micPermissionError = "Microphone permission is required for live voice conversations."
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            attachedBitmap = bmp
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            if (mime.startsWith("image/")) {
                attachedBitmap = runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }.getOrNull()
            } else {
                // Documents go to the server (POST /api/assistant/documents):
                // text is extracted there and the details are pulled into
                // jobs/clients. The old inline 4000-char read is gone —
                // the server is the only reader now.
                val name = runCatching {
                    context.contentResolver.query(
                        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull() ?: uri.lastPathSegment ?: "Document"
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                when {
                    bytes == null -> localUploadError = "Could not read that file."
                    bytes.size > 10 * 1024 * 1024 -> localUploadError = "That file is over the 10 MB limit."
                    else -> {
                        localUploadError = null
                        onUploadDocument(
                            name,
                            mime.ifBlank { "application/octet-stream" },
                            Base64.encodeToString(bytes, Base64.NO_WRAP)
                        )
                    }
                }
            }
        }
    }

    val activeEmployee = remember(aiState.selectedEmployeeId) {
        RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == aiState.selectedEmployeeId }
            ?: RcosDefaults.AI_EMPLOYEES.first()
    }

    val roleMessages = remember(messages, aiState.selectedEmployeeId) {
        val filtered = messages.filter { it.employeeId == aiState.selectedEmployeeId || it.employeeId.isBlank() }
        filtered.ifEmpty { messages }
    }

    LaunchedEffect(roleMessages.size, aiState.streamingText.length) {
        if (roleMessages.isNotEmpty() || aiState.isStreaming) {
            val targetIndex = (roleMessages.size + if (aiState.isStreaming) 1 else 0).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    fun launchSpeechDictation() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate message for ${activeEmployee.name}")
        }
        runCatching { speechLauncher.launch(intent) }
    }

    fun toggleLiveVoiceSession() {
        if (liveVoiceState.status == LiveVoiceStatus.LIVE || liveVoiceState.status == LiveVoiceStatus.CONNECTING) {
            onStopLiveVoice()
            return
        }
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            micPermissionError = null
            onStartLiveVoice()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun submitCurrentPrompt(promptOverride: String? = null) {
        val prompt = (promptOverride ?: inputText).trim()
        if (prompt.isEmpty() && attachedBitmap == null && attachedDocumentIds.isEmpty()) return
        onSendMessage(prompt, attachedDocText, attachedBitmap)
        if (promptOverride == null) inputText = ""
        attachedBitmap = null
        attachedDocName = ""
        attachedDocText = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            // Keyboard fix: without this the soft keyboard covers the
            // input bar (and the pills) when typing. Same pattern as
            // AccountScreen — the input stays above the keyboard.
            .imePadding()
    ) {
        // Header with Live Voice quick trigger
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Assistant",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AuroraTextPrimary
                )
                Text(
                    text = "${activeEmployee.name} • ${aiState.activeModelId}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraEmerald,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val isLiveOrConnecting = liveVoiceState.status == LiveVoiceStatus.LIVE ||
                liveVoiceState.status == LiveVoiceStatus.CONNECTING
            OutlinedButton(
                onClick = { toggleLiveVoiceSession() },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isLiveOrConnecting) AuroraRed else AuroraEmerald),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isLiveOrConnecting) AuroraRed.copy(alpha = 0.16f) else AuroraEmerald.copy(alpha = 0.14f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("ai_header_live_voice_button")
            ) {
                Icon(
                    imageVector = if (isLiveOrConnecting) Icons.Outlined.CallEnd else Icons.Outlined.GraphicEq,
                    contentDescription = if (isLiveOrConnecting) "End live voice" else "Start live voice",
                    tint = if (isLiveOrConnecting) AuroraRed else AuroraEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isLiveOrConnecting) "End Live" else "Go Live",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLiveOrConnecting) AuroraRed else AuroraEmerald
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Role Specialist Selector Strip (System Instruction per AI Employee role)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RcosDefaults.AI_EMPLOYEES.take(8).forEach { emp ->
                val selected = emp.id == aiState.selectedEmployeeId
                FilterChip(
                    selected = selected,
                    onClick = { onSelectEmployee(emp.id) },
                    label = { Text(emp.name, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AuroraEmerald,
                        selectedLabelColor = AuroraOnEmerald,
                        selectedLeadingIconColor = AuroraOnEmerald,
                        containerColor = AuroraCardSurface,
                        labelColor = AuroraTextSecondary,
                        iconColor = AuroraEmerald
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = AuroraBorder
                    )
                )
            }
        }

        // Model Routing Profile Strip (Auto, 3.8 Flash, Reasoning, Multimodal, Lite)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiTaskProfile.entries.forEach { profile ->
                val selected = profile == aiState.selectedTaskProfile
                FilterChip(
                    selected = selected,
                    onClick = { onSelectTaskProfile(profile) },
                    label = {
                        Text(
                            text = profile.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AuroraCardElevated,
                        selectedLabelColor = AuroraEmerald,
                        selectedLeadingIconColor = AuroraEmerald,
                        containerColor = AuroraCardSurface,
                        labelColor = AuroraTextMuted,
                        iconColor = AuroraTextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) AuroraEmerald else AuroraBorder
                    ),
                    modifier = Modifier.testTag("ai_model_profile_${profile.id}")
                )
            }
        }

        // LiveKit Live Voice Session Card
        if (liveVoiceState.status != LiveVoiceStatus.IDLE) {
            Spacer(Modifier.height(6.dp))
            LiveVoiceSessionBanner(
                state = liveVoiceState,
                isSpeakerphone = isSpeakerphone,
                onEndVoice = onStopLiveVoice,
                onToggleSpeakerphone = onToggleSpeakerphone,
                onDismissError = onDismissLiveVoiceError
            )
        }

        if (micPermissionError != null) {
            Spacer(Modifier.height(6.dp))
            Surface(
                color = AuroraRed.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AuroraRed.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = micPermissionError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Allow Mic", color = AuroraEmerald)
                    }
                }
            }
        }

        // Assistant documents (server-side). Visible only when there is
        // something to show: documents, an upload in flight, or an error.
        if (documents.isNotEmpty() || isUploadingDocument || documentError != null || localUploadError != null) {
            Spacer(Modifier.height(6.dp))
            AuroraCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_documents_section")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Documents",
                        style = MaterialTheme.typography.titleSmall,
                        color = AuroraTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUploadingDocument) {
                        CircularProgressIndicator(color = AuroraEmerald, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
                val docError = documentError ?: localUploadError
                if (docError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = docError, style = MaterialTheme.typography.labelSmall, color = AuroraRed)
                }
                documents.forEach { doc ->
                    AssistantDocumentRow(
                        document = doc,
                        attached = doc.id in attachedDocumentIds,
                        onToggleAttach = { onToggleAttachDocument(doc.id) },
                        onDelete = { onDeleteDocument(doc.id) },
                        onCreateJob = { onCreateJobFromDocument(doc) },
                        onAddClient = { onAddClientFromDocument(doc) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        // Main Body: Either Hero Orb + 5 Starter Prompts OR Scrollable Multi-Turn Conversation Thread
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (roleMessages.isEmpty() && !aiState.isStreaming) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        AcousticWaveOrb(
                            isActive = liveVoiceState.status == LiveVoiceStatus.LIVE,
                            size = 148.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "How can I help you today?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AuroraTextPrimary
                        )
                        Text(
                            text = "Role: ${activeEmployee.tagline} • ${aiState.selectedTaskProfile.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraTextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(RcosDefaults.AI_STARTER_PROMPTS) { prompt ->
                        AuroraCard(
                            onClick = { submitCurrentPrompt(prompt) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_starter_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(AuroraEmerald.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SmartToy,
                                        contentDescription = null,
                                        tint = AuroraEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AuroraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ai_chat_thread"),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            AcousticWaveOrb(
                                isActive = aiState.isStreaming || liveVoiceState.status == LiveVoiceStatus.LIVE,
                                size = 46.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeEmployee.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AuroraEmerald
                                )
                                Text(
                                    text = if (aiState.isStreaming) {
                                        "Streaming via ${aiState.activeModelId}..."
                                    } else {
                                        "Multi-turn history active • ${aiState.activeModelId}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraTextMuted
                                )
                            }
                        }
                    }

                    items(roleMessages, key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}" } }) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isUser) AuroraEmerald else AuroraCardSurface,
                                border = BorderStroke(1.dp, if (isUser) AuroraEmerald else AuroraBorder),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (!isUser) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (msg.senderName.isNotBlank()) {
                                                Text(
                                                    text = msg.senderName,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = AuroraEmerald
                                                )
                                            }
                                            if (msg.modelUsed.isNotBlank()) {
                                                Text(
                                                    text = msg.modelUsed,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AuroraTextMuted
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(3.dp))
                                    }
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isUser) AuroraOnEmerald else AuroraTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (aiState.isStreaming) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = AuroraCardElevated,
                                    border = BorderStroke(1.dp, AuroraEmerald.copy(alpha = 0.5f)),
                                    modifier = Modifier.widthIn(max = 310.dp)
                                ) {
                                    Text(
                                        text = aiState.streamingText.ifBlank { "Analyzing with ${aiState.activeModelId}..." },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = AuroraTextPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (aiState.error != null) {
            Surface(
                color = AuroraRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AuroraRed.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = aiState.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextPrimary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (attachedBitmap != null || attachedDocName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AuroraCardElevated)
                    .border(1.dp, AuroraEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (attachedBitmap != null) "Photo ready for AI analysis" else "Doc: $attachedDocName",
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraEmerald,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = {
                        attachedBitmap = null
                        attachedDocName = ""
                        attachedDocText = ""
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove attachment", tint = AuroraTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Bottom Input Bar + Voice (Live API) / Camera / Attach pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask ${activeEmployee.name}...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(onClick = { launchSpeechDictation() }) {
                        Icon(Icons.Outlined.Mic, contentDescription = "Dictate message", tint = AuroraTextSecondary)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_message_input"),
                colors = auroraFieldColors()
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (!aiState.isStreaming) AuroraEmerald else AuroraBorder)
                    .testTag("ai_send_button")
                    .clickable(enabled = !aiState.isStreaming) { submitCurrentPrompt() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowUpward,
                    contentDescription = "Send to AI Assistant",
                    tint = AuroraOnEmerald
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isLive = liveVoiceState.status == LiveVoiceStatus.LIVE ||
                liveVoiceState.status == LiveVoiceStatus.CONNECTING
            AiUtilityPill(
                label = if (isLive) "End Voice" else "Voice",
                icon = if (isLive) Icons.Outlined.CallEnd else Icons.Outlined.GraphicEq,
                testTag = "ai_voice_button",
                highlighted = isLive,
                onClick = { toggleLiveVoiceSession() },
                modifier = Modifier.weight(1f)
            )
            AiUtilityPill(
                label = "Camera",
                icon = Icons.Outlined.CameraAlt,
                testTag = "ai_camera_button",
                onClick = { runCatching { cameraLauncher.launch(null) } },
                modifier = Modifier.weight(1f)
            )
            AiUtilityPill(
                label = "Attach",
                icon = Icons.Outlined.AttachFile,
                testTag = "ai_attach_button",
                onClick = { fileLauncher.launch(arrayOf("image/*", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/*")) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AssistantDocumentRow(
    document: AssistantDocument,
    attached: Boolean,
    onToggleAttach: () -> Unit,
    onDelete: () -> Unit,
    onCreateJob: () -> Unit,
    onAddClient: () -> Unit
) {
    // Two-tap confirm for the record-creating / destructive actions:
    // first tap arms ("Confirm?"), second tap runs it. "Job" needs no
    // confirm — it only asks Morgan to draft in chat (Phase 5 owns the
    // in-chat job-confirmation card).
    var confirmAction by remember { mutableStateOf<String?>(null) }
    val extracted = document.extracted
    val who = listOf(extracted.contactName, extracted.companyName)
        .filter { it.isNotBlank() }.joinToString(" • ")
    val detail = listOf(who, extracted.phone, extracted.email)
        .filter { it.isNotBlank() }.joinToString(" • ")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (detail.isNotBlank()) detail else "No contact details extracted",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(document.sizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextMuted
                )
            }
            TextButton(onClick = onToggleAttach) {
                Text(
                    if (attached) "Attached" else "Attach",
                    color = if (attached) AuroraEmerald else AuroraTextSecondary
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCreateJob) {
                Text("Job", color = AuroraEmerald)
            }
            TextButton(onClick = {
                if (confirmAction == "client") {
                    confirmAction = null
                    onAddClient()
                } else {
                    confirmAction = "client"
                }
            }) {
                Text(if (confirmAction == "client") "Confirm add?" else "Client", color = AuroraEmerald)
            }
            TextButton(onClick = {
                if (confirmAction == "delete") {
                    confirmAction = null
                    onDelete()
                } else {
                    confirmAction = "delete"
                }
            }) {
                Text(if (confirmAction == "delete") "Confirm delete?" else "Delete", color = AuroraRed)
            }
        }
    }
}

/** File size as "12 KB" / "1.2 MB" — same shape as the old app's
 * `formatFileSize` in `src/lib/assistantDocuments.ts`. */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
    return "%.1f MB".format(Locale.US, bytes / (1024.0 * 1024.0))
}

@Composable
private fun LiveVoiceSessionBanner(
    state: LiveVoiceUiState,
    isSpeakerphone: Boolean,
    onEndVoice: () -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onDismissError: () -> Unit
) {
    val isError = state.status == LiveVoiceStatus.ERROR
    val isLive = state.status == LiveVoiceStatus.LIVE
    val timerText = remember(state.durationSeconds) {
        val m = state.durationSeconds / 60
        val s = state.durationSeconds % 60
        String.format(Locale.US, "%02d:%02d", m, s)
    }

    AuroraCard(
        highlighted = isLive,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_live_voice_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AcousticWaveOrb(isActive = isLive, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isError) "Live Voice Error" else "${state.activeRoleName} • Live Voice",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isError) AuroraRed else AuroraEmerald
                )
                Text(
                    text = state.error ?: "${state.statusDetail} ($timerText)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isError) {
                TextButton(onClick = onDismissError) {
                    Text("Dismiss", color = AuroraEmerald)
                }
            } else {
                OutlinedButton(
                    onClick = onEndVoice,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AuroraRed),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("ai_live_voice_stop_button")
                ) {
                    Text("End", style = MaterialTheme.typography.labelMedium, color = AuroraRed)
                }
            }
        }
        if (isLive) {
            // The LiveKit agent has no text-injection channel (the
            // server worker only listens to audio), so the old Gemini
            // Live nudge chips are gone. Speak to the agent, or send
            // the question in text chat instead.
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onToggleSpeakerphone,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AuroraBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("ai_live_voice_speaker_button")
                ) {
                    Text(
                        if (isSpeakerphone) "Speakerphone: On" else "Speakerphone: Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = AuroraTextPrimary
                    )
                }
                Text(
                    text = "Voice starts on the earpiece.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuroraTextSecondary
                )
            }
        }
    }
}

@Composable
private fun AiUtilityPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (highlighted) AuroraRed else AuroraBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (highlighted) AuroraRed.copy(alpha = 0.14f) else AuroraCardSurface
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier
            .height(44.dp)
            .testTag(testTag)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (highlighted) AuroraRed else AuroraTextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) AuroraRed else AuroraTextPrimary
        )
    }
}
