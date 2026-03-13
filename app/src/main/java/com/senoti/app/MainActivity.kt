package com.senoti.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.senoti.app.ui.navigation.NavGraph
import com.senoti.app.ui.screens.PermissionScreen
import com.senoti.app.ui.theme.SeNotiTheme
import com.senoti.app.viewmodel.NotificationViewModel
import com.senoti.app.viewmodel.PublishLogViewModel
import com.senoti.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var hasNotificationAccess by mutableStateOf(false)
    private var showWelcomeScreen by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appVersion = getAppVersionName()

        setContent {
            SeNotiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LaunchedEffect(showWelcomeScreen) {
                        if (showWelcomeScreen) {
                            delay(1800)
                            showWelcomeScreen = false
                        }
                    }
                    if (showWelcomeScreen) {
                        com.senoti.app.ui.screens.WelcomeScreen(
                            appVersion = appVersion
                        )
                    } else if (hasNotificationAccess) {
                        MainContent()
                    } else {
                        PermissionScreen(
                            onRequestPermission = {
                                openNotificationListenerSettings()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasNotificationAccess = isNotificationListenerEnabled()
    }

    @Composable
    private fun MainContent() {
        val app = application as SeNotiApplication
        val viewModel: NotificationViewModel = viewModel(
            factory = NotificationViewModel.Factory(app.repository)
        )
        val settingsViewModel: SettingsViewModel = viewModel(
            factory = SettingsViewModel.Factory(app.settingsRepository)
        )
        val publishLogViewModel: PublishLogViewModel = viewModel(
            factory = PublishLogViewModel.Factory(
                app.database.publishLogDao(),
                app.settingsRepository
            )
        )
        val navController = rememberNavController()

        NavGraph(
            navController = navController,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            publishLogViewModel = publishLogViewModel
        )
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(this, com.senoti.app.service.NotificationListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    private fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }
}
