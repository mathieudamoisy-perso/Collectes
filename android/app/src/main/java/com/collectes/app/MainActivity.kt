package com.collectes.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.collectes.app.data.CalendarRepository
import com.collectes.app.data.PreferencesManager
import com.collectes.app.data.VexinCommune
import com.collectes.app.data.VexinCommunes
import com.collectes.app.data.WasteType
import com.collectes.app.notifications.NotificationHelper
import com.collectes.app.ui.AppTab
import com.collectes.app.ui.BottomBarOverlay
import com.collectes.app.ui.CollectesTheme
import com.collectes.app.ui.OnboardingSetupScreen
import com.collectes.app.ui.GuideScreen
import com.collectes.app.ui.HomeScreen
import com.collectes.app.ui.HomeViewModel
import com.collectes.app.ui.HomeViewModelFactory
import com.collectes.app.ui.LocalBottomBarInset
import com.collectes.app.ui.LocalPagerNestedScroll
import com.collectes.app.ui.ReliabilitySetupScreen
import com.collectes.app.ui.SettingsScreen
import com.collectes.app.ui.SettingsViewModel
import com.collectes.app.ui.SettingsViewModelFactory
import com.collectes.app.ui.rememberBottomBarFallbackHeight
import com.collectes.app.ui.rememberBottomBarInset
import com.collectes.app.ui.rememberPagerNestedScrollConnection
import com.collectes.app.util.BatteryOptimizationHelper
import com.collectes.app.util.ExactAlarmHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        enableEdgeToEdge()

        val repository = CalendarRepository(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val useBrandColors by preferencesManager.useBrandColors.collectAsState(initial = true)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            // Initial non-null : 1er frame = vraie UI (évite splash Android 12+ coincé).
            val communeSetupDone by preferencesManager.hasCompletedCommuneSetup.collectAsState(initial = false)
            val reliabilitySetupDone by preferencesManager.hasCompletedReliabilitySetup.collectAsState(initial = true)
            var needsReliabilityCatchUp by remember { mutableStateOf(false) }
            var calendarPrefetchJob by remember { mutableStateOf<Job?>(null) }
            val lifecycleOwner = LocalLifecycleOwner.current

            fun refreshReliabilityCatchUp(communeDone: Boolean, reliabilityDone: Boolean) {
                needsReliabilityCatchUp = communeDone &&
                    !reliabilityDone &&
                    (
                        !NotificationHelper.canPostNotifications(context) ||
                            !ExactAlarmHelper.canScheduleExactAlarms(context) ||
                            !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    )
            }

            LaunchedEffect(communeSetupDone, reliabilitySetupDone) {
                refreshReliabilityCatchUp(communeSetupDone, reliabilitySetupDone)
                if (communeSetupDone && !reliabilitySetupDone) {
                    val missing = !NotificationHelper.canPostNotifications(context) ||
                        !ExactAlarmHelper.canScheduleExactAlarms(context) ||
                        !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    if (!missing) {
                        preferencesManager.setReliabilitySetupDone(true)
                    }
                }
                reportFullyDrawn()
            }
            DisposableEffect(lifecycleOwner, context, communeSetupDone, reliabilitySetupDone) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        refreshReliabilityCatchUp(communeSetupDone, reliabilitySetupDone)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            CollectesTheme(useBrandColors = useBrandColors) {
                val surfaceColor = MaterialTheme.colorScheme.surface
                DisposableEffect(darkTheme, surfaceColor) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            lightScrim = Color.TRANSPARENT,
                            darkScrim = Color.TRANSPARENT
                        ) { darkTheme },
                        navigationBarStyle = SystemBarStyle.auto(
                            lightScrim = surfaceColor.toArgb(),
                            darkScrim = surfaceColor.toArgb()
                        ) { darkTheme }
                    )
                    onDispose { }
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    when {
                        !communeSetupDone -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(WindowInsets.statusBars.asPaddingValues())
                        ) {
                            OnboardingSetupScreen(
                                communes = VexinCommunes.all,
                                onCommuneSelected = { commune ->
                                    calendarPrefetchJob?.cancel()
                                    calendarPrefetchJob = scope.launch {
                                        try {
                                            repository.prefetchCalendar(commune)
                                        } catch (_: CancellationException) {
                                            // changement de commune pendant l’onboarding
                                        }
                                    }
                                },
                                onSetupComplete = { commune, reminderTimeMinutes ->
                                    scope.launch {
                                        completeOnboarding(
                                            repository = repository,
                                            preferencesManager = preferencesManager,
                                            commune = commune,
                                            reminderTimeMinutes = reminderTimeMinutes,
                                            prefetchJob = calendarPrefetchJob
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        needsReliabilityCatchUp -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(WindowInsets.statusBars.asPaddingValues())
                        ) {
                            ReliabilitySetupScreen(
                                onContinue = {
                                    scope.launch {
                                        preferencesManager.setReliabilitySetupDone(true)
                                    }
                                },
                                stepLabel = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> CollectesMainApp(
                            repository = repository,
                            preferencesManager = preferencesManager
                        )
                    }
                }
            }
        }
    }

    private suspend fun completeOnboarding(
        repository: CalendarRepository,
        preferencesManager: PreferencesManager,
        commune: VexinCommune,
        reminderTimeMinutes: Int,
        prefetchJob: Job?
    ) {
        try {
            prefetchJob?.join()
        } catch (_: CancellationException) {
            // prefetch annulé : sync ci-dessous si besoin
        }
        preferencesManager.completeInitialSetup(commune, reminderTimeMinutes)
        if (!repository.hasCachedCalendar()) {
            repository.ensureCalendarSynced(force = true)
        } else {
            repository.ensureCalendarSynced(force = false)
        }
        repository.rescheduleReminders(reminderTimeMinutes)
    }
}

@Composable
private fun CollectesMainApp(
    repository: CalendarRepository,
    preferencesManager: PreferencesManager
) {
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(repository, preferencesManager)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(repository, preferencesManager)
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val tabs = AppTab.entries
    var guideDetailType by remember { mutableStateOf<WasteType?>(null) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    var bottomBarMeasuredHeight by remember { mutableStateOf(0.dp) }
    val bottomBarFallbackHeight = rememberBottomBarFallbackHeight()
    val bottomBarHeight = if (bottomBarMeasuredHeight > 0.dp) {
        bottomBarMeasuredHeight
    } else {
        bottomBarFallbackHeight
    }
    val bottomBarInset = rememberBottomBarInset(bottomBarHeight)
    val pagerNestedScrollConnection = rememberPagerNestedScrollConnection(pagerState)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (tabs[page] != AppTab.Guide) {
                guideDetailType = null
            }
        }
    }

    fun selectTab(tab: AppTab) {
        val page = tabs.indexOf(tab)
        if (tab != AppTab.Guide) {
            guideDetailType = null
        }
        scope.launch {
            pagerState.animateScrollToPage(page)
        }
    }

    DisposableEffect(lifecycleOwner, homeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.reloadDates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        CompositionLocalProvider(
            LocalPagerNestedScroll provides pagerNestedScrollConnection,
            LocalBottomBarInset provides bottomBarInset
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
                beyondViewportPageCount = 1,
                pageSpacing = 0.dp
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { clip = true }
                ) {
                    when (tabs[page]) {
                        AppTab.Collectes -> CollectesTab(
                            homeViewModel = homeViewModel,
                            settingsViewModel = settingsViewModel,
                            onRefresh = { homeViewModel.refresh(force = true) },
                            onFilterChange = { homeViewModel.setFilter(it) },
                            onNextCollectionClick = { type ->
                                homeViewModel.setFilter(type)
                                guideDetailType = null
                                selectTab(AppTab.Collectes)
                            }
                        )
                        AppTab.Guide -> GuideTab(
                            settingsViewModel = settingsViewModel,
                            guideDetailType = guideDetailType,
                            onGuideDetailConsumed = { guideDetailType = null },
                            onNextCollectionClick = { type ->
                                homeViewModel.setFilter(type)
                                guideDetailType = null
                                selectTab(AppTab.Collectes)
                            },
                            homeViewModel = homeViewModel
                        )
                        AppTab.Settings -> SettingsScreen(
                            viewModel = settingsViewModel,
                            showBack = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        BottomBarOverlay(
            pagerState = pagerState,
            onTabSelected = ::selectTab,
            onHeightChanged = { bottomBarMeasuredHeight = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun CollectesTab(
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    onRefresh: () -> Unit,
    onFilterChange: (WasteType?) -> Unit,
    onNextCollectionClick: (WasteType) -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val commune by settingsViewModel.selectedCommune.collectAsState()

    HomeScreen(
        uiState = homeState,
        commune = commune,
        communes = settingsViewModel.communes,
        onCommuneSelected = { settingsViewModel.setCommune(it) },
        onRefresh = onRefresh,
        onFilterChange = onFilterChange,
        onNextCollectionClick = onNextCollectionClick,
        nextCollectionDate = { type -> homeViewModel.findNextCollection(type) },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun GuideTab(
    settingsViewModel: SettingsViewModel,
    homeViewModel: HomeViewModel,
    guideDetailType: WasteType?,
    onGuideDetailConsumed: () -> Unit,
    onNextCollectionClick: (WasteType) -> Unit
) {
    val commune by settingsViewModel.selectedCommune.collectAsState()

    GuideScreen(
        commune = commune,
        initialDetailType = guideDetailType,
        onInitialDetailConsumed = onGuideDetailConsumed,
        onNextCollectionClick = onNextCollectionClick,
        nextCollectionDate = { type -> homeViewModel.findNextCollection(type) },
        modifier = Modifier.fillMaxSize()
    )
}
