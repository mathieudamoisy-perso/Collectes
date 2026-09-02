package com.smirtom.app

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smirtom.app.data.CalendarRepository
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.data.WasteType
import com.smirtom.app.ui.AppTab
import com.smirtom.app.ui.GuideScreen
import com.smirtom.app.ui.HomeScreen
import com.smirtom.app.ui.HomeViewModel
import com.smirtom.app.ui.HomeViewModelFactory
import com.smirtom.app.ui.SettingsScreen
import com.smirtom.app.ui.SettingsViewModel
import com.smirtom.app.ui.SettingsViewModelFactory
import com.smirtom.app.ui.BottomBarOverlay
import com.smirtom.app.ui.SmirtomTheme
import com.smirtom.app.ui.LocalPagerNestedScroll
import com.smirtom.app.ui.rememberBottomBarHideScrollConnection
import com.smirtom.app.ui.rememberBottomBarInset
import com.smirtom.app.ui.rememberBottomBarFallbackHeight
import com.smirtom.app.ui.rememberBottomBarScrollState
import com.smirtom.app.ui.rememberBottomBarVisibility
import com.smirtom.app.ui.rememberPagerNestedScrollConnection
import com.smirtom.app.ui.LocalBottomBarHideScroll
import com.smirtom.app.ui.LocalBottomBarInset
import com.smirtom.app.ui.LocalBottomBarVisibility
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val repository = CalendarRepository(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)

        setContent {
            val darkTheme = isSystemInDarkTheme()

            SmirtomTheme {
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
                    val bottomBarScrollState = rememberBottomBarScrollState()
                    var bottomBarMeasuredHeight by remember { mutableStateOf(0.dp) }
                    val bottomBarFallbackHeight = rememberBottomBarFallbackHeight()
                    val bottomBarHeight = if (bottomBarMeasuredHeight > 0.dp) {
                        bottomBarMeasuredHeight
                    } else {
                        bottomBarFallbackHeight
                    }
                    val bottomBarInset = rememberBottomBarInset(bottomBarHeight)
                    val bottomBarVisibility = rememberBottomBarVisibility(bottomBarScrollState)
                    val pagerNestedScrollConnection = rememberPagerNestedScrollConnection(pagerState)
                    val bottomBarHideScrollConnection = rememberBottomBarHideScrollConnection(
                        bottomBarScrollState
                    )

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.settledPage }.collect { page ->
                            if (tabs[page] != AppTab.Guide) {
                                guideDetailType = null
                            }
                            bottomBarScrollState.show()
                        }
                    }

                    fun selectTab(tab: AppTab) {
                        val page = tabs.indexOf(tab)
                        if (tab != AppTab.Guide) {
                            guideDetailType = null
                        }
                        bottomBarScrollState.show()
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
                            LocalBottomBarHideScroll provides bottomBarHideScrollConnection,
                            LocalBottomBarInset provides bottomBarInset,
                            LocalBottomBarVisibility provides bottomBarVisibility
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
                                beyondViewportPageCount = tabs.lastIndex,
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
                            scrollState = bottomBarScrollState,
                            onTabSelected = ::selectTab,
                            onHeightChanged = { bottomBarMeasuredHeight = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
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
