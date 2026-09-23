package com.residentsleeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.residentsleeper.ui.dashboard.DashboardScreen
import com.residentsleeper.ui.dashboard.DashboardViewModel
import com.residentsleeper.ui.review.ReviewScreen
import com.residentsleeper.ui.review.ReviewViewModel
import com.residentsleeper.ui.settings.SettingsScreen
import com.residentsleeper.ui.settings.SettingsViewModel
import com.residentsleeper.ui.theme.ResidentSleeperTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            ResidentSleeperTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") {
                        val dashboardViewModel: DashboardViewModel = viewModel()
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToReviews = { navController.navigate("reviews") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("reviews") {
                        val reviewViewModel: ReviewViewModel = viewModel()
                        ReviewScreen(
                            viewModel = reviewViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
