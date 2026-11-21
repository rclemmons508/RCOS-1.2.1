package com.rcsolutions.rcosmobile.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.model.ChatMessage
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

@Composable
fun AiAssistantScreen(
    aiState: AiConsoleState,
    messages: List<ChatMessage>,
    onSelectEmployee: (String) -> Unit,
    onSendMessage: (prompt: String, docText: String, bitmap: Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var attachedDocName by remember { mutableStateOf("") }
    var attachedDocText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

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
                val readText = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty()
                if (readText.isNotBlank()) {
                    attachedDocName = uri.lastPathSegment ?: "Attached document"
                    attachedDocText = readText.take(4000)
                }
            }
        }
    }

    LaunchedEffect(messages.size, aiState.streamingText.length) {
        if (messages.isNotEmpty() || aiState.isStreaming) {
            val targetIndex = (messages.size + if (aiState.isStreaming) 1 else 0 - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    fun launchSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to RC Solutions AI Assistant")
        }
        runCatching { speechLauncher.launch(intent) }
    }

    fun submitCurrentPrompt(promptOverride: String? = null) {
        val prompt = (promptOverride ?: inputText).trim()
        if (prompt.isEmpty() && attachedBitmap == null) return
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
    ) {
        // Header matching Screen 2
        Text(
            text = "AI Assistant",
            style = MaterialTheme.typography.headlineMedium,
            color = AuroraTextPrimary
        )
        Text(
            text = "Your 24/7 business partner.",
            style = MaterialTheme.typography.bodyMedium,
            color = AuroraTextSecondary
        )
        Spacer(Modifier.height(10.dp))

        // Role Specialist Selector Strip (15 AI Employees consolidated into unified AI Assistant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RcosDefaults.AI_EMPLOYEES.take(7).forEach { emp ->
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

        // Main Body: Either Hero Orb + 5 Starter Prompts OR Conversation Stream
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !aiState.isStreaming) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        AcousticWaveOrb(
                            isActive = false,
                            size = 152.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "How can I help you today?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AuroraTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
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
                    modifier = Modifier.fillMaxSize(),
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
                                isActive = aiState.isStreaming,
                                size = 48.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val empName = RcosDefaults.AI_EMPLOYEES.firstOrNull {
                                    it.id == aiState.selectedEmployeeId
                                }?.name ?: "AI Assistant"
                                Text(
                                    text = empName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AuroraEmerald
                                )
                                Text(
                                    text = if (aiState.isStreaming) "Thinking & streaming live..." else "Powered by Gemini 3.8 Flash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraTextMuted
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}" } }) { msg ->
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
                                    if (!isUser && msg.senderName.isNotBlank()) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AuroraEmerald
                                        )
                                        Spacer(Modifier.height(2.dp))
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
                                        text = aiState.streamingText.ifBlank { "Analyzing business context..." },
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

        // Bottom Input Bar + Voice / Camera / Attach pills (Screen 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(onClick = { launchSpeechInput() }) {
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
            AiUtilityPill(
                label = "Voice",
                icon = Icons.Outlined.GraphicEq,
                testTag = "ai_voice_button",
                onClick = { launchSpeechInput() },
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
                onClick = { fileLauncher.launch(arrayOf("image/*", "text/*")) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AiUtilityPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AuroraBorder),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier
            .height(44.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = null, tint = AuroraTextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AuroraTextPrimary)
    }
}
