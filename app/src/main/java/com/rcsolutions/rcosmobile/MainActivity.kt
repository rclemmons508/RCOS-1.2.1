package com.rcsolutions.rcosmobile

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rcsolutions.rcosmobile.data.AiRepository
import com.rcsolutions.rcosmobile.data.LiveVoiceManager
import com.rcsolutions.rcosmobile.data.RcosRepository
import com.rcsolutions.rcosmobile.data.UserPreferencesRepository
import com.rcsolutions.rcosmobile.data.WeatherService
import com.rcsolutions.rcosmobile.data.attemptAutoSignIn
import com.rcsolutions.rcosmobile.data.authStateFlow
import com.rcsolutions.rcosmobile.data.signOut
import com.rcsolutions.rcosmobile.model.ActionStatus
import com.rcsolutions.rcosmobile.model.Job
import com.rcsolutions.rcosmobile.model.UiState
import com.rcsolutions.rcosmobile.security.configureAppCheck
import com.rcsolutions.rcosmobile.ui.MainViewModel
import com.rcsolutions.rcosmobile.ui.components.AuroraBackground
import com.rcsolutions.rcosmobile.ui.components.AuroraCard
import com.rcsolutions.rcosmobile.ui.components.AuroraPrimaryCta
import com.rcsolutions.rcosmobile.ui.components.AuroraSecondaryCta
import com.rcsolutions.rcosmobile.ui.components.CreateQuoteOrInvoiceDialog
import com.rcsolutions.rcosmobile.ui.components.NotificationsCenterDialog
import com.rcsolutions.rcosmobile.ui.components.RcSolutionsBrandLockup
import com.rcsolutions.rcosmobile.ui.screens.AccountScreen
import com.rcsolutions.rcosmobile.ui.screens.AiAssistantScreen
import com.rcsolutions.rcosmobile.ui.screens.AuthScreen
import com.rcsolutions.rcosmobile.ui.screens.CallsScreen
import com.rcsolutions.rcosmobile.ui.screens.CustomersScreen
import com.rcsolutions.rcosmobile.ui.screens.HomeScreen
import com.rcsolutions.rcosmobile.ui.screens.OnboardingScreen
import com.rcsolutions.rcosmobile.ui.screens.ScheduleScreen
import com.rcsolutions.rcosmobile.ui.screens.TasksFilter
import com.rcsolutions.rcosmobile.ui.screens.TasksScreen
import com.rcsolutions.rcosmobile.ui.theme.AuroraBorder
import com.rcsolutions.rcosmobile.ui.theme.AuroraCanvasDeep
import com.rcsolutions.rcosmobile.ui.theme.AuroraEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraObsidian
import com.rcsolutions.rcosmobile.ui.theme.AuroraOnEmerald
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextMuted
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextPrimary
import com.rcsolutions.rcosmobile.ui.theme.AuroraTextSecondary
import com.rcsolutions.rcosmobile.ui.theme.AuroraOneTheme
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object AiRoute
@Serializable data object CallsRoute
@Serializable data object TasksRoute
@Serializable data object SettingsRoute
@Serializable data object CustomersRoute
@Serializable data object ScheduleRoute
@Serializable data object ApprovalsRoute

private data class AuroraNavTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PRIMARY_TABS = listOf(
    AuroraNavTab(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    AuroraNavTab(AiRoute, "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
    AuroraNavTab(CallsRoute, "Calls", Icons.Filled.Call, Icons.Outlined.Call, "nav_calls"),
    AuroraNavTab(TasksRoute, "Tasks", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "nav_tasks"),
    AuroraNavTab(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // CRITICAL: Configure App Check with launch intent before setContent, ViewModels, or Firebase calls.
        configureAppCheck(this, intent)

        setContent {
            AuroraOneTheme {
                RcosAppRoot()
            }
        }
    }
}

@Composable
private fun RcosAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: MainViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val appContext = context.applicationContext
                        MainViewModel(
                            repository = RcosRepository(appContext),
                            prefsRepo = UserPreferencesRepository(appContext),
                            aiRepo = AiRepository(),
                            liveVoiceManager = LiveVoiceManager(),
                            weatherService = WeatherService(),
                            currentUserId = uid,
                            currentUserEmail = user.email.orEmpty(),
                            currentUserName = user.displayName.orEmpty()
                        )
                    }
                }
            )
            AuthenticatedAuroraShell(
                viewModel = viewModel,
                onSignOutRequested = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope,
                        onBeforeAuthSignOut = { viewModel.clearPrefsBeforeSignOut() }
                    )
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedAuroraShell(
    viewModel: MainViewModel,
    onSignOutRequested: () -> Unit
) {
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val businessesState by viewModel.businessesState.collectAsStateWithLifecycle()
    val activeBusiness by viewModel.activeBusiness.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val jobsState by viewModel.jobsState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val voicemailsState by viewModel.voicemailsState.collectAsStateWithLifecycle()
    val actionsState by viewModel.actionsState.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val teamMessagesState by viewModel.teamMessagesState.collectAsStateWithLifecycle()
    val agentMessagesState by viewModel.agentMessagesState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val liveVoiceState by viewModel.liveVoiceState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val isOperationalSyncing by viewModel.isOperationalSyncing.collectAsStateWithLifecycle()
    val lastOperationalSyncLabel by viewModel.lastOperationalSyncLabel.collectAsStateWithLifecycle()
    val operationalSyncError by viewModel.operationalSyncError.collectAsStateWithLifecycle()
    val weeklyDispatchJobsState by viewModel.weeklyDispatchJobsState.collectAsStateWithLifecycle()
    val isWeeklyTrendsRefreshing by viewModel.isWeeklyTrendsRefreshing.collectAsStateWithLifecycle()
    val weeklyTrendsSyncLabel by viewModel.weeklyTrendsSyncLabel.collectAsStateWithLifecycle()
    val weeklyTrendsError by viewModel.weeklyTrendsError.collectAsStateWithLifecycle()
    val onboardingExtraction by viewModel.onboardingExtraction.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    val businesses = (businessesState as? UiState.Success)?.data.orEmpty()
    val jobs = (jobsState as? UiState.Success)?.data.orEmpty()
    val customers = (customersState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val calls = (callsState as? UiState.Success)?.data.orEmpty()
    val voicemails = (voicemailsState as? UiState.Success)?.data.orEmpty()
    val actions = (actionsState as? UiState.Success)?.data.orEmpty()
    val chatMessages = (chatMessagesState as? UiState.Success)?.data.orEmpty()
    val teamMessages = (teamMessagesState as? UiState.Success)?.data.orEmpty()
    val agentMessages = (agentMessagesState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()

    var showNewWorkspaceOnboarding by remember { mutableStateOf(false) }
    var dismissedEmptyOnboarding by remember { mutableStateOf(false) }

    val shouldShowOnboarding = showNewWorkspaceOnboarding ||
        (businessesState is UiState.Success && businesses.isEmpty() && !dismissedEmptyOnboarding)

    if (businessesState is UiState.Loading) {
        AuroraBackground {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RcSolutionsBrandLockup()
                    Spacer(Modifier.height(20.dp))
                    CircularProgressIndicator(color = AuroraEmerald)
                }
            }
        }
        return
    }

    if (businessesState is UiState.Error && !showNewWorkspaceOnboarding) {
        val errMsg = (businessesState as UiState.Error).message
        AuroraBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AuroraCard(highlighted = true, modifier = Modifier.widthIn(max = 420.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RcSolutionsBrandLockup()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Unable to Sync Workspace",
                            style = MaterialTheme.typography.headlineSmall,
                            color = AuroraTextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AuroraTextSecondary
                        )
                        Spacer(Modifier.height(18.dp))
                        AuroraPrimaryCta(
                            text = "Retry Connection",
                            testTag = "workspace_retry_button",
                            onClick = { viewModel.retryLoadWorkspaces() }
                        )
                        Spacer(Modifier.height(10.dp))
                        AuroraSecondaryCta(
                            text = "Create Workspace",
                            testTag = "workspace_error_setup_button",
                            onClick = { showNewWorkspaceOnboarding = true }
                        )
                        Spacer(Modifier.height(10.dp))
                        AuroraSecondaryCta(
                            text = "Sign Out",
                            testTag = "workspace_error_signout_button",
                            onClick = onSignOutRequested
                        )
                    }
                }
            }
        }
        return
    }

    if (shouldShowOnboarding) {
        val canCancel = businesses.isNotEmpty()
        if (canCancel) {
            BackHandler {
                showNewWorkspaceOnboarding = false
                viewModel.clearOnboardingExtraction()
            }
        }
        OnboardingScreen(
            defaultOwnerName = viewModel.currentUserName,
            extractionState = onboardingExtraction,
            isSaving = isMutating,
            canCancel = canCancel,
            onAnalyzeInput = { text, bmp -> viewModel.analyzeOnboardingInput(text, bmp) },
            onClearExtraction = { viewModel.clearOnboardingExtraction() },
            onSaveWorkspace = { name, industryId, services, area, hours, pricing, tone, mode, threshold, emergency, knowledge ->
                viewModel.saveNewBusiness(
                    name = name,
                    industryId = industryId,
                    services = services,
                    serviceArea = area,
                    businessHours = hours,
                    pricingApproach = pricing,
                    brandTone = tone,
                    autonomyMode = mode,
                    dollarThreshold = threshold,
                    emergencyAvailable = emergency,
                    knowledgeText = knowledge,
                    onCreated = {
                        showNewWorkspaceOnboarding = false
                        dismissedEmptyOnboarding = true
                        viewModel.clearOnboardingExtraction()
                    }
                )
            },
            onCancel = {
                if (canCancel) {
                    showNewWorkspaceOnboarding = false
                } else {
                    dismissedEmptyOnboarding = true
                }
                viewModel.clearOnboardingExtraction()
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    var showQuoteDialog by remember { mutableStateOf(false) }
    var quotePrefillJob by remember { mutableStateOf<Job?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val ownerFirstName = remember(viewModel.currentUserName, viewModel.currentUserEmail) {
        val clean = viewModel.currentUserName.trim()
        if (clean.isNotBlank()) clean.substringBefore(" ")
        else viewModel.currentUserEmail.substringBefore("@").ifBlank { "Owner" }
    }

    AuroraBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = AuroraCanvasDeep,
                            tonalElevation = 0.dp
                        ) {
                            PRIMARY_TABS.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true ||
                                    (tab.route is TasksRoute && currentDestination?.hasRoute(ApprovalsRoute::class) == true)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AuroraOnEmerald,
                                        selectedTextColor = AuroraEmerald,
                                        indicatorColor = AuroraEmerald,
                                        unselectedIconColor = AuroraTextMuted,
                                        unselectedTextColor = AuroraTextMuted
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = AuroraCanvasDeep,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.height(12.dp))
                            PRIMARY_TABS.forEach { tab ->
                                val selected = currentDestination?.hasRoute(tab.route::class) == true ||
                                    (tab.route is TasksRoute && currentDestination?.hasRoute(ApprovalsRoute::class) == true)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = AuroraOnEmerald,
                                        selectedTextColor = AuroraEmerald,
                                        indicatorColor = AuroraEmerald,
                                        unselectedIconColor = AuroraTextMuted,
                                        unselectedTextColor = AuroraTextMuted
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp)
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    ownerFirstName = ownerFirstName,
                                    business = activeBusiness,
                                    weather = weatherState,
                                    focusQuote = localPrefs.focusQuote,
                                    calls = calls,
                                    actions = actions,
                                    jobs = jobs,
                                    agentMessages = agentMessages,
                                    jobsState = jobsState,
                                    auditLogsState = auditLogsState,
                                    agentMessagesState = agentMessagesState,
                                    actionsState = actionsState,
                                    isOperationalSyncing = isOperationalSyncing,
                                    lastOperationalSyncLabel = lastOperationalSyncLabel,
                                    operationalSyncError = operationalSyncError,
                                    weeklyDispatchJobsState = weeklyDispatchJobsState,
                                    isWeeklyTrendsRefreshing = isWeeklyTrendsRefreshing,
                                    weeklyTrendsSyncLabel = weeklyTrendsSyncLabel,
                                    weeklyTrendsError = weeklyTrendsError,
                                    onRefreshOperationalFeeds = { viewModel.refreshOperationalFeeds() },
                                    onRefreshWeeklyTrends = { viewModel.refreshWeeklyDispatchTrends() },
                                    onUpdateJobStatus = { job, status -> viewModel.updateJobStatus(job, status) },
                                    onDismissAgentMessage = { viewModel.dismissAgentMessage(it) },
                                    onOpenNotifications = { showNotificationsDialog = true },
                                    onUpdateFocusQuote = { viewModel.updateFocusQuote(it) },
                                    onUpdateWeatherCity = { viewModel.updateWeatherCity(it) },
                                    onNavigateCalls = { navigateTopTab(CallsRoute) },
                                    onOpenCreateQuote = {
                                        quotePrefillJob = null
                                        showQuoteDialog = true
                                    },
                                    onNavigateSchedule = { navController.navigate(ScheduleRoute) },
                                    onNavigateCustomers = { navController.navigate(CustomersRoute) },
                                    onNavigateTasks = { navigateTopTab(TasksRoute) },
                                    onNavigateApprovals = { navController.navigate(ApprovalsRoute) }
                                )
                            }

                            composable<AiRoute> {
                                AiAssistantScreen(
                                    aiState = aiState,
                                    liveVoiceState = liveVoiceState,
                                    messages = chatMessages,
                                    onSelectEmployee = { viewModel.selectAiEmployee(it) },
                                    onSelectTaskProfile = { viewModel.selectAiTaskProfile(it) },
                                    onSendMessage = { prompt, docText, bmp ->
                                        viewModel.sendAiMessage(prompt, docText, bmp)
                                    },
                                    onStartLiveVoice = { viewModel.startLiveVoiceConversation() },
                                    onStopLiveVoice = { viewModel.stopLiveVoiceConversation() },
                                    onSendLiveVoiceNudge = { viewModel.sendLiveVoiceNudge(it) },
                                    onDismissLiveVoiceError = { viewModel.dismissLiveVoiceError() }
                                )
                            }

                            composable<CallsRoute> {
                                CallsScreen(
                                    business = activeBusiness,
                                    calls = calls,
                                    voicemails = voicemails,
                                    liveVoiceState = liveVoiceState,
                                    onToggleAiReceptionist = { viewModel.toggleAiReceptionist(it) },
                                    onStartLiveReceptionistCall = {
                                        viewModel.startLiveVoiceConversation(employeeIdOverride = "ai_receptionist")
                                    },
                                    onStopLiveReceptionistCall = { viewModel.stopLiveVoiceConversation() },
                                    onLogCall = { name, phone, dir, status, duration, summary ->
                                        viewModel.logCall(name, phone, dir, status, duration, summary)
                                    },
                                    onMarkVoicemailReviewed = { viewModel.markVoicemailReviewed(it) }
                                )
                            }

                            composable<TasksRoute> {
                                TasksScreen(
                                    tasks = tasks,
                                    actions = actions,
                                    initialFilter = TasksFilter.TODAY,
                                    onAddTask = { title, sub, due, bucket, cat ->
                                        viewModel.addTask(title, sub, due, bucket, cat)
                                    },
                                    onToggleTask = { viewModel.toggleTask(it) },
                                    onDecideApproval = { action, approved, draft, amt ->
                                        viewModel.decideApproval(action, approved, draft, amt)
                                    },
                                    onMarkInvoicePaid = { viewModel.markInvoicePaid(it) },
                                    onOpenCreateQuote = {
                                        quotePrefillJob = null
                                        showQuoteDialog = true
                                    }
                                )
                            }

                            composable<ApprovalsRoute> {
                                TasksScreen(
                                    tasks = tasks,
                                    actions = actions,
                                    initialFilter = TasksFilter.APPROVALS,
                                    onAddTask = { title, sub, due, bucket, cat ->
                                        viewModel.addTask(title, sub, due, bucket, cat)
                                    },
                                    onToggleTask = { viewModel.toggleTask(it) },
                                    onDecideApproval = { action, approved, draft, amt ->
                                        viewModel.decideApproval(action, approved, draft, amt)
                                    },
                                    onMarkInvoicePaid = { viewModel.markInvoicePaid(it) },
                                    onOpenCreateQuote = {
                                        quotePrefillJob = null
                                        showQuoteDialog = true
                                    }
                                )
                            }

                            composable<CustomersRoute> {
                                CustomersScreen(
                                    customers = customers,
                                    onBack = { navController.popBackStack() },
                                    onAddCustomer = { name, company, phone, email, addr, status, svc, notes ->
                                        viewModel.addCustomer(name, company, phone, email, addr, status, svc, notes)
                                    },
                                    onUpdateCustomerStatus = { cust, status ->
                                        viewModel.updateCustomerStatus(cust, status)
                                    },
                                    onDraftFollowUp = { cust, onReady ->
                                        viewModel.draftCustomerFollowUpAndSave(cust, onReady)
                                    }
                                )
                            }

                            composable<ScheduleRoute> {
                                ScheduleScreen(
                                    business = activeBusiness,
                                    jobs = jobs,
                                    onBack = { navController.popBackStack() },
                                    onAddJob = { title, client, phone, addr, date, time, dur, cat, value, prio, desc ->
                                        viewModel.addJob(title, client, phone, addr, date, time, dur, cat, value, prio, desc)
                                    },
                                    onUpdateJobStatus = { job, status ->
                                        viewModel.updateJobStatus(job, status)
                                    },
                                    onCreateInvoiceForJob = { job ->
                                        quotePrefillJob = job
                                        showQuoteDialog = true
                                    }
                                )
                            }

                            composable<SettingsRoute> {
                                AccountScreen(
                                    ownerName = viewModel.currentUserName,
                                    ownerEmail = viewModel.currentUserEmail,
                                    business = activeBusiness,
                                    allBusinesses = businesses,
                                    localPrefs = localPrefs,
                                    teamMessages = teamMessages,
                                    auditLogs = auditLogs,
                                    actions = actions,
                                    onSwitchBusiness = { viewModel.switchBusiness(it) },
                                    onCreateNewWorkspace = { showNewWorkspaceOnboarding = true },
                                    onUpdateBusiness = { updates, msg ->
                                        viewModel.updateBusinessProfile(updates, msg)
                                    },
                                    onToggleEmployee = { id, enabled -> viewModel.toggleEmployee(id, enabled) },
                                    onToggleAutomationMaster = { viewModel.toggleAutomationMaster(it) },
                                    onToggleAutomationRule = { id, enabled -> viewModel.toggleAutomationRule(id, enabled) },
                                    onSaveNotificationPrefs = { push, sound, vib, quiet ->
                                        viewModel.updateNotificationPrefs(push, sound, vib, quiet)
                                    },
                                    onSendTeamMessage = { viewModel.sendTeamMessage(it) },
                                    onDeleteWorkspace = {
                                        viewModel.deleteActiveWorkspace {
                                            navigateTopTab(HomeRoute)
                                        }
                                    },
                                    onSignOut = onSignOutRequested
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuoteDialog) {
        CreateQuoteOrInvoiceDialog(
            services = activeBusiness?.services.orEmpty(),
            prefillJob = quotePrefillJob,
            isGeneratingAi = aiState.isGeneratingQuote,
            isSaving = isMutating,
            aiSuggestion = aiState.lastQuoteSuggestion,
            onGenerateWithAi = { client, svc, notes ->
                viewModel.draftQuoteWithAi(client, svc, notes)
            },
            onClearAiSuggestion = { viewModel.clearQuoteSuggestion() },
            onSubmit = { client, title, summary, amount, draft, isInvoice, jobId ->
                viewModel.submitQuoteOrInvoice(
                    clientName = client,
                    title = title,
                    summary = summary,
                    amount = amount,
                    draftContent = draft,
                    isInvoice = isInvoice,
                    linkedJobId = jobId
                )
                showQuoteDialog = false
                quotePrefillJob = null
            },
            onDismiss = {
                showQuoteDialog = false
                quotePrefillJob = null
            }
        )
    }

    if (showNotificationsDialog) {
        val pendingApprovals = actions.count { it.status == ActionStatus.AWAITING_APPROVAL.code }
        NotificationsCenterDialog(
            agentMessages = agentMessages,
            pendingApprovalsCount = pendingApprovals,
            onDismissMessage = { viewModel.dismissAgentMessage(it) },
            onRouteAction = { route ->
                when (route.lowercase()) {
                    "ai" -> navigateTopTab(AiRoute)
                    "calls" -> navigateTopTab(CallsRoute)
                    "tasks" -> navigateTopTab(TasksRoute)
                    "customers" -> navController.navigate(CustomersRoute)
                    "schedule" -> navController.navigate(ScheduleRoute)
                    "approvals" -> navController.navigate(ApprovalsRoute)
                    else -> navigateTopTab(HomeRoute)
                }
            },
            onOpenApprovals = { navController.navigate(ApprovalsRoute) },
            onDismiss = { showNotificationsDialog = false }
        )
    }
}
