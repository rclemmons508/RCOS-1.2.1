package com.rcsolutions.rcosmobile.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.rcsolutions.rcosmobile.data.AiRepository
import com.rcsolutions.rcosmobile.data.DocumentInput
import com.rcsolutions.rcosmobile.data.extractDocumentInput
import com.rcsolutions.rcosmobile.data.isAppCheckFailure
import com.rcsolutions.rcosmobile.data.ApiResult
import com.rcsolutions.rcosmobile.data.AssistantDocument
import com.rcsolutions.rcosmobile.data.ChatReply
import com.rcsolutions.rcosmobile.data.ConnectionState
import com.rcsolutions.rcosmobile.data.ConnectivityMonitor
import com.rcsolutions.rcosmobile.data.DiagnosticsScan
import com.rcsolutions.rcosmobile.data.GmailConnection
import com.rcsolutions.rcosmobile.data.GmailOAuthStart
import com.rcsolutions.rcosmobile.data.LiveCall
import com.rcsolutions.rcosmobile.data.ServerApiClient
import com.rcsolutions.rcosmobile.data.ServerPhoneCall
import com.rcsolutions.rcosmobile.data.ServerStatus
import com.rcsolutions.rcosmobile.data.LiveKitVoiceManager
import com.rcsolutions.rcosmobile.data.OUTBOUND_SETUP_MESSAGE
import com.rcsolutions.rcosmobile.data.OutboundCallManager
import com.rcsolutions.rcosmobile.data.OutboundStatus
import com.rcsolutions.rcosmobile.data.MODEL_GENERAL
import com.rcsolutions.rcosmobile.data.MODEL_LIVEKIT_VOICE
import com.rcsolutions.rcosmobile.data.LocalPrefsState
import com.rcsolutions.rcosmobile.data.RcosDefaults
import com.rcsolutions.rcosmobile.data.RcosRepository
import com.rcsolutions.rcosmobile.data.UserPreferencesRepository
import com.rcsolutions.rcosmobile.data.WeatherService
import com.rcsolutions.rcosmobile.data.toBusinessSyncRequest
import com.rcsolutions.rcosmobile.notifications.registerPushTokenForBusiness
import com.rcsolutions.rcosmobile.security.readDebugSecret
import com.rcsolutions.rcosmobile.model.ActionRecord
import com.rcsolutions.rcosmobile.model.ActionStatus
import com.rcsolutions.rcosmobile.model.AgentInboxMessage
import com.rcsolutions.rcosmobile.model.AgentSettings
import com.rcsolutions.rcosmobile.model.AiTaskProfile
import com.rcsolutions.rcosmobile.model.AuditLog
import com.rcsolutions.rcosmobile.model.AutonomyMode
import com.rcsolutions.rcosmobile.model.BusinessAccount
import com.rcsolutions.rcosmobile.model.BusinessMember
import com.rcsolutions.rcosmobile.model.CallDirection
import com.rcsolutions.rcosmobile.model.CallRecord
import com.rcsolutions.rcosmobile.model.CallStatus
import com.rcsolutions.rcosmobile.model.ChatMessage
import com.rcsolutions.rcosmobile.model.Customer
import com.rcsolutions.rcosmobile.model.CustomerRequest
import com.rcsolutions.rcosmobile.model.CustomerStatus
import com.rcsolutions.rcosmobile.model.CustomField
import com.rcsolutions.rcosmobile.model.FieldTask
import com.rcsolutions.rcosmobile.model.Invoice
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.JobPriority
import com.rcsolutions.rcosmobile.model.JobStatus
import com.rcsolutions.rcosmobile.model.LiveVoiceStatus
import com.rcsolutions.rcosmobile.model.LiveVoiceUiState
import com.rcsolutions.rcosmobile.model.OnboardingExtraction
import com.rcsolutions.rcosmobile.model.OutboundCallStatus
import com.rcsolutions.rcosmobile.model.OutboundCallUiState
import com.rcsolutions.rcosmobile.model.Quote
import com.rcsolutions.rcosmobile.model.QuoteDraftSuggestion
import com.rcsolutions.rcosmobile.model.TaskBucket
import com.rcsolutions.rcosmobile.model.TaskCategory
import com.rcsolutions.rcosmobile.model.TeamMessage
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.model.UserProfile
import com.rcsolutions.rcosmobile.model.VoicemailRecord
import com.rcsolutions.rcosmobile.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    val selectedTaskProfile: AiTaskProfile = AiTaskProfile.AUTO,
    val activeModelId: String = MODEL_GENERAL,
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
    private val liveKitVoiceManager: LiveKitVoiceManager,
    private val outboundCallManager: OutboundCallManager,
    private val weatherService: WeatherService,
    private val serverApi: ServerApiClient = ServerApiClient(),
    private val connectivityMonitor: ConnectivityMonitor? = null,
    private val appContext: Context,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
) : ViewModel() {

    val localPrefs: StateFlow<LocalPrefsState> = prefsRepo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LocalPrefsState())

    private val reloadTrigger = MutableStateFlow(0)

    /** 8 fixes Nov 26 (item 4) — bumping this re-subscribes the AI
     * chat listener, so a manual refresh (or a send) always shows the
     * latest messages instead of a stale snapshot. */
    private val chatReloadTrigger = MutableStateFlow(0)

    /** Business ids with a server re-sync in flight — the same guard
     * as the old app's `businessSyncInFlight` set, so concurrent
     * triggers (load + foreground) never double-send one business. */
    private val businessSyncInFlight = mutableSetOf<String>()

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

    private val _weeklyFetchedJobs = MutableStateFlow<Pair<String, List<Job>>?>(null)

    val jobsState: StateFlow<UiState<List<Job>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            _weeklyFetchedJobs.value = null
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeJobs(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load scheduled jobs")

    val weeklyDispatchJobsState: StateFlow<UiState<List<Job>>> =
        combine(activeBusinessIdFlow, jobsState, _weeklyFetchedJobs) { bizId, liveState, fetchedPair ->
            when (liveState) {
                is UiState.Success -> {
                    val liveList = liveState.data
                    val fetched = fetchedPair?.takeIf { it.first == bizId }?.second
                    if (fetched != null && fetched.size > liveList.size) {
                        UiState.Success(fetched)
                    } else {
                        liveState
                    }
                }
                is UiState.Loading -> {
                    val fetched = fetchedPair?.takeIf { it.first == bizId }?.second
                    if (fetched != null) UiState.Success(fetched) else UiState.Loading
                }
                is UiState.Error -> {
                    val fetched = fetchedPair?.takeIf { it.first == bizId }?.second
                    if (fetched != null) UiState.Success(fetched) else liveState
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

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

    val chatMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        combine(activeBusinessIdFlow, chatReloadTrigger) { bizId, _ -> bizId }
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

    val invoicesState: StateFlow<UiState<List<Invoice>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeInvoices(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load invoices")

    val quotesState: StateFlow<UiState<List<Quote>>> = activeBusinessIdFlow
        .flatMapLatest { bizId ->
            if (bizId.isBlank()) flowOf(emptyList())
            else repository.observeQuotes(bizId, currentUserId)
        }
        .toUiStateFlow("Failed to load quotes")

    // Job categories follow the business, never a hardcoded trade list:
    // the business's own services first, then its industry preset's
    // defaults. A business with no services and no preset gets an
    // honestly empty list (never a generic trade list).
    val jobCategories: StateFlow<List<String>> = activeBusiness
        .map { biz ->
            biz?.services?.ifEmpty {
                RcosDefaults.findPreset(biz.industry).defaultServices
            }.orEmpty()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    // Per-user profile (/users/{uid}): phone + contact email for the
    // signed-in person. Loaded once at start; absent until first save.
    private val _userProfileState = MutableStateFlow<UiState<UserProfile?>>(UiState.Loading)
    val userProfileState: StateFlow<UiState<UserProfile?>> = _userProfileState.asStateFlow()

    fun refreshUserProfile() {
        viewModelScope.launch {
            _userProfileState.value = try {
                UiState.Success(repository.getUserProfile(currentUserId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.localizedMessage ?: "Failed to load profile.")
            }
        }
    }

    private val _weatherState = MutableStateFlow<WeatherSnapshot?>(null)
    val weatherState: StateFlow<WeatherSnapshot?> = _weatherState.asStateFlow()

    private val _aiState = MutableStateFlow(AiConsoleState())
    val aiState: StateFlow<AiConsoleState> = _aiState.asStateFlow()

    private val _liveVoiceState = MutableStateFlow(LiveVoiceUiState())
    val liveVoiceState: StateFlow<LiveVoiceUiState> = _liveVoiceState.asStateFlow()

    /** Outbound dial-out call state (LiveKit dialer). The manager owns
     * the call; this mirrors it, and the overlay renders it. */
    private val _outboundCallState = MutableStateFlow(OutboundCallUiState())
    val outboundCallState: StateFlow<OutboundCallUiState> = _outboundCallState.asStateFlow()

    /** Outbound dialing availability on the server (trunk configured?).
     * Null = never checked; unknown never blocks a dial attempt. */
    private val _outboundStatus = MutableStateFlow<OutboundStatus?>(null)
    val outboundStatus: StateFlow<OutboundStatus?> = _outboundStatus.asStateFlow()

    /** Name/phone/note for the call in progress — logged when it ends
     * (only when it actually connected for > 0 seconds). */
    private var outboundLogInfo: Triple<String, String, String>? = null
    private var outboundLoggedForSeconds: Int = -1

    /** True while the live voice session is on the speakerphone. The
     * session always starts on the earpiece (see LiveKitVoiceManager);
     * this only flips after the user taps the toggle. */
    private val _isSpeakerphone = MutableStateFlow(false)
    val isSpeakerphone: StateFlow<Boolean> = _isSpeakerphone.asStateFlow()

    /** Server-side assistant documents for this business
     * (POST/GET/DELETE /api/assistant/documents). Uploaded from the
     * Assistant page; attached ones ground the next /api/chat call. */
    private val _documents = MutableStateFlow<List<AssistantDocument>>(emptyList())
    val documents: StateFlow<List<AssistantDocument>> = _documents.asStateFlow()

    private val _attachedDocumentIds = MutableStateFlow<Set<String>>(emptySet())
    val attachedDocumentIds: StateFlow<Set<String>> = _attachedDocumentIds.asStateFlow()

    private val _isUploadingDocument = MutableStateFlow(false)
    val isUploadingDocument: StateFlow<Boolean> = _isUploadingDocument.asStateFlow()

    private val _documentError = MutableStateFlow<String?>(null)
    val documentError: StateFlow<String?> = _documentError.asStateFlow()

    private var liveTimerJob: kotlinx.coroutines.Job? = null

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val _isOperationalSyncing = MutableStateFlow(false)
    val isOperationalSyncing: StateFlow<Boolean> = _isOperationalSyncing.asStateFlow()

    private val _lastOperationalSyncLabel = MutableStateFlow<String?>(null)
    val lastOperationalSyncLabel: StateFlow<String?> = _lastOperationalSyncLabel.asStateFlow()

    private val _operationalSyncError = MutableStateFlow<String?>(null)
    val operationalSyncError: StateFlow<String?> = _operationalSyncError.asStateFlow()

    // ---- Phase 2 - Railway server connection ----
    // Real connectivity from the device (never a fixed ONLINE label).
    val connectionState: StateFlow<ConnectionState> =
        connectivityMonitor?.state ?: MutableStateFlow(ConnectionState.UNKNOWN).asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus())
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _diagnosticsState = MutableStateFlow<UiState<DiagnosticsScan>?>(null)
    val diagnosticsState: StateFlow<UiState<DiagnosticsScan>?> = _diagnosticsState.asStateFlow()

    private val _gmailConnection = MutableStateFlow<GmailConnection>(GmailConnection.Unknown)
    val gmailConnection: StateFlow<GmailConnection> = _gmailConnection.asStateFlow()

    private val _gmailOAuthStart = MutableStateFlow<GmailOAuthStart?>(null)
    val gmailOAuthStart: StateFlow<GmailOAuthStart?> = _gmailOAuthStart.asStateFlow()

    private val _serverChatState = MutableStateFlow<UiState<ChatReply>?>(null)
    val serverChatState: StateFlow<UiState<ChatReply>?> = _serverChatState.asStateFlow()

    /** Probe the live server (public health check - no token needed).
     * Any HTTP answer proves the server is reachable; only a network
     * failure or a missing network means it is not. Never fakes success. */
    fun refreshServerStatus() {
        viewModelScope.launch {
            if (connectionState.value == ConnectionState.OFFLINE) {
                _serverStatus.value = ServerStatus(
                    reachable = false,
                    lastCheckedAtEpochMs = System.currentTimeMillis(),
                    detail = "No network connection.",
                )
                return@launch
            }
            _serverStatus.value = when (val result = serverApi.getHealth()) {
                is ApiResult.Success -> ServerStatus(
                    reachable = true,
                    lastCheckedAtEpochMs = System.currentTimeMillis(),
                    detail = "Server responding (${result.data.status}).",
                )
                is ApiResult.HttpError -> ServerStatus(
                    reachable = true,
                    lastCheckedAtEpochMs = System.currentTimeMillis(),
                    detail = "Server reachable, answered HTTP ${result.status}.",
                )
                is ApiResult.NetworkError -> ServerStatus(
                    reachable = false,
                    lastCheckedAtEpochMs = System.currentTimeMillis(),
                    detail = result.message,
                )
            }
        }
    }

    /** Run the server diagnostics scan (signed-in users only - the
     * server answers 401 when unsigned). */
    fun runDiagnostics() {
        viewModelScope.launch {
            _diagnosticsState.value = UiState.Loading
            _diagnosticsState.value = when (val result = serverApi.getDiagnostics()) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.HttpError -> UiState.Error(
                    if (result.status == 401) "Please sign in again to run diagnostics."
                    else result.message
                )
                is ApiResult.NetworkError -> UiState.Error(
                    "Server unreachable - check your connection and try again."
                )
            }
        }
    }

    /** Check whether the signed-in user has Gmail connected (their own
     * token only). 404 means not connected - a normal state, not an error. */
    fun checkGmailConnection() {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            when (val result = serverApi.getGmailToken(biz.id)) {
                is ApiResult.Success -> _gmailConnection.value = GmailConnection.Connected(result.data.email)
                is ApiResult.HttpError -> {
                    if (result.status == 404) {
                        _gmailConnection.value = GmailConnection.NotConnected
                    } else {
                        _gmailConnection.value = GmailConnection.Unknown
                        _bannerMessage.value = result.message
                    }
                }
                is ApiResult.NetworkError -> {
                    _gmailConnection.value = GmailConnection.Unknown
                    _bannerMessage.value = "Could not check Gmail - server unreachable."
                }
            }
        }
    }

    /** Start Gmail OAuth (Phase 6 opens the returned URL in the system
     * browser). The result is exposed via [gmailOAuthStart]. */
    fun startGmailOAuth() {
        val biz = activeBusiness.value ?: run {
            _bannerMessage.value = "Complete workspace setup first to connect Gmail."
            return
        }
        viewModelScope.launch {
            when (val result = serverApi.startGmailOAuth(biz.id)) {
                is ApiResult.Success -> _gmailOAuthStart.value = result.data
                is ApiResult.HttpError -> _bannerMessage.value = result.message
                is ApiResult.NetworkError -> _bannerMessage.value = "Could not start Gmail sign-in - server unreachable."
            }
        }
    }

    /**
     * POST /api/chat transport only (Phase 3 wires the Assistant UI).
     * Sends the given chat history to the server and exposes the real
     * reply - or the server's honest fallback - via [serverChatState].
     */
    fun sendChatViaServer(
        messages: List<ChatMessage>,
        roleId: String = "executive_assistant",
        modelTier: String = "general",
        documentIds: List<String> = emptyList(),
    ) {
        val biz = activeBusiness.value ?: run {
            _serverChatState.value = UiState.Error("Complete workspace setup first to chat with the server.")
            return
        }
        if (messages.isEmpty()) {
            _serverChatState.value = UiState.Error("Nothing to send yet.")
            return
        }
        viewModelScope.launch {
            _serverChatState.value = UiState.Loading
            _serverChatState.value = when (val result = serverApi.postChat(messages, roleId, modelTier, biz.id, documentIds)) {
                is ApiResult.Success -> {
                    if (result.data.isFallback) {
                        Log.w(TAG, "Server chat answered with its offline fallback (modelUsed=rcos-offline-engine).")
                    }
                    UiState.Success(result.data)
                }
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error("Server unreachable - check your connection and try again.")
            }
        }
    }

    /** Single entry point for pull-to-refresh gestures: reload Firestore
     * data (offline cache first) and re-probe the live server. Screen
     * gesture wiring lands with the Phase 3 UI work; the backing is
     * ready now. */
    fun refreshServerAndData() {
        retryLoadWorkspaces()
        refreshUserProfile()
        refreshServerStatus()
    }

    private val _isWeeklyTrendsRefreshing = MutableStateFlow(false)
    val isWeeklyTrendsRefreshing: StateFlow<Boolean> = _isWeeklyTrendsRefreshing.asStateFlow()

    private val _weeklyTrendsSyncLabel = MutableStateFlow<String?>(null)
    val weeklyTrendsSyncLabel: StateFlow<String?> = _weeklyTrendsSyncLabel.asStateFlow()

    private val _weeklyTrendsError = MutableStateFlow<String?>(null)
    val weeklyTrendsError: StateFlow<String?> = _weeklyTrendsError.asStateFlow()

    private val _onboardingExtraction = MutableStateFlow<UiState<OnboardingExtraction>?>(null)
    val onboardingExtraction: StateFlow<UiState<OnboardingExtraction>?> = _onboardingExtraction.asStateFlow()

    // True when the last AI extraction was rejected by Firebase App
    // Check (this build's debug token was never registered in the
    // console). The raw error tells the owner nothing, so the
    // onboarding screen shows the setup card with the token instead.
    // See security/AppCheckInit.readDebugSecret.
    private val _onboardingAppCheckError = MutableStateFlow(false)
    val onboardingAppCheckError: StateFlow<Boolean> = _onboardingAppCheckError.asStateFlow()

    // 8 fixes Nov 26 (item 8) — a general App Check failure notice.
    // Set whenever an App Check-protected AI call (follow-up draft,
    // quote draft, direct Gemini chat) is rejected, so the shell shows
    // the setup card with the debug token instead of the raw
    // "Firebase App Check token is invalid." banner. The onboarding
    // screen keeps its own, separate flag above.
    private val _appCheckNotice = MutableStateFlow(false)
    val appCheckNotice: StateFlow<Boolean> = _appCheckNotice.asStateFlow()

    // 8 fixes Nov 26 (item 1) — live calls the receptionist is
    // handling right now (GET /api/calls/live, polled every 6s while
    // the Calls screen is visible) and the server's finished-call log
    // (GET /api/phone/calls — the ONLY record of receptionist-answered
    // calls, which the server keeps in its own store, not Firestore).
    // Both start empty; an Error means the server could not be reached
    // and the UI shows an honest unavailable state, never fake calls.
    private val _liveCallsState = MutableStateFlow<UiState<List<LiveCall>>>(UiState.Success(emptyList()))
    val liveCallsState: StateFlow<UiState<List<LiveCall>>> = _liveCallsState.asStateFlow()

    private val _serverCallsState = MutableStateFlow<UiState<List<ServerPhoneCall>>>(UiState.Success(emptyList()))
    val serverCallsState: StateFlow<UiState<List<ServerPhoneCall>>> = _serverCallsState.asStateFlow()

    private var liveCallPollJob: kotlinx.coroutines.Job? = null

    /** 8 fixes Nov 26 (item 1) — start polling the server for live
     * calls and the finished-call log (same 6s cadence as the old app).
     * Called when the Calls screen appears; stopped when it leaves. */
    fun startLiveCallPolling() {
        if (liveCallPollJob?.isActive == true) return
        liveCallPollJob = viewModelScope.launch {
            while (true) {
                refreshLiveCalls()
                refreshServerCalls()
                delay(6_000L)
            }
        }
    }

    fun stopLiveCallPolling() {
        liveCallPollJob?.cancel()
        liveCallPollJob = null
    }

    fun refreshLiveCalls() {
        val bizId = activeBusiness.value?.id.orEmpty()
        if (bizId.isBlank()) return
        viewModelScope.launch {
            when (val result = serverApi.fetchLiveCalls(bizId)) {
                is ApiResult.Success -> _liveCallsState.value = UiState.Success(result.data)
                is ApiResult.HttpError -> _liveCallsState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> _liveCallsState.value = UiState.Error(result.message)
            }
        }
    }

    fun refreshServerCalls() {
        val bizId = activeBusiness.value?.id.orEmpty()
        if (bizId.isBlank()) return
        viewModelScope.launch {
            when (val result = serverApi.fetchServerCallLog(bizId)) {
                is ApiResult.Success -> _serverCallsState.value = UiState.Success(result.data)
                is ApiResult.HttpError -> _serverCallsState.value = UiState.Error(result.message)
                is ApiResult.NetworkError -> _serverCallsState.value = UiState.Error(result.message)
            }
        }
    }

    /** 8 fixes Nov 26 (item 1) — (re-)register this device's FCM
     * token for the active business so the old Railway server sends
     * the receptionist-answered alert here. Re-sent on launch and on
     * every foreground return, even when unchanged. Silent on failure:
     * push registration must never surface as a user-facing error. */
    fun registerPushToken() {
        val bizId = activeBusiness.value?.id.orEmpty()
        if (bizId.isBlank()) return
        viewModelScope.launch {
            registerPushTokenForBusiness(appContext, serverApi, bizId)
        }
    }

    // Label of the document the extraction came from, e.g. "Flyer.pdf".
    private val _onboardingDocLabel = MutableStateFlow<String?>(null)
    val onboardingDocLabel: StateFlow<String?> = _onboardingDocLabel.asStateFlow()

    init {
        refreshUserProfile()
        viewModelScope.launch {
            combine(localPrefs, activeBusiness) { prefs, biz ->
                prefs.weatherCity.ifBlank { biz?.serviceArea.orEmpty() }
            }.distinctUntilChanged().collect { city ->
                _weatherState.value = if (city.isNotBlank()) weatherService.fetchCurrentWeather(city) else null
            }
        }
        // Phase 2 - server status probe at start, and a real offline
        // signal: going offline posts the existing banner once (the
        // app keeps showing cached Firestore data); coming back online
        // re-probes the server automatically.
        refreshServerStatus()
        // Server business re-sync (old app's PR #58, ported): the
        // server keeps its own business store, wiped on every
        // redeploy, and its login gate refuses a businessId the caller
        // is not a member of ("You are not on this team"). Re-save the
        // active business when it is first observed or switches, so the
        // gate passes after a wipe. A failed sync is retried on the
        // next foreground return (see onAppForegrounded), not
        // forgotten for the session.
        viewModelScope.launch {
            var lastSyncedId: String? = null
            activeBusiness.collect { biz ->
                val id = biz?.id.orEmpty()
                if (biz == null || id.isBlank() || id == lastSyncedId) return@collect
                lastSyncedId = id
                syncBusinessToServer(biz)
                // 8 fixes Nov 26 (item 1) — register this device's FCM
                // token for the business so the receptionist-answered
                // alert reaches this app. Re-sent on every business
                // switch, even when unchanged.
                registerPushToken()
            }
        }
        viewModelScope.launch {
            var wasOnline: Boolean? = null
            connectionState.collect { state ->
                val online = state == ConnectionState.ONLINE
                if (wasOnline == true && !online) {
                    _serverStatus.value = _serverStatus.value.copy(
                        reachable = false,
                        detail = "No network connection - showing your saved data.",
                    )
                    _bannerMessage.value = "You're offline - showing your saved data. Changes will sync when you're back online."
                } else if (wasOnline == false && online) {
                    refreshServerStatus()
                }
                wasOnline = online
            }
        }
        // Outbound dialer: mirror the manager's call state, and log the
        // call once when it ends (only when it actually connected).
        viewModelScope.launch {
            outboundCallManager.state.collect { managerState ->
                val previous = _outboundCallState.value
                _outboundCallState.value = managerState
                val ended = managerState.status == OutboundCallStatus.ENDED ||
                    managerState.status == OutboundCallStatus.ERROR
                val wasActive = previous.status == OutboundCallStatus.DIALING ||
                    previous.status == OutboundCallStatus.CONNECTED
                if (ended && wasActive) logOutboundCallIfNeeded(managerState)
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

    /**
     * Server business re-sync (old app's PR #58, ported). The server
     * keeps its own business store (local JSON, wiped on every
     * redeploy) and its login gate refuses a businessId the caller is
     * not a member of — that is the "You are not on this team" 403 on
     * live voice, Assistant chat, and Documents. Re-saving the
     * business restores the server's copy with the caller as owner.
     * Best-effort and SILENT: a failure never blocks the app, shows
     * no error, and fakes nothing — it only logs, and the next
     * foreground return retries. The GET check means a business the
     * server already has is never re-POSTed.
     */
    private fun syncBusinessToServer(biz: BusinessAccount) {
        val id = biz.id
        if (id.isBlank() || businessSyncInFlight.contains(id)) return
        businessSyncInFlight.add(id)
        viewModelScope.launch {
            try {
                when (val listed = serverApi.listServerBusinesses()) {
                    is ApiResult.Success -> {
                        if (listed.data.any { it.id == id }) return@launch
                    }
                    else -> {
                        // Unknown server state: try the POST anyway —
                        // the server answers 200/201 for the owner, and
                        // a failure here is only logged below.
                    }
                }
                when (val result = serverApi.registerBusiness(
                    biz.toBusinessSyncRequest(currentUserId, currentUserEmail, currentUserName)
                )) {
                    is ApiResult.Success -> Log.i(TAG, "Business $id re-synced to server.")
                    is ApiResult.HttpError -> Log.w(TAG, "Business re-sync failed (${result.status}): ${result.message}")
                    is ApiResult.NetworkError -> Log.w(TAG, "Business re-sync failed: ${result.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Business re-sync failed", e)
            } finally {
                businessSyncInFlight.remove(id)
            }
        }
    }

    /** Called when the app returns to the foreground: a redeploy can
     * wipe the server's business store while the app sits
     * backgrounded, so re-check (and re-save if missing) the active
     * business. Silent and best-effort, like the initial sync. */
    fun onAppForegrounded() {
        val biz = activeBusiness.value ?: return
        syncBusinessToServer(biz)
        // 8 fixes Nov 26 (item 1) — re-register the FCM token on every
        // foreground return: a server redeploy wipes the token store,
        // and a stale registration is the silent way the alert stops.
        registerPushToken()
    }

    fun refreshOperationalFeeds() {
        val biz = activeBusiness.value ?: return
        if (_isOperationalSyncing.value) return
        viewModelScope.launch {
            _isOperationalSyncing.value = true
            _operationalSyncError.value = null
            repository.fetchOperationalSnapshot(biz.id, currentUserId)
                .onSuccess {
                    val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date())
                    _lastOperationalSyncLabel.value = "Synced $timeStr"
                }
                .onFailure { e ->
                    _operationalSyncError.value = e.localizedMessage ?: "Could not sync Firestore events."
                }
            _isOperationalSyncing.value = false
        }
    }

    fun refreshWeeklyDispatchTrends() {
        val biz = activeBusiness.value ?: return
        if (_isWeeklyTrendsRefreshing.value) return
        viewModelScope.launch {
            _isWeeklyTrendsRefreshing.value = true
            _weeklyTrendsError.value = null
            repository.fetchWeeklyDispatchJobs(biz.id, currentUserId)
                .onSuccess { fetchedJobs ->
                    _weeklyFetchedJobs.value = biz.id to fetchedJobs
                    val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date())
                    _weeklyTrendsSyncLabel.value = "Synced $timeStr"
                }
                .onFailure { e ->
                    _weeklyTrendsError.value = e.localizedMessage ?: "Failed to fetch weekly job dispatch trends."
                }
            _isWeeklyTrendsRefreshing.value = false
        }
    }

    fun analyzeOnboardingInput(descriptionOrDoc: String, bitmap: Bitmap?) {
        if (descriptionOrDoc.isBlank() && bitmap == null) {
            _bannerMessage.value = "Describe your company or attach a flyer/document first."
            return
        }
        viewModelScope.launch {
            _onboardingExtraction.value = UiState.Loading
            _onboardingAppCheckError.value = false
            handleOnboardingExtraction(
                aiRepo.extractOnboardingProfile(
                    descriptionOrDoc,
                    attachedBitmaps = bitmap?.let { listOf(it) } ?: emptyList()
                )
            )
        }
    }

    /** Upload any company document (PDF, Word, image, text) and have
     * the AI fill the onboarding sections from what it actually
     * states. Missing fields stay empty — nothing is invented. */
    fun analyzeOnboardingDocument(uri: Uri, mimeType: String?, displayName: String?) {
        viewModelScope.launch {
            _onboardingExtraction.value = UiState.Loading
            _onboardingAppCheckError.value = false
            when (val input = extractDocumentInput(appContext, uri, mimeType, displayName)) {
                is DocumentInput.Unsupported -> {
                    _onboardingExtraction.value = UiState.Error(input.reason)
                }
                is DocumentInput.Text -> {
                    _onboardingDocLabel.value = input.label
                    handleOnboardingExtraction(
                        aiRepo.extractOnboardingProfile(descriptionOrDoc = input.text)
                    )
                }
                is DocumentInput.Images -> {
                    _onboardingDocLabel.value = input.label
                    handleOnboardingExtraction(
                        aiRepo.extractOnboardingProfile(
                            descriptionOrDoc = "Company document: ${input.label}. Extract the business profile from the attached pages.",
                            attachedBitmaps = input.bitmaps
                        )
                    )
                }
            }
        }
    }

    private fun handleOnboardingExtraction(result: Result<OnboardingExtraction>) {
        result.onSuccess { extracted ->
            _onboardingExtraction.value = UiState.Success(extracted)
            _onboardingAppCheckError.value = false
        }.onFailure { e ->
            if (isAppCheckFailure(e)) {
                // No red raw error: the setup card explains the fix
                // and shows the token to register.
                _onboardingExtraction.value = null
                _onboardingAppCheckError.value = true
            } else {
                _onboardingExtraction.value = UiState.Error(e.localizedMessage ?: "AI analysis failed.")
            }
        }
    }

    fun dismissAppCheckNotice() {
        _onboardingAppCheckError.value = false
        // 8 fixes Nov 26 (item 8) — the same dismiss clears the
        // general notice too (the shell's setup-card dialog).
        _appCheckNotice.value = false
    }

    /** The debug secret this build's App Check tokens are signed with
     * (debug builds only, after an AI call was attempted once). Shown
     * in-app so the owner can paste it into the Firebase console
     * (Project Settings → App Check → Android app → Manage debug
     * tokens). Null until the SDK has generated one. */
    fun debugAppCheckToken(): String? = readDebugSecret(appContext)

    fun clearOnboardingExtraction() {
        _onboardingExtraction.value = null
        _onboardingDocLabel.value = null
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
        onCreated: () -> Unit,
        // Contact + team sections of the rebuilt onboarding form.
        // Blank means "not provided" — never invented from elsewhere.
        contactName: String = "",
        contactPhone: String = "",
        contactEmail: String = "",
        teamSize: String = "",
        servesResidential: Boolean = true,
        servesCommercial: Boolean = false,
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
            contactName = contactName.trim().ifBlank { currentUserName.ifBlank { cleanName } },
            contactPhone = contactPhone.trim(),
            contactEmail = contactEmail.trim().ifBlank { currentUserEmail },
            teamSize = teamSize.trim(),
            servesResidential = servesResidential,
            servesCommercial = servesCommercial,
            ownerEmail = currentUserEmail,
        )
        viewModelScope.launch {
            _isMutating.value = true
            repository.createBusiness(biz)
                .onSuccess { newId ->
                    prefsRepo.setActiveBusinessId(newId)
                    // Per-user profile: phone + contact email live on the
                    // User record, never on the business doc. Best effort —
                    // a profile-save failure must not fail the workspace.
                    viewModelScope.launch {
                        repository.saveUserProfile(
                            UserProfile(
                                displayName = currentUserName,
                                contactEmail = currentUserEmail
                            )
                        )
                        refreshUserProfile()
                    }
                    if (serviceArea.isNotBlank()) prefsRepo.setWeatherCity(serviceArea)
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

    /** Save one agent's info/permissions/thresholds (agent edit
     * screen). Stored as agentSettings.<employeeId> on the business
     * doc — the Firestore rules allowlist includes agentSettings, so
     * this write passes for the owner. A failure surfaces as the
     * normal banner, never a silent drop. */
    fun updateAgentSettings(employeeId: String, settings: AgentSettings) {
        updateBusinessProfile(
            mapOf("agentSettings.$employeeId" to settings.toMap()),
            successMsg = "Agent settings saved."
        )
    }

    // Per-user profile save. This writes ONLY /users/{uid} — it never
    // touches the business doc, so a member can never overwrite company
    // contact info by saving their own phone/email.
    fun saveUserProfile(displayName: String, phone: String, contactEmail: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val current = (_userProfileState.value as? UiState.Success)?.data
            repository.saveUserProfile(
                UserProfile(
                    displayName = displayName.trim().ifBlank { currentUserName },
                    phone = phone.trim(),
                    contactEmail = contactEmail.trim(),
                    businessIds = current?.businessIds.orEmpty()
                )
            ).onSuccess {
                refreshUserProfile()
                _bannerMessage.value = "Profile saved."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to save profile."
            }
            _isMutating.value = false
        }
    }

    // Company contact details are owner-only. Rules enforce this too
    // (business updates require the owner), but fail fast here with a
    // plain message instead of a Firestore permission error.
    fun updateBusinessContact(contactName: String, contactPhone: String, contactEmail: String) {
        val biz = activeBusiness.value ?: return
        if (biz.ownerId != currentUserId) {
            _bannerMessage.value = "Only the business owner can change company contact details."
            return
        }
        updateBusinessProfile(
            mapOf(
                "contactName" to contactName.trim(),
                "contactPhone" to contactPhone.trim(),
                "contactEmail" to contactEmail.trim()
            ),
            successMsg = "Company contact details updated."
        )
    }

    // ---- Invoices & Quotes (money loop) ----
    fun createInvoice(title: String, clientName: String, amount: Double, dueDate: String = "") {
        val biz = activeBusiness.value ?: return
        if (title.isBlank() || clientName.isBlank()) {
            _bannerMessage.value = "Invoice title and customer name are required."
            return
        }
        viewModelScope.launch {
            repository.createInvoice(
                biz,
                Invoice(
                    businessId = biz.id,
                    title = title.trim(),
                    clientName = clientName.trim(),
                    amount = amount,
                    status = "unpaid",
                    dueDate = dueDate.trim()
                )
            ).onSuccess {
                _bannerMessage.value = "Invoice created."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to create invoice."
            }
        }
    }

    fun updateInvoiceStatus(invoice: Invoice, status: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.updateInvoiceStatus(biz.id, invoice.id, status)
                .onSuccess { _bannerMessage.value = "Invoice marked $status." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to update invoice." }
        }
    }

    fun createQuote(title: String, clientName: String, amount: Double) {
        val biz = activeBusiness.value ?: return
        if (title.isBlank() || clientName.isBlank()) {
            _bannerMessage.value = "Quote title and customer name are required."
            return
        }
        viewModelScope.launch {
            repository.createQuote(
                biz,
                Quote(
                    businessId = biz.id,
                    title = title.trim(),
                    clientName = clientName.trim(),
                    amount = amount,
                    status = "draft"
                )
            ).onSuccess {
                _bannerMessage.value = "Quote created."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to create quote."
            }
        }
    }

    fun updateQuoteStatus(quote: Quote, status: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            repository.updateQuoteStatus(biz.id, quote.id, status)
                .onSuccess { _bannerMessage.value = "Quote $status." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to update quote." }
        }
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
        val nextModel = aiRepo.resolveModelName(
            employeeId = employeeId,
            taskProfile = _aiState.value.selectedTaskProfile,
            hasMultimodalOrDoc = false
        )
        _aiState.value = _aiState.value.copy(
            selectedEmployeeId = employeeId,
            activeModelId = nextModel
        )
    }

    fun selectAiTaskProfile(profile: AiTaskProfile) {
        val nextModel = aiRepo.resolveModelName(
            employeeId = _aiState.value.selectedEmployeeId,
            taskProfile = profile,
            hasMultimodalOrDoc = false
        )
        _aiState.value = _aiState.value.copy(
            selectedTaskProfile = profile,
            activeModelId = nextModel
        )
    }

    /** Server model tier for the selected task profile. The server maps
     * `complex` -> PRO model, `fast` -> fast model, anything else ->
     * primary model — see `POST /api/chat` in server.ts. */
    private fun modelTierFor(taskProfile: AiTaskProfile): String = when (taskProfile) {
        AiTaskProfile.FAST -> "fast"
        AiTaskProfile.DEEP_REASONING -> "complex"
        else -> "general"
    }

    /** 8 fixes Nov 26 (item 4) — re-subscribe the AI chat listener so
     * the thread shows the latest messages. Also bumped after a send,
     * so a message that was just saved is never left looking unsent. */
    fun refreshChatMessages() {
        chatReloadTrigger.value += 1
    }

    /** 8 fixes Nov 26 (item 4) — delete one AI Assistant thread (all
     * of that employee's messages). The caller confirms first; the
     * live listener removes the messages from the screen as they are
     * deleted. Only the owner can delete (rules enforce it). */
    fun deleteChatThread(employeeId: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            repository.deleteChatThread(biz.id, employeeId)
                .onSuccess { _bannerMessage.value = "Conversation deleted." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to delete conversation." }
            _isMutating.value = false
        }
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
        val employeeId = _aiState.value.selectedEmployeeId
        val taskProfile = _aiState.value.selectedTaskProfile
        val attachedDocIds = _attachedDocumentIds.value.toList()
        val clean = userPrompt.trim()
        if (clean.isEmpty() && attachedBitmap == null && attachedDocIds.isEmpty()) return
        val effectivePrompt = when {
            clean.isNotEmpty() -> clean
            attachedBitmap != null -> "Please analyze this job site photo and suggest next steps."
            else -> "Please review the attached document and summarize the key details."
        }
        val existingHistory = (chatMessagesState.value as? UiState.Success)?.data
            .orEmpty()
            .filter { it.employeeId == employeeId || it.employeeId.isBlank() }

        viewModelScope.launch {
            _aiState.value = _aiState.value.copy(
                // The reply comes from the Railway server, not from a
                // model running on the phone — say so until the real
                // model id arrives with the reply.
                activeModelId = "Railway",
                isStreaming = true,
                streamingText = "",
                error = null
            )
            val userMessage = ChatMessage(
                businessId = biz.id,
                senderId = currentUserId,
                senderName = currentUserName.ifBlank { "Owner" },
                employeeId = employeeId,
                role = "user",
                text = effectivePrompt
            )
            repository.saveChatMessage(biz, userMessage)
            // 8 fixes Nov 26 (item 4) — re-subscribe after the save so
            // the just-sent message shows immediately instead of
            // waiting on the listener's next snapshot.
            refreshChatMessages()
            // Photos have no server endpoint (`POST /api/chat` is
            // text-only), so they still go through direct Gemini
            // (AiRepository) with the same per-business system prompt.
            // Text + server documents always go through /api/chat.
            if (attachedBitmap != null) {
                sendAiMessageViaDirectGemini(
                    biz = biz,
                    employeeId = employeeId,
                    taskProfile = taskProfile,
                    effectivePrompt = effectivePrompt,
                    existingHistory = existingHistory,
                    attachedDocumentText = attachedDocumentText,
                    attachedBitmap = attachedBitmap
                )
                return@launch
            }
            val history = existingHistory.takeLast(23) + userMessage
            when (val result = serverApi.postChat(history, employeeId, modelTierFor(taskProfile), biz.id, attachedDocIds)) {
                is ApiResult.Success -> {
                    val reply = result.data
                    _aiState.value = _aiState.value.copy(activeModelId = reply.modelUsed)
                    repository.saveChatMessage(
                        biz,
                        ChatMessage(
                            businessId = biz.id,
                            senderId = employeeId,
                            senderName = RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == employeeId }?.name ?: "AI Assistant",
                            employeeId = employeeId,
                            role = "assistant",
                            text = reply.content,
                            modelUsed = reply.modelUsed
                        )
                    )
                    if (reply.isFallback) {
                        _bannerMessage.value = "The server's AI is down — this reply came from its offline fallback. Check it before acting on it."
                    }
                    _aiState.value = _aiState.value.copy(isStreaming = false, streamingText = "")
                }
                is ApiResult.HttpError -> {
                    Log.w(TAG, "Server chat failed: ${result.status} ${result.message}")
                    _aiState.value = _aiState.value.copy(
                        isStreaming = false,
                        streamingText = "",
                        error = result.message
                    )
                }
                is ApiResult.NetworkError -> {
                    _aiState.value = _aiState.value.copy(
                        isStreaming = false,
                        streamingText = "",
                        error = "Server unreachable - check your connection and try again."
                    )
                }
            }
        }
    }

    /** Photo path only: direct Gemini via AiRepository, exactly as
     * Phase 1 wired it. Text chat never comes here — it goes through
     * the server (see [sendAiMessage]). */
    private suspend fun sendAiMessageViaDirectGemini(
        biz: BusinessAccount,
        employeeId: String,
        taskProfile: AiTaskProfile,
        effectivePrompt: String,
        existingHistory: List<ChatMessage>,
        attachedDocumentText: String,
        attachedBitmap: Bitmap?
    ) {
        val resolvedModel = aiRepo.resolveModelName(
            employeeId = employeeId,
            taskProfile = taskProfile,
            hasMultimodalOrDoc = attachedBitmap != null || attachedDocumentText.isNotBlank()
        )
        _aiState.value = _aiState.value.copy(activeModelId = resolvedModel)
        try {
            var finalReply = ""
            var modelUsed = resolvedModel
            aiRepo.streamAssistantReply(
                prompt = effectivePrompt,
                historyMessages = existingHistory,
                taskProfile = taskProfile,
                business = biz,
                employeeId = employeeId,
                customers = (customersState.value as? UiState.Success)?.data.orEmpty(),
                jobs = (jobsState.value as? UiState.Success)?.data.orEmpty(),
                tasks = (tasksState.value as? UiState.Success)?.data.orEmpty(),
                calls = (callsState.value as? UiState.Success)?.data.orEmpty(),
                actions = (actionsState.value as? UiState.Success)?.data.orEmpty(),
                attachedDocumentText = attachedDocumentText,
                attachedBitmap = attachedBitmap
            ).collect { event ->
                finalReply = event.accumulatedText
                modelUsed = event.modelUsed
                _aiState.value = _aiState.value.copy(
                    activeModelId = modelUsed,
                    streamingText = finalReply
                )
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
                        modelUsed = modelUsed
                    )
                )
            }
            _aiState.value = _aiState.value.copy(isStreaming = false, streamingText = "")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Assistant error", e)
            // 8 fixes Nov 26 (item 8) — an App Check rejection shows
            // the setup card with the debug token; the raw error text
            // is never shown on the chat screen.
            if (isAppCheckFailure(e)) {
                _appCheckNotice.value = true
                _aiState.value = _aiState.value.copy(isStreaming = false, streamingText = "", error = null)
            } else {
                _aiState.value = _aiState.value.copy(
                    isStreaming = false,
                    streamingText = "",
                    error = e.localizedMessage ?: "AI Assistant encountered an error."
                )
            }
        }
    }

    // ---- Live Voice Conversations (LiveKit + Gemini realtime) ----
    /** Server persona for an employee id. Unknown ids fall back to
     * `executive` — the server itself also falls back to
     * executive_assistant for unknown roleIds. */
    private fun personaForEmployee(employeeId: String): String = when (employeeId) {
        "ai_receptionist" -> "receptionist"
        "sales" -> "sales"
        "operations" -> "operations"
        "customer_service" -> "support"
        "technician" -> "technician"
        "finance" -> "finance"
        "marketing" -> "marketing"
        else -> "executive"
    }

    fun startLiveVoiceConversation(employeeIdOverride: String? = null) {
        val employeeId = employeeIdOverride ?: _aiState.value.selectedEmployeeId
        val employee = RcosDefaults.AI_EMPLOYEES.firstOrNull { it.id == employeeId }
            ?: RcosDefaults.AI_EMPLOYEES.first()
        val biz = activeBusiness.value
        val persona = personaForEmployee(employee.id)

        // An outbound dial-out call owns the mic and the earpiece —
        // live voice waits until it ends (the dialer refuses the
        // reverse too).
        if (outboundCallState.value.status == OutboundCallStatus.DIALING ||
            outboundCallState.value.status == OutboundCallStatus.CONNECTED
        ) {
            _liveVoiceState.value = LiveVoiceUiState(
                status = LiveVoiceStatus.ERROR,
                modelId = MODEL_LIVEKIT_VOICE,
                activeRoleName = employee.name,
                durationSeconds = 0,
                statusDetail = "Voice session unavailable",
                error = "End the outbound call first, then start live voice."
            )
            return
        }

        viewModelScope.launch {
            liveTimerJob?.cancel()
            _liveVoiceState.value = LiveVoiceUiState(
                status = LiveVoiceStatus.CONNECTING,
                modelId = MODEL_LIVEKIT_VOICE,
                activeRoleName = employee.name,
                durationSeconds = 0,
                statusDetail = "Connecting to LiveKit voice...",
                error = null
            )
            _isSpeakerphone.value = false
            if (biz == null) {
                _liveVoiceState.value = LiveVoiceUiState(
                    status = LiveVoiceStatus.ERROR,
                    modelId = MODEL_LIVEKIT_VOICE,
                    activeRoleName = employee.name,
                    durationSeconds = 0,
                    statusDetail = "Voice session unavailable",
                    error = "Complete workspace setup first to use live voice."
                )
                return@launch
            }
            liveKitVoiceManager.startConversation(businessId = biz.id, persona = persona)
                .onSuccess {
                    _liveVoiceState.value = LiveVoiceUiState(
                        status = LiveVoiceStatus.LIVE,
                        modelId = MODEL_LIVEKIT_VOICE,
                        activeRoleName = employee.name,
                        durationSeconds = 0,
                        statusDetail = "Listening & speaking live • LiveKit",
                        error = null
                    )
                    liveTimerJob = viewModelScope.launch {
                        while (true) {
                            delay(1000L)
                            _liveVoiceState.value = _liveVoiceState.value.copy(
                                durationSeconds = _liveVoiceState.value.durationSeconds + 1
                            )
                        }
                    }
                }
                .onFailure { e ->
                    val msg = e.localizedMessage ?: "Could not start the live voice conversation."
                    _liveVoiceState.value = LiveVoiceUiState(
                        status = LiveVoiceStatus.ERROR,
                        modelId = MODEL_LIVEKIT_VOICE,
                        activeRoleName = employee.name,
                        durationSeconds = 0,
                        statusDetail = "Voice session unavailable",
                        error = msg
                    )
                }
        }
    }

    /** Speakerphone toggle for the live session. The session always
     * starts on the earpiece; this flips it and reports the actual
     * state back (see LiveKitVoiceManager.setSpeakerphone). */
    fun toggleLiveVoiceSpeakerphone() {
        val current = _liveVoiceState.value
        if (current.status != LiveVoiceStatus.LIVE && current.status != LiveVoiceStatus.CONNECTING) return
        _isSpeakerphone.value = liveKitVoiceManager.setSpeakerphone(!_isSpeakerphone.value)
    }

    fun stopLiveVoiceConversation(logSession: Boolean = true) {
        val current = _liveVoiceState.value
        val elapsedSec = current.durationSeconds
        val wasLive = current.status == LiveVoiceStatus.LIVE || current.status == LiveVoiceStatus.CONNECTING
        liveTimerJob?.cancel()
        liveTimerJob = null
        viewModelScope.launch {
            liveKitVoiceManager.stopConversation()
            _liveVoiceState.value = LiveVoiceUiState()
            _isSpeakerphone.value = false
            val biz = activeBusiness.value
            if (logSession && wasLive && elapsedSec >= 2 && biz != null) {
                val mins = elapsedSec / 60
                val secs = elapsedSec % 60
                val durationLabel = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                val timeLabel = SimpleDateFormat("h:mm a", Locale.US).format(Date())
                val boundedCallerId = current.modelId.ifBlank { MODEL_LIVEKIT_VOICE }.take(30)
                repository.createCallRecord(
                    biz,
                    CallRecord(
                        businessId = biz.id,
                        callerName = "Live Voice (${current.activeRoleName})",
                        callerNumber = boundedCallerId,
                        direction = CallDirection.OUTBOUND.code,
                        status = CallStatus.COMPLETED.code,
                        durationLabel = durationLabel,
                        timeLabel = timeLabel,
                        summary = "Real-time LiveKit voice session with ${current.activeRoleName}",
                        answeredBy = "ai_receptionist"
                    )
                )
            }
        }
    }

    fun dismissLiveVoiceError() {
        _liveVoiceState.value = LiveVoiceUiState()
        _isSpeakerphone.value = false
    }

    // ---- Outbound dial-out calls (LiveKit dialer, original PR #75) ----
    /**
     * Dial a number from the Calls dialpad in-app over LiveKit SIP —
     * the original app's dialer (PR #75), not the phone dialer. The
     * number is validated like the original (>= 7 digits); the trunk
     * status is checked first so a missing trunk is an honest message
     * instead of a failed dial; an unknown status never blocks the
     * attempt (the dial itself surfaces the real error).
     */
    fun startOutboundCall(name: String, phone: String, note: String) {
        val biz = activeBusiness.value
        val cleanPhone = phone.trim()
        if (biz == null) {
            _outboundCallState.value = OutboundCallUiState(
                status = OutboundCallStatus.ERROR,
                number = cleanPhone,
                error = "Pick a business first, then dial."
            )
            return
        }
        if (liveVoiceState.value.status == LiveVoiceStatus.CONNECTING ||
            liveVoiceState.value.status == LiveVoiceStatus.LIVE
        ) {
            _outboundCallState.value = OutboundCallUiState(
                status = OutboundCallStatus.ERROR,
                number = cleanPhone,
                error = "End the live voice session first, then dial out."
            )
            return
        }
        if (cleanPhone.length < 7) {
            _outboundCallState.value = OutboundCallUiState(
                status = OutboundCallStatus.ERROR,
                number = cleanPhone,
                error = "Enter a full phone number to dial."
            )
            return
        }
        outboundLogInfo = Triple(name.trim(), cleanPhone, note.trim())
        outboundLoggedForSeconds = -1
        viewModelScope.launch {
            when (val statusResult = serverApi.fetchOutboundStatus(biz.id)) {
                is ApiResult.Success -> {
                    _outboundStatus.value = statusResult.data
                    if (!statusResult.data.configured) {
                        _outboundCallState.value = OutboundCallUiState(
                            status = OutboundCallStatus.ERROR,
                            number = cleanPhone,
                            error = OUTBOUND_SETUP_MESSAGE
                        )
                        return@launch
                    }
                }
                else -> {
                    // Unknown status: let the dial attempt surface the real error.
                }
            }
            outboundCallManager.dial(biz.id, cleanPhone, name.trim())
                .onFailure { e ->
                    val message = e.message.orEmpty()
                    val friendly = if (message.contains("trunk", ignoreCase = true) ||
                        message.contains("not set up", ignoreCase = true)
                    ) {
                        OUTBOUND_SETUP_MESSAGE
                    } else {
                        message.ifBlank { "The call could not be started. Try again." }
                    }
                    _outboundCallState.value = OutboundCallUiState(
                        status = OutboundCallStatus.ERROR,
                        number = cleanPhone,
                        error = friendly
                    )
                }
        }
    }

    fun endOutboundCall() {
        viewModelScope.launch { outboundCallManager.hangup() }
    }

    /** Speakerphone toggle for the outbound call. Starts on the
     * earpiece; flips and reports the actual state back. */
    fun toggleOutboundSpeakerphone() {
        val current = _outboundCallState.value
        if (current.status != OutboundCallStatus.DIALING && current.status != OutboundCallStatus.CONNECTED) return
        outboundCallManager.setSpeakerphone(!current.speakerOn)
    }

    fun toggleOutboundMute() {
        val current = _outboundCallState.value
        if (current.status != OutboundCallStatus.DIALING && current.status != OutboundCallStatus.CONNECTED) return
        viewModelScope.launch { outboundCallManager.setMuted(!current.muted) }
    }

    fun sendOutboundDtmf(digit: String) {
        viewModelScope.launch { outboundCallManager.sendDtmf(digit) }
    }

    fun clearOutboundSentDigits() {
        outboundCallManager.clearSentDigits()
    }

    fun dismissOutboundError() {
        outboundCallManager.reset()
        _outboundCallState.value = OutboundCallUiState()
    }

    /** Refresh the outbound trunk status (called when the Calls screen
     * appears). A failure leaves the last known status; the dial
     * attempt surfaces the real error. */
    fun refreshOutboundStatus() {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            when (val result = serverApi.fetchOutboundStatus(biz.id)) {
                is ApiResult.Success -> _outboundStatus.value = result.data
                else -> Unit
            }
        }
    }

    /**
     * Logs the outbound call once, when it ends — only when it actually
     * connected for more than 0 seconds, exactly like the original
     * (OutboundDialer endCall). Direction OUTBOUND, answeredBy
     * human_operator (the owner dialed). A call that never connected
     * logs nothing: no fake "2 min" entry.
     */
    private fun logOutboundCallIfNeeded(managerState: OutboundCallUiState) {
        val info = outboundLogInfo ?: return
        val seconds = managerState.durationSeconds
        if (seconds <= 0 || outboundLoggedForSeconds == seconds) return
        outboundLoggedForSeconds = seconds
        val (name, phone, note) = info
        val durationLabel = if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
        val summary = buildString {
            append("Outbound call to ${formatDialDisplay(phone)} from the app dialer.")
            if (note.isNotBlank()) append(" $note")
        }
        logCall(
            callerName = name.ifBlank { formatDialDisplay(phone) },
            callerNumber = phone,
            direction = CallDirection.OUTBOUND,
            status = CallStatus.COMPLETED,
            durationLabel = durationLabel,
            summary = summary,
            answeredBy = "human_operator"
        )
    }

    /** US display format for a dialed number — the original app's
     * formatDialDisplay: +1 (XXX) XXX-XXXX, (XXX) XXX-XXXX, anything
     * else shown as typed. */
    private fun formatDialDisplay(raw: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (hasPlus && digits.length == 11 && digits.startsWith("1")) {
            return "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
        }
        if (!hasPlus && digits.length == 10) {
            return "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
        }
        if (!hasPlus && digits.length == 11 && digits.startsWith("1")) {
            return "+1 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7)}"
        }
        return trimmed
    }

    // ---- Assistant documents (server-side) ----
    /** List this business's assistant documents (newest first). */
    fun refreshAssistantDocuments() {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            when (val result = serverApi.listAssistantDocuments(biz.id)) {
                is ApiResult.Success -> {
                    _documents.value = result.data
                    _documentError.value = null
                }
                is ApiResult.HttpError -> _documentError.value = result.message
                is ApiResult.NetworkError -> _documentError.value = "Server unreachable - documents could not be listed."
            }
        }
    }

    /** Upload one document (PDF / Word / text / CSV, 10 MB max — the
     * server answers 422 when it is too big or has no readable text).
     * A successful upload is attached to the next chat message
     * automatically, so Ryan can ask about it right away. */
    fun uploadAssistantDocument(name: String, mimeType: String, fileBase64: String) {
        val biz = activeBusiness.value ?: run {
            _documentError.value = "Complete workspace setup first to upload documents."
            return
        }
        viewModelScope.launch {
            _isUploadingDocument.value = true
            _documentError.value = null
            when (val result = serverApi.uploadAssistantDocument(biz.id, name, mimeType, fileBase64)) {
                is ApiResult.Success -> {
                    _documents.value = listOf(result.data) + _documents.value
                    _attachedDocumentIds.value = _attachedDocumentIds.value + result.data.id
                    _bannerMessage.value = "Document uploaded: ${result.data.name}"
                }
                is ApiResult.HttpError -> _documentError.value = result.message
                is ApiResult.NetworkError -> _documentError.value = "Server unreachable - document could not be uploaded."
            }
            _isUploadingDocument.value = false
        }
    }

    fun toggleAttachDocument(documentId: String) {
        _attachedDocumentIds.value = if (documentId in _attachedDocumentIds.value) {
            _attachedDocumentIds.value - documentId
        } else {
            _attachedDocumentIds.value + documentId
        }
    }

    fun deleteAssistantDocument(documentId: String) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            when (val result = serverApi.deleteAssistantDocument(biz.id, documentId)) {
                is ApiResult.Success -> {
                    _documents.value = _documents.value.filterNot { it.id == documentId }
                    _attachedDocumentIds.value = _attachedDocumentIds.value - documentId
                }
                is ApiResult.HttpError -> _documentError.value = result.message
                is ApiResult.NetworkError -> _documentError.value = "Server unreachable - document could not be deleted."
            }
        }
    }

    /** Draft a job from a document, the way the old app does it: send
     * Morgan a chat message ("Create a job for {client} about
     * {scope}") with the document attached. Nothing is created until
     * the draft is confirmed — the in-chat job-confirmation card is
     * Phase 5 work. */
    fun createJobFromDocument(document: AssistantDocument) {
        val extracted = document.extracted
        val client = extracted.contactName.ifBlank { extracted.companyName }
        if (client.isBlank()) {
            _bannerMessage.value = "This document does not name a client, so a job cannot be drafted from it."
            return
        }
        val scope = extracted.notes.ifBlank { document.name }
        _attachedDocumentIds.value = _attachedDocumentIds.value + document.id
        sendAiMessage(userPrompt = "Create a job for $client about $scope")
    }

    /** Add the document's client as a customer (two-tap confirm in
     * the UI — this only runs on the second tap). Uses the extracted
     * name, phone, and email; the server extracts no address, so the
     * address stays blank for Ryan to fill in. */
    fun addClientFromDocument(document: AssistantDocument) {
        val biz = activeBusiness.value ?: return
        val extracted = document.extracted
        val name = extracted.contactName.ifBlank { extracted.companyName }
        if (name.isBlank()) {
            _bannerMessage.value = "This document does not name a client, so no customer can be added from it."
            return
        }
        viewModelScope.launch {
            repository.createCustomer(
                biz,
                Customer(
                    businessId = biz.id,
                    name = name.trim(),
                    company = extracted.companyName.trim(),
                    phone = extracted.phone.trim(),
                    email = extracted.email.trim(),
                    address = "",
                    status = CustomerStatus.ACTIVE.code,
                    healthScore = 92L,
                    lastContactDate = SimpleDateFormat("MMM dd", Locale.US).format(Date()),
                    serviceType = "",
                    aiSummary = extracted.notes.trim()
                )
            ).onSuccess {
                _bannerMessage.value = "$name added to Customers."
            }.onFailure { e ->
                _bannerMessage.value = e.localizedMessage ?: "Failed to add customer."
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
                    // 8 fixes Nov 26 (item 8) — App Check rejection shows
                    // the setup card, never the raw banner.
                    if (isAppCheckFailure(e)) {
                        _appCheckNotice.value = true
                    } else {
                        _bannerMessage.value = e.localizedMessage ?: "AI quote draft failed."
                    }
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

    /** 8 fixes Nov 26 (item 3) — save the customer detail edits:
     * contact fields, notes (`aiSummary`), and the owner-defined
     * custom fields (label + value). Only the allowlisted fields are
     * written, so the rules' `affectedKeys` check passes. */
    fun updateCustomerDetails(customer: Customer, phone: String, email: String, address: String, notes: String, customFields: List<CustomField>) {
        val biz = activeBusiness.value ?: return
        viewModelScope.launch {
            _isMutating.value = true
            repository.updateCustomer(
                biz.id,
                customer.id,
                mapOf(
                    "phone" to phone.trim(),
                    "email" to email.trim(),
                    "address" to address.trim(),
                    "aiSummary" to notes.trim(),
                    "customFields" to customFields.filter { it.label.isNotBlank() }.map { it.toMap() },
                )
            ).onSuccess { _bannerMessage.value = "Customer updated." }
                .onFailure { e -> _bannerMessage.value = e.localizedMessage ?: "Failed to update customer." }
            _isMutating.value = false
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
                    // 8 fixes Nov 26 (item 8) — an App Check rejection
                    // shows the setup card with the debug token, never
                    // the raw "Firebase App Check token is invalid."
                    // banner. Any other failure keeps the plain banner.
                    if (isAppCheckFailure(e)) {
                        _appCheckNotice.value = true
                    } else {
                        _bannerMessage.value = e.localizedMessage ?: "AI follow-up failed."
                    }
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
        summary: String,
        answeredBy: String? = null
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
                    answeredBy = answeredBy ?: if (biz.aiReceptionistEnabled) "ai_receptionist" else "human_operator"
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

    /** 8 fixes Nov 26 (item 6) — invite a team member by name +
     * email/phone. They are added to the business roster with no UID
     * (an invite, not a signed-in account): `members` gains the entry
     * and `memberEmails` gains the email, so they appear in the team
     * chat roster immediately. The server re-sync tolerates a blank
     * userId (its `sanitizeMembers` coerces it to ''). Owner-only
     * (business updates require the owner). */
    fun addTeamMember(name: String, email: String, phone: String) {
        val biz = activeBusiness.value ?: return
        if (biz.ownerId != currentUserId) {
            _bannerMessage.value = "Only the business owner can add team members."
            return
        }
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        if (cleanName.isBlank() || (cleanEmail.isBlank() && phone.trim().isBlank())) {
            _bannerMessage.value = "Add a name and an email or phone number."
            return
        }
        val members = biz.members + BusinessMember(
            userId = "",
            email = cleanEmail,
            name = cleanName,
            role = "member",
        )
        val memberEmails = biz.memberEmails.toMutableMap().apply {
            if (cleanEmail.isNotBlank()) put(cleanEmail, "member")
        }
        updateBusinessProfile(
            mapOf(
                "members" to members.map {
                    mapOf(
                        "userId" to it.userId,
                        "email" to it.email,
                        "name" to it.name,
                        "role" to it.role,
                    )
                },
                "memberEmails" to memberEmails,
            ),
            successMsg = "$cleanName added to the team."
        )
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
        stopLiveVoiceConversation(logSession = false)
        prefsRepo.clearAllOnSignOut()
    }

    override fun onCleared() {
        liveTimerJob?.cancel()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            liveKitVoiceManager.stopConversation()
        }
        super.onCleared()
    }
}
