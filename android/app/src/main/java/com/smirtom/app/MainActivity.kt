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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smirtom.app.data.CalendarRepository
import com.smirtom.app.data.PreferencesManager
import com.smirtom.app.ui.HomeScreen
import com.smirtom.app.ui.HomeViewModel
import com.smirtom.app.ui.HomeViewModelFactory
import com.smirtom.app.ui.SettingsScreen
import com.smirtom.app.ui.SettingsViewModel
import com.smirtom.app.ui.SettingsViewModelFactory
import com.smirtom.app.ui.SmirtomTheme

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
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT,
                        darkScrim = Color.TRANSPARENT
                    ) { darkTheme }
                )
                onDispose { }
            }

            SmirtomTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModelFactory(repository)
                    )
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(repository, preferencesManager)
                    )
                    val homeState by homeViewModel.uiState.collectAsState()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                uiState = homeState,
                                onRefresh = { homeViewModel.refresh(force = true) },
                                onOpenSettings = { navController.navigate("settings") },
                                onFilterChange = { homeViewModel.setFilter(it) }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
