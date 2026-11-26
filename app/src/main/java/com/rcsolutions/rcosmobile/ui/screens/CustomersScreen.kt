package com.rcsolutions.rcosmobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rcsolutions.rcosmobile.model.CustomField
import com.rcsolutions.rcosmobile.model.Customer
import com.rcsolutions.rcosmobile.model.CustomerStatus
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraDivider
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraFilterPill
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSectionHeader
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardSurface
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary

private enum class CustomerFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    LEADS("Leads"),
    ARCHIVED("Archived")
}

@Composable
fun CustomersScreen(
    customers: List<Customer>,
    jobs: List<Job> = emptyList(),
    onBack: (() -> Unit)? = null,
    onAddCustomer: (name: String, company: String, phone: String, email: String, address: String, status: String, serviceType: String, notes: String) -> Unit,
    onUpdateCustomerStatus: (Customer, String) -> Unit,
    onUpdateCustomerDetails: (Customer, String, String, String, String, List<CustomField>) -> Unit = { _, _, _, _, _, _ -> },
    onDraftFollowUp: (Customer, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CustomerFilter.ALL) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var aiDraftPreview by remember { mutableStateOf<String?>(null) }

    val filtered = remember(customers, searchQuery, selectedFilter) {
        customers.filter { cust ->
            val matchesFilter = when (selectedFilter) {
                CustomerFilter.ALL -> true
                CustomerFilter.ACTIVE -> cust.status == CustomerStatus.ACTIVE.code || cust.status == CustomerStatus.VIP.code
                CustomerFilter.LEADS -> cust.status == CustomerStatus.LEAD.code
                CustomerFilter.ARCHIVED -> cust.status == CustomerStatus.INACTIVE.code
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                cust.name.lowercase().contains(q) ||
                cust.company.lowercase().contains(q) ||
                cust.phone.lowercase().contains(q)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = AuroraTextPrimary)
                }
            }
            Column {
                Text("Customers", style = MaterialTheme.typography.headlineMedium, color = AuroraTextPrimary)
                Text("Build relationships. Grow your business.", style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
            }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search customers...") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = AuroraTextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customers_search_input"),
            colors = auroraFieldColors()
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomerFilter.entries.forEach { filter ->
                AuroraFilterPill(
                    label = filter.label,
                    selected = selectedFilter == filter,
                    testTag = "customers_filter_${filter.name.lowercase()}",
                    onClick = { selectedFilter = filter },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    AuroraEmptyState(
                        icon = Icons.Outlined.Groups,
                        title = if (customers.isEmpty()) "No customers yet" else "No matching customers",
                        subtitle = "Save client profiles to keep phone numbers, notes, and AI follow-ups in one place.",
                        actionLabel = "+ Add Customer",
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { cust ->
                    AuroraCard(
                        onClick = { selectedCustomer = cust },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AuroraCardElevated)
                                    .border(1.dp, AuroraEmerald.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cust.initials(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AuroraEmerald
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cust.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AuroraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val statusLabel = CustomerStatus.fromCode(cust.status).label
                                val extra = if (cust.phone.isNotBlank()) " • ${cust.phone}" else ""
                                Text(
                                    text = "$statusLabel$extra",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuroraTextSecondary,
                                    maxLines = 1
                                )
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Open customer", tint = AuroraTextMuted)
                        }
                    }
                }
            }
        }

        AuroraPrimaryCta(
            text = "Add Customer",
            icon = Icons.Outlined.Add,
            testTag = "customers_add_button",
            onClick = { showAddDialog = true }
        )
    }

    if (selectedCustomer != null) {
        val cust = selectedCustomer!!
        val customerJobs = remember(cust, jobs) {
            jobs.filter { it.clientId == cust.id || (it.clientId.isBlank() && it.clientName == cust.name) }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        // 8 fixes Nov 26 (item 3) — the customer detail is a full
        // screen now, not a cramped dialog: editable contact info,
        // notes, owner-defined custom fields, and the customer's jobs.
        // The follow-up button is a two-line full-width button, so
        // its label can no longer clip.
        Dialog(
            onDismissRequest = { selectedCustomer = null; aiDraftPreview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AuroraCardElevated
            ) {
                CustomerDetailSheet(
                    customer = cust,
                    jobs = customerJobs,
                    aiDraftPreview = aiDraftPreview,
                    onClose = { selectedCustomer = null; aiDraftPreview = null },
                    onDraftFollowUp = { onDraftFollowUp(cust) { draft -> aiDraftPreview = draft } },
                    onUpdateStatus = { status ->
                        onUpdateCustomerStatus(cust, status)
                        selectedCustomer = null
                    },
                    onSave = { phone, email, address, notes, customFields ->
                        onUpdateCustomerDetails(cust, phone, email, address, notes, customFields)
                        selectedCustomer = cust.copy(
                            phone = phone,
                            email = email,
                            address = address,
                            aiSummary = notes,
                            customFields = customFields,
                        )
                    },
                )
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var status by remember { mutableStateOf(CustomerStatus.ACTIVE.code) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AuroraCardElevated,
            title = { Text("Add Customer", color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full or Residence Name") }, singleLine = true, colors = auroraFieldColors())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true, colors = auroraFieldColors())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, singleLine = true, colors = auroraFieldColors())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Service Address (optional)") }, singleLine = true, colors = auroraFieldColors())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Service Notes") }, singleLine = true, colors = auroraFieldColors())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuroraFilterPill(
                            label = "Active",
                            selected = status == CustomerStatus.ACTIVE.code,
                            testTag = "add_cust_active",
                            onClick = { status = CustomerStatus.ACTIVE.code },
                            modifier = Modifier.weight(1f)
                        )
                        AuroraFilterPill(
                            label = "New Lead",
                            selected = status == CustomerStatus.LEAD.code,
                            testTag = "add_cust_lead",
                            onClick = { status = CustomerStatus.LEAD.code },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onAddCustomer(name, "", phone, email, address, status, "", notes)
                        showAddDialog = false
                    }
                }) {
                    Text("Save Customer", color = AuroraEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = AuroraTextSecondary)
                }
            }
        )
    }
}

/**
 * 8 fixes Nov 26 (item 3) — the full-screen customer profile. Keeps
 * the call/text/email actions and the AI follow-up draft, and adds
 * what the bare card never had: editable contact info, a notes field,
 * owner-defined custom fields (label + value, add/remove), and the
 * customer's jobs. Nothing is invented: fields start from the stored
 * customer, jobs only appear when the data exists, and the save writes
 * only the allowlisted fields.
 */
@Composable
private fun CustomerDetailSheet(
    customer: Customer,
    jobs: List<Job>,
    aiDraftPreview: String?,
    onClose: () -> Unit,
    onDraftFollowUp: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onSave: (phone: String, email: String, address: String, notes: String, customFields: List<CustomField>) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var phone by remember(customer.id) { mutableStateOf(customer.phone) }
    var email by remember(customer.id) { mutableStateOf(customer.email) }
    var address by remember(customer.id) { mutableStateOf(customer.address) }
    var notes by remember(customer.id) { mutableStateOf(customer.aiSummary) }
    var customFields by remember(customer.id) { mutableStateOf(customer.customFields) }
    var newFieldLabel by remember { mutableStateOf("") }
    var newFieldValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) {
                Text("‹ Back", color = AuroraTextSecondary)
            }
            TextButton(onClick = { onSave(phone, email, address, notes, customFields) }) {
                Text("Save", color = AuroraEmerald)
            }
        }

        // Header: avatar, name, status.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AuroraCardElevated)
                    .border(1.dp, AuroraEmerald.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.initials(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AuroraEmerald
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AuroraTextPrimary,
                    maxLines = 2
                )
                Text(
                    text = CustomerStatus.fromCode(customer.status).label,
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraEmerald
                )
            }
        }

        // Call / text / email actions.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (customer.phone.isNotBlank()) {
                IconButton(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))) }
                    },
                    modifier = Modifier.clip(CircleShape).background(AuroraEmerald.copy(alpha = 0.16f))
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = "Call customer", tint = AuroraEmerald)
                }
                IconButton(
                    onClick = {
                        val msgBody = aiDraftPreview ?: "Hi ${customer.name}, following up on your service."
                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${customer.phone}")).apply {
                            putExtra("sms_body", msgBody)
                        }
                        runCatching { context.startActivity(smsIntent) }
                    },
                    modifier = Modifier.clip(CircleShape).background(AuroraEmerald.copy(alpha = 0.16f))
                ) {
                    Icon(Icons.Outlined.Message, contentDescription = "Text customer", tint = AuroraEmerald)
                }
            }
            if (customer.email.isNotBlank()) {
                IconButton(
                    onClick = {
                        val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${customer.email}"))
                        runCatching { context.startActivity(mailIntent) }
                    },
                    modifier = Modifier.clip(CircleShape).background(AuroraEmerald.copy(alpha = 0.16f))
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = "Email customer", tint = AuroraEmerald)
                }
            }
        }

        // Contact info — editable.
        AuroraCard {
            AuroraSectionHeader(title = "Contact")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
        }

        // Notes — editable.
        AuroraCard {
            AuroraSectionHeader(title = "Notes")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Anything worth remembering about this customer") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
        }

        // Custom fields — add/remove, stored on the customer doc.
        AuroraCard {
            AuroraSectionHeader(title = "Extra Information")
            Spacer(Modifier.height(8.dp))
            if (customFields.isEmpty()) {
                Text(
                    text = "No extra fields yet — add things like a gate code or a preferred contact method.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextSecondary
                )
            } else {
                customFields.forEachIndexed { index, field ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(field.label, style = MaterialTheme.typography.labelMedium, color = AuroraTextPrimary)
                            Text(field.value, style = MaterialTheme.typography.bodyMedium, color = AuroraTextSecondary)
                        }
                        IconButton(onClick = { customFields = customFields.filterIndexed { i, _ -> i != index } }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove field", tint = AuroraTextMuted)
                        }
                    }
                    if (index < customFields.size - 1) {
                        Spacer(Modifier.height(6.dp))
                        AuroraDivider()
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = newFieldLabel,
                onValueChange = { newFieldLabel = it },
                label = { Text("Field label (e.g. Gate code)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newFieldValue,
                onValueChange = { newFieldValue = it },
                label = { Text("Value") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = auroraFieldColors()
            )
            Spacer(Modifier.height(8.dp))
            AuroraSecondaryCta(
                text = "Add Field",
                testTag = "customer_add_field",
                onClick = {
                    if (newFieldLabel.isNotBlank() && newFieldValue.isNotBlank()) {
                        customFields = customFields + CustomField(label = newFieldLabel.trim(), value = newFieldValue.trim())
                        newFieldLabel = ""
                        newFieldValue = ""
                    }
                }
            )
        }

        // Jobs / history — only when the data exists.
        AuroraCard {
            AuroraSectionHeader(title = "Jobs & History")
            Spacer(Modifier.height(8.dp))
            if (jobs.isEmpty()) {
                Text(
                    text = "No jobs for this customer yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AuroraTextSecondary
                )
            } else {
                jobs.forEach { job ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(job.title.ifBlank { "Job" }, style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                        val whenText = listOf(job.scheduledDate, job.scheduledTime).filter { it.isNotBlank() }.joinToString(" ")
                        Text(
                            text = listOf(JobStatus.fromCode(job.status).label, whenText).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = AuroraTextSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // AI follow-up — full-width two-line button, so the label can
        // no longer clip the way the old dialog's button did.
        OutlinedButton(
            onClick = onDraftFollowUp,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AuroraBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AuroraCardSurface,
                contentColor = AuroraTextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_ai_followup")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AI Draft Follow-Up", style = MaterialTheme.typography.titleSmall, color = AuroraTextPrimary)
                Text("Queues in Approvals for your review", style = MaterialTheme.typography.labelSmall, color = AuroraTextSecondary)
            }
        }

        if (aiDraftPreview != null) {
            AuroraCard(highlighted = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = AuroraEmerald, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Queued in Approvals:", style = MaterialTheme.typography.labelSmall, color = AuroraEmerald)
                }
                Spacer(Modifier.height(4.dp))
                Text(aiDraftPreview, style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary)
            }
        }

        if (customer.status == CustomerStatus.LEAD.code) {
            TextButton(onClick = { onUpdateStatus(CustomerStatus.ACTIVE.code) }) {
                Text("Convert Lead to Active Customer", color = AuroraEmerald)
            }
        } else if (customer.status != CustomerStatus.INACTIVE.code) {
            TextButton(onClick = { onUpdateStatus(CustomerStatus.INACTIVE.code) }) {
                Text("Archive Customer", color = AuroraTextMuted)
            }
        }

        AuroraPrimaryCta(
            text = "Save Customer",
            testTag = "customer_save_button",
            onClick = { onSave(phone, email, address, notes, customFields) }
        )
        Spacer(Modifier.height(8.dp))
    }
}
