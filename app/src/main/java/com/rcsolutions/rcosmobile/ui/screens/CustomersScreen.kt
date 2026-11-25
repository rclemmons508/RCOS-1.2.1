package com.rcsolutions.rcosmobile.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.rcsolutions.rcosmobile.model.Customer
import com.rcsolutions.rcosmobile.model.CustomerStatus
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraEmptyState
import com.rcsolutions.rcosmobile.ui.components.AuroraFilterPill
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.theme.AuroraCardElevated
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
    onBack: (() -> Unit)? = null,
    onAddCustomer: (name: String, company: String, phone: String, email: String, address: String, status: String, serviceType: String, notes: String) -> Unit,
    onUpdateCustomerStatus: (Customer, String) -> Unit,
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
        AlertDialog(
            onDismissRequest = { selectedCustomer = null; aiDraftPreview = null },
            containerColor = AuroraCardElevated,
            title = { Text(cust.name, color = AuroraTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = CustomerStatus.fromCode(cust.status).label,
                        style = MaterialTheme.typography.labelMedium,
                        color = AuroraEmerald
                    )
                    if (cust.phone.isNotBlank()) Text("Phone: ${cust.phone}", color = AuroraTextSecondary)
                    if (cust.email.isNotBlank()) Text("Email: ${cust.email}", color = AuroraTextSecondary)
                    if (cust.address.isNotBlank()) Text("Address: ${cust.address}", color = AuroraTextSecondary)
                    if (cust.aiSummary.isNotBlank()) Text("Notes: ${cust.aiSummary}", color = AuroraTextPrimary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (cust.phone.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))) }
                                },
                                modifier = Modifier.clip(CircleShape).background(AuroraEmerald.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Outlined.Phone, contentDescription = "Call customer", tint = AuroraEmerald)
                            }
                            IconButton(
                                onClick = {
                                    val msgBody = aiDraftPreview ?: "Hi ${cust.name}, following up on your service."
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${cust.phone}")).apply {
                                        putExtra("sms_body", msgBody)
                                    }
                                    runCatching { context.startActivity(smsIntent) }
                                },
                                modifier = Modifier.clip(CircleShape).background(AuroraEmerald.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Outlined.Message, contentDescription = "Text customer", tint = AuroraEmerald)
                            }
                        }
                        if (cust.email.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${cust.email}"))
                                    runCatching { context.startActivity(mailIntent) }
                                },
                                modifier = Modifier.clip(CircleShape).background(AuroraEmerald.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Outlined.Email, contentDescription = "Email customer", tint = AuroraEmerald)
                            }
                        }
                    }

                    AuroraSecondaryCta(
                        text = "AI Draft Follow-Up (Queues Approval)",
                        testTag = "customer_ai_followup",
                        onClick = {
                            onDraftFollowUp(cust) { draft -> aiDraftPreview = draft }
                        }
                    )

                    if (aiDraftPreview != null) {
                        AuroraCard(highlighted = true) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = AuroraEmerald, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Queued in Approvals:", style = MaterialTheme.typography.labelSmall, color = AuroraEmerald)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(aiDraftPreview.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = AuroraTextPrimary)
                        }
                    }

                    if (cust.status == CustomerStatus.LEAD.code) {
                        TextButton(onClick = {
                            onUpdateCustomerStatus(cust, CustomerStatus.ACTIVE.code)
                            selectedCustomer = null
                        }) {
                            Text("Convert Lead to Active Customer", color = AuroraEmerald)
                        }
                    } else if (cust.status != CustomerStatus.INACTIVE.code) {
                        TextButton(onClick = {
                            onUpdateCustomerStatus(cust, CustomerStatus.INACTIVE.code)
                            selectedCustomer = null
                        }) {
                            Text("Archive Customer", color = AuroraTextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomer = null; aiDraftPreview = null }) {
                    Text("Close", color = AuroraEmerald)
                }
            }
        )
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
