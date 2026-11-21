package com.rcsolutions.rcosmobile.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.rcsolutions.rcosmobile.data.AiRepository
import com.rcsolutions.rcosmobile.data.LocalPrefsState
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.data.RcosRepository
import com.rcsolutions.rcosmobile.data.UserPreferencesRepository
import com.rcsolutions.rcosmobile.data.WeatherService
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.ActionStatus
import com.rcsolutions.rcosmobile.model.AgentInboxMessage
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.AutonomyMode
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.CallDirection
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.CallStatus
import com.rcsolutions.rcosmobile.model.ChatMessage
import com.rcsolutions.rcosmobile.model.Customer
import com.rcsolutions.rcosmobile.model.CustomerRequest
import com.rcsolutions.rcosmobile.model.FieldTask
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobPriority
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.model.OnboardingExtraction
import com.rcsolutions.rcosmobile.model.QuoteDraftSuggestion
import com.rcsolutions.rcosmobile.model.TaskBucket
import com.rcsolutions.rcosmobile.model.TaskCategory
import com.rcsolutions.rcosmobile.model.TeamMessage
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.model.VoicemailRecord
import com.rcsolutions.rcosmobile.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "MainViewModel"
private const val STOP_TIMEOUT_MS = 5000L

data class AiConsoleState(
    val selectedEmployeeId: String = "executive_assistant",
    val isStreaming: Boolean = false,
    val streamingText: String = "",
    val error: String? = null,
    val lastQuoteSuggestion: QuoteDraftSuggestion? = null,
    val isGeneratingQuote: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: RcosRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val aiRepo: AiRepository,
    private val weatherService: WeatherService,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
) : ViewModel() {

    val localPrefs: StateFlow<LocalPrefsState> = prefsRepo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LocalPrefsState())

    private val reloadTrigger = MutableStateFlow(0)

    val businessesState: StateFlow<UiState<List<BusinessAccount>>> =
        reloadTrigger.flatMapLatest {
            repository.observeBusinesses(currentUserId)
                .mapToUiState("Failed to load business workspaces")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun retryLoadWorkspaces() {
        reloadTrigger.value += 1
    }

    val activeBusiness: StateFlow<BusinessAccount?> =
        combine(businessesState, localPrefs) { bizUi, prefs ->
            val list = (bizUi as? UiState.Success)?.data.orEmpty()
            list.firstOrNull { it.id == prefs.activeBusinessId } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val activeBusinessIdFlow: Flow<String> = activeBusiness
        .map { it?.id.orEmpty() }
        .distinctUntilChanged()

    val jobsState: StateFlow<UiState<List<Job>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeJobs(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load scheduled jobs")

    val customersState: StateFlow<UiState<List<Customer>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeCustomers(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load customers")

    val tasksState: StateFlow<UiState<List<FieldTask>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeTasks(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load tasks")

    val callsState: StateFlow<UiState<List<CallRecord>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeCalls(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load calls")

    val voicemailsState: StateFlow<UiState<List<VoicemailRecord>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeVoicemails(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load voicemails")

    val requestsState: StateFlow<UiState<List<CustomerRequest>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeRequests(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load requests")

    val actionsState: StateFlow<UiState<List<ActionRecord>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeActions(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load approvals & quotes")

    val chatMessagesState: StateFlow<UiState<List<ChatMessage>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeChatMessages(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load assistant chat")

    val teamMessagesState: StateFlow<UiState<List<TeamMessage>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeTeamMessages(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load team chat")

    val agentMessagesState: StateFlow<UiState<List<AgentInboxMessage>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeAgentMessages(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load agent alerts")

    val auditLogsState: StateFlow<UiState<List<AuditLog>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeAuditLogs(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load audit log")

    private val _weatherState = MutableStateFlow<WeatherSnapshot?>(null)
    val weatherState: StateFlow<WeatherSnapshot?> = _weatherState.asStateFlow()

    private val _aiState = MutableStateFlow(AiConsoleState())
    val aiState: StateFlow<AiConsoleState> = _aiState.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _onboardingExtraction = MutableStateFlow<UiState<OnboardingExtraction>?>(null)
    val onboardingExtraction: StateFlow<UiState<OnboardingExtraction>?> = _onboardingExtraction.asStateFlow()

    init {
        viewModelScope.launch {
            combine(localPrefs, activeBusiness) { prefs, biz ->
                prefs.weatherCity.ifBlank { biz?.serviceArea.orEmpty() }
            }.distinctUntilChanged().collect { city ->
                _weatherState.value = if (city.isNotBlank()) weatherService.fetchCurrentWeather(city) else null
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showBanner(msg: String) {
        _bannerMessage.value = msg
    }

    private fun <T> Flow<List<T>>.mapToUiState(fallbackError: String): Flow<UiState<List<T>>> =
        map<List<T>, UiState<List<T>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, fallbackError, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }

    private fun <T> Flow<List<T>>.toUiStateFlow(fallbackError: String): StateFlow<UiState<List<T>>> =
        mapToUiState(fallbackError)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    fun switchBusiness(businessId: String) {
        viewModelScope.launch {
            prefsRepo.setActiveBusinessId(businessId)
        }
    }

    fun analyzeOnboardingInput(descriptionOrDoc: String, bitmap: Bitmap?) {
        if (descriptionOrDoc.isBlank() && bitmap == null) {
            _bannerMessage.value = "Describe your company or attach a flyer/document first."
            return
        }
        viewModelScope.launch {
            _onboardingExtraction.value = UiState.Loading
            aiRepo.extractOnboardingProfile(descriptionOrDoc, bitmap)
                .onSuccess { extracted ->
                    _onboardingExtraction.value = UiState.Success(extracted)
                }
                .onFailure { e ->
                    _onboardingExtraction.value = UiState.Error(e.localizedMessage ?: "AI analysis failed.")
                }
        }
    }

    fun clearOnboardingExtraction() {
        _onboardingExtraction.value = null
    }

    fun saveNewBusiness(
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
        onCreated: () -> Unit
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _bannerMessage.value = "Business name is required."
            return
        }
        val preset = RcosDefaults.findPreset(industryId)
        val activeEmps = RcosDefaults.AI_EMPLOYEES.associate { emp ->
            emp.id to (emp.id in preset.suggestedEmployees)
        }
        val biz = BusinessAccount(
            ownerId = currentUserId,
            memberUids = listOf(currentUserId),
            name = cleanName,
            industry = preset.id,
            services = services.ifEmpty { preset.defaultServices },
            serviceArea = serviceArea.trim(),
            businessHours = businessHours.trim().ifBlank { "Mon–Fri 8:00 AM – 6:00 PM" },
            pricingApproach = pricingApproach.trim().ifBlank { "Clear written quotes before starting work" },
            brandTone = brandTone.trim().ifBlank { preset.recommendedTone },
            businessKnowledge = knowledgeText.trim(),
            autonomyMode = autonomyMode.code,
            answeringMode = "ai",
            aiReceptionistEnabled = true,
            dollarThreshold = dollarThreshold.coerceIn(0.0, 1_000_000.0),
            emergencyAvailable = emergencyAvailable,
            activeEmployees = activeEmps,
            onboardingCompleted = true,
            contactName = currentUserName.ifBlank { cleanName },
            contactEmail = currentUserEmail,
            ownerEmail = currentUserEmail,
        )
        viewModelScope.launch {
            _isMutating.value = true
            repository.createBusiness(biz)
                .onSuccess { newId ->
                    prefsRepo.setActiveBusinessId(newId)
                    if (serviceArea.isNotBlank()) prefsRepo.setWeatherCity(serviceArea)
                    // Seed a welcoming proactive briefing from Executive Assistant in Firestore
                    val createdBiz = biz.copy(id = newId)
                    repository.saveAgentMessage(
                        createdBiz,
                        AgentInboxMessage(
                            businessId = newId,
                            agentId = "executive_assistant",
                            kind = "suggestion",
                            title = "${cleanName} workspace ready",
                            body = "Your ${preset.name} team is configured with ${biz.services.size} services and an approval threshold of $${dollarThreshold.toInt()}. Start by adding a customer, creating a quote, or chatting with AI.",
                            status = "unread",
                            actionLabel = "Open AI",
                            actionRoute = "ai"
                        )
                    )
                    _bannerMessage.value = "$cleanName workspace created."
                    onCreated()
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Failed to create workspace."
                }
            _isMutating.value = false
        }
    }

    fun updateBusinessProfile(updates: Map<String, Any>, successMsg: String = "Workspace updated.") {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            repository.updateBusinessFields(biz.id, updates)
                .onSuccess {
                    (updates["serviceArea"] as? String)?.let { area ->
                        if (area.isNotBlank()) prefsRepo.setWeatherCity(area)
                    }
                    _bannerMessage.value = successMsg
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Update failed."
                }
            _isMutating.value = false
        }
    }

    fun toggleAiReceptionist(enabled: Boolean) {
        updateBusinessProfile(
            mapOf(
                "aiReceptionistEnabled" to enabled,
                "answeringMode" to if (enabled) "ai" else "manual"
            ),
            successMsg = if (enabled) "AI Receptionist live & ready." else "AI Receptionist paused (manual ring)."
        )
    }

    fun toggleEmployee(employeeId: String, enabled: Boolean) {
        val biz = activeBusiness.value ?: return
        val updated = biz.activeEmployees.toMutableMap().apply { put(employeeId, enabled) }
        updateBusinessProfile(
            mapOf("activeEmployees" to updated),
            successMsg = "${RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == employeeId }?.name ?: "Agent"} ${if (enabled) "enabled" else "paused"}."
        )
    }

    fun updateEmployeeTone(employeeId: String, tone: String) {
        val biz = activeBusiness.value ?: return
        val updated = biz.employeeTones.toMutableMap().apply { put(employeeId, tone) }
        updateBusinessProfile(mapOf("employeeTones" to updated), "Voice tone saved.")
    }

    fun deleteActiveWorkspace(onDeleted: () -> Unit) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            repository.deleteBusinessWorkspace(biz.id)
                .onSuccess {
                    _bannerMessage.value = "Workspace deleted permanently."
                    onDeleted()
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Could not delete workspace."
                }
            _isMutating.value = false
        }
    }

    // ---- AI Assistant Chat ----
    fun selectAiEmployee(employeeId: String) {
        _aiState.value = _aiState.value.copy(selectedEmployeeId = employeeId)
    }

    fun sendAiMessage(
        userPrompt: String,
        attachedDocumentText: String = "",
        attachedBitmap: Bitmap? = null
    ) {
        val biz = activeBusiness.value ?: run {
            _bannerMessage.value = "Complete workspace setup first to chat with your AI team."
            return
        }
        val clean = userPrompt.trim()
        if (clean.isEmpty() && attachedBitmap == null) return
        val effectivePrompt = clean.ifEmpty { "Please analyze this job site photo and suggest next steps." }
        val employeeId = _aiState.value.selectedEmployeeId

        viewModelScope.launch {
            _aiState.value = _aiState.value.copy(isStreaming = true, streamingText = "", error = null)
            repository.saveChatMessage(
                biz,
                ChatMessage(
                    businessId = biz.id,
                    senderId = currentUserId,
                    senderName = currentUserName.ifBlank { "Owner" },
                    employeeId = employeeId,
                    role = "user",
                    text = effectivePrompt
                )
            )
            try {
                var finalReply = ""
                aiRepo.streamAssistantReply(
                    prompt = effectivePrompt,
                    business = biz,
                    employeeId = employeeId,
                    customers = (customersState.value as? UiState.Success)?.data.orEmpty(),
                    jobs = (jobsState.value as? UiState.Success)?.data.orEmpty(),
                    tasks = (tasksState.value as? UiState.Success)?.data.orEmpty(),
                    calls = (callsState.value as? UiState.Success)?.data.orEmpty(),
                    actions = (actionsState.value as? UiState.Success)?.data.orEmpty(),
                    attachedDocumentText = attachedDocumentText,
                    attachedBitmap = attachedBitmap
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = _aiState.value.copy(streamingText = partial)
                }

                if (finalReply.isNotBlank()) {
                    repository.saveChatMessage(
                        biz,
                        ChatMessage(
                            businessId = biz.id,
                            senderId = employeeId,
                            senderName = RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == employeeId }?.name ?: "AI Assistant",
                            employeeId = employeeId,
                            role = "assistant",
                            text = finalReply,
                            modelUsed = "gemini-3.8-flash"
                        )
                    )
                }
                _aiState.value = _aiState.value.copy(isStreaming = false, streamingText = "")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Assistant error", e)
                _aiState.value = _aiState.value.copy(
                    isStreaming = false,
                    streamingText = "",
                    error = e.localizedMessage ?: "AI Assistant encountered an error."
                )
            }
        }
    }

    // ---- Quotes / Invoices / Approvals ----
    fun draftQuoteWithAi(clientName: String, serviceType: String, scopeNotes: String) {
        val biz = activeBusiness.value ?: return
        if (clientName.isBlank() || serviceType.isBlank()) {
            _bannerMessage.value = "Customer name and service type are required."
            return
        }
        viewModelScope.launch {
            _aiState.value = _aiState.value.copy(isGeneratingQuote = true, error = null)
            aiRepo.generateQuoteDraft(biz, clientName.trim(), serviceType.trim(), scopeNotes.trim())
                .onSuccess { draft ->
                    _aiState.value = _aiState.value.copy(isGeneratingQuote = false, lastQuoteSuggestion = draft)
                }
                .onFailure { e ->
                    _aiState.value = _aiState.value.copy(isGeneratingQuote = false)
                    _bannerMessage.value = e.localizedMessage ?: "AI quote draft failed."
                }
        }
    }

    fun clearQuoteSuggestion() {
        _aiState.value = _aiState.value.copy(lastQuoteSuggestion = null)
    }

    fun submitQuoteOrInvoice(
        clientName: String,
        title: String,
        summary: String,
        amount: Double,
        draftContent: String,
        isInvoice: Boolean = false,
        linkedJobId: String = ""
    ) {
        val biz = activeBusiness.value ?: return
        if (clientName.isBlank() || title.isBlank()) {
            _bannerMessage.value = "Customer name and title are required."
            return
        }
        val needsApproval = biz.autonomyMode == AutonomyMode.SUPERVISED.code ||
            amount >= biz.dollarThreshold || isInvoice
        val status = if (needsApproval) ActionStatus.AWAITING_APPROVAL.code else ActionStatus.APPROVED.code
        val invNumber = if (isInvoice) "INV-${System.currentTimeMillis().toString().takeLast(5)}" else ""
        val dueDate = if (isInvoice) {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(System.currentTimeMillis() + 14L * 86400_000L))
        } else ""

        viewModelScope.launch {
            _isMutating.value = true
            repository.createAction(
                biz,
                ActionRecord(
                    businessId = biz.id,
                    employeeId = if (isInvoice) "finance" else "sales",
                    actionType = if (isInvoice) "send_invoice" else "generate_quote",
                    title = title.trim(),
                    summary = summary.trim().ifBlank { draftContent.take(180) },
                    clientName = clientName.trim(),
                    status = status,
                    riskCategory = if (isInvoice) "money_movement" else "commitment_outbound",
                    requiresApproval = needsApproval,
                    dollarAmount = amount.coerceAtLeast(0.0),
                    draftContent = draftContent.trim(),
                    invoiceNumber = invNumber,
                    dueDate = dueDate,
                    paymentStatus = "unpaid",
                    scheduledJobId = linkedJobId
                )
            ).onSuccess {
                _aiState.value = _aiState.value.copy(lastQuoteSuggestion = null)
                _bannerMessage.value = if (needsApproval) {
                    "${if (isInvoice) "Invoice" else "Quote"} queued for your approval."
                } else {
                    "Quote auto-approved under $${biz.dollarThreshold.toInt()} limit."
                }
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to save item."
            }
            _isMutating.value = false
        }
    }

    fun decideApproval(
        action: ActionRecord,
        approved: Boolean,
        editedDraft: String? = null,
        editedAmount: Double? = null
    ) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            val decision = if (approved) ActionStatus.APPROVED.code else ActionStatus.REJECTED.code
            repository.decideActionApproval(biz, action, decision, editedDraft, editedAmount)
                .onSuccess {
                    _bannerMessage.value = if (approved) "Approved & logged." else "Declined & logged."
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "Approval update failed."
                }
            _isMutating.value = false
        }
    }

    fun markInvoicePaid(actionId: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.markInvoicePaid(biz.id, actionId)
                .onSuccess { _bannerMessage.value = "Invoice marked paid." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Update failed." }
        }
    }

    // ---- Tasks ----
    fun addTask(
        title: String,
        subtitle: String,
        dueTimeLabel: String,
        bucket: TaskBucket,
        category: TaskCategory,
        customerName: String = ""
    ) {
        val biz = activeBusiness.value ?: return
        if (title.isBlank()) {
            _bannerMessage.value = "Task title is required."
            return
        }
        viewModelScope.launch {
            repository.createTask(
                biz,
                FieldTask(
                    businessId = biz.id,
                    title = title.trim(),
                    subtitle = subtitle.trim(),
                    dueTimeLabel = dueTimeLabel.trim().ifBlank { "Today" },
                    bucket = bucket.code,
                    category = category.code,
                    completed = false,
                    relatedCustomerName = customerName.trim()
                )
            ).onSuccess {
                _bannerMessage.value = "Task added."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to add task."
            }
        }
    }

    fun toggleTask(task: FieldTask) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.toggleTaskCompleted(biz.id, task.id, !task.completed)
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to update task." }
        }
    }

    fun deleteTask(taskId: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.deleteTask(biz.id, taskId)
                .onSuccess { _bannerMessage.value = "Task removed." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Delete failed." }
        }
    }

    // ---- Jobs / Schedule ----
    fun addJob(
        title: String,
        clientName: String,
        clientPhone: String,
        address: String,
        scheduledDate: String,
        scheduledTime: String,
        durationLabel: String,
        category: String,
        estimatedValue: Double,
        priority: JobPriority,
        description: String
    ) {
        val biz = activeBusiness.value ?: return
        if (title.isBlank() || clientName.isBlank()) {
            _bannerMessage.value = "Appointment title and customer name are required."
            return
        }
        viewModelScope.launch {
            repository.createJob(
                biz,
                Job(
                    businessId = biz.id,
                    title = title.trim(),
                    clientName = clientName.trim(),
                    clientPhone = clientPhone.trim(),
                    address = address.trim(),
                    status = JobStatus.DISPATCHED.code,
                    priority = priority.code,
                    assignedTechnician = RcosDefaults.findPreset(biz.industry).technicianRoleName,
                    estimatedValue = estimatedValue.coerceAtLeast(0.0),
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime.ifBlank { "9:00 AM" },
                    durationLabel = durationLabel.ifBlank { "1h" },
                    category = category.ifBlank { biz.services.firstOrNull() ?: "Service" },
                    description = description.trim()
                )
            ).onSuccess {
                _bannerMessage.value = "Appointment scheduled."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to schedule appointment."
            }
        }
    }

    fun updateJobStatus(job: Job, status: JobStatus) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.updateJobStatus(biz.id, job.id, status.code)
                .onSuccess { _bannerMessage.value = "Job marked ${status.label}." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to update job." }
        }
    }

    // ---- Customers ----
    fun addCustomer(
        name: String,
        company: String,
        phone: String,
        email: String,
        address: String,
        status: String,
        serviceType: String,
        notes: String
    ) {
        val biz = activeBusiness.value ?: return
        if (name.isBlank()) {
            _bannerMessage.value = "Customer name is required."
            return
        }
        viewModelScope.launch {
            repository.createCustomer(
                biz,
                Customer(
                    businessId = biz.id,
                    name = name.trim(),
                    company = company.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    address = address.trim(),
                    status = status,
                    healthScore = 92L,
                    lastContactDate = SimpleDateFormat("MMM dd", Locale.US).format(Date()),
                    serviceType = serviceType.trim(),
                    aiSummary = notes.trim()
                )
            ).onSuccess {
                _bannerMessage.value = "$name added to Customers."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to add customer."
            }
        }
    }

    fun updateCustomerStatus(customer: Customer, status: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.updateCustomer(biz.id, customer.id, mapOf("status" to status))
                .onSuccess { _bannerMessage.value = "Customer updated." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to update customer." }
        }
    }

    fun draftCustomerFollowUpAndSave(customer: Customer, onDraftReady: (String) -> Unit) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            aiRepo.generateCustomerFollowUp(biz, customer)
                .onSuccess { draft ->
                    repository.createAction(
                        biz,
                        ActionRecord(
                            businessId = biz.id,
                            employeeId = "customer_service",
                            actionType = "send_message",
                            title = "Follow-Up Message for ${customer.name}",
                            summary = draft.take(180),
                            clientName = customer.name,
                            status = ActionStatus.AWAITING_APPROVAL.code,
                            riskCategory = "routine_outbound",
                            requiresApproval = true,
                            dollarAmount = 0.0,
                            draftContent = draft
                        )
                    )
                    onDraftReady(draft)
                }
                .onFailure { e ->
                    _bannerMessage.value = e.localizedMessage ?: "AI follow-up failed."
                }
            _isMutating.value = false
        }
    }

    // ---- Calls & Voicemails & Requests ----
    fun logCall(
        callerName: String,
        callerNumber: String,
        direction: CallDirection,
        status: CallStatus,
        durationLabel: String,
        summary: String
    ) {
        val biz = activeBusiness.value ?: return
        if (callerName.isBlank() || callerNumber.isBlank()) {
            _bannerMessage.value = "Caller name and phone number are required."
            return
        }
        val timeLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
        viewModelScope.launch {
            repository.createCallRecord(
                biz,
                CallRecord(
                    businessId = biz.id,
                    callerName = callerName.trim(),
                    callerNumber = callerNumber.trim(),
                    direction = direction.code,
                    status = status.code,
                    durationLabel = durationLabel.ifBlank { "1 min" },
                    timeLabel = timeLabel,
                    summary = summary.trim(),
                    answeredBy = if (biz.aiReceptionistEnabled) "ai_receptionist" else "human_operator"
                )
            ).onSuccess {
                _bannerMessage.value = "Call logged."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to log call."
            }
        }
    }

    fun markVoicemailReviewed(voicemailId: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.markVoicemailReviewed(biz.id, voicemailId, true)
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Update failed." }
        }
    }

    fun addCustomerRequest(
        name: String,
        phone: String,
        email: String,
        location: String,
        serviceType: String,
        urgency: String,
        notes: String
    ) {
        val biz = activeBusiness.value ?: return
        if (name.isBlank()) {
            _bannerMessage.value = "Customer name is required."
            return
        }
        viewModelScope.launch {
            repository.createRequest(
                biz,
                CustomerRequest(
                    businessId = biz.id,
                    customerName = name.trim(),
                    customerPhone = phone.trim(),
                    customerEmail = email.trim(),
                    serviceLocation = location.trim(),
                    serviceType = serviceType.ifBlank { biz.services.firstOrNull() ?: "General Service" },
                    description = notes.trim(),
                    urgency = urgency
                )
            ).onSuccess {
                _bannerMessage.value = "Service request logged."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to save request."
            }
        }
    }

    // ---- Team Chat & Agent Alerts & Preferences ----
    fun sendTeamMessage(text: String) {
        val biz = activeBusiness.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.saveTeamMessage(biz, text)
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to send team message." }
        }
    }

    fun dismissAgentMessage(id: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.updateAgentMessageStatus(biz.id, id, "dismissed")
        }
    }

    fun updateFocusQuote(quote: String) {
        viewModelScope.launch {
            prefsRepo.setFocusQuote(quote)
            _bannerMessage.value = "Focus quote updated."
        }
    }

    fun updateWeatherCity(city: String) {
        viewModelScope.launch {
            prefsRepo.setWeatherCity(city)
        }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setBiometricsEnabled(enabled)
            _bannerMessage.value = if (enabled) "Biometric lock enabled." else "Biometric lock disabled."
        }
    }

    fun updateNotificationPrefs(push: Boolean, sound: Boolean, vibration: Boolean, quietHours: Boolean) {
        viewModelScope.launch {
            prefsRepo.setNotificationSettings(push, sound, vibration, quietHours)
            _bannerMessage.value = "Notification preferences saved."
        }
    }

    fun toggleAutomationMaster(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutomationMaster(enabled)
        }
    }

    fun toggleAutomationRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.toggleAutomationRule(ruleId, enabled)
        }
    }

    suspend fun clearPrefsBeforeSignOut() {
        prefsRepo.clearAllOnSignOut()
    }
}
