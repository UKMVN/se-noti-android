package com.senoti.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.senoti.app.ui.screens.NotificationDetailScreen
import com.senoti.app.ui.screens.NotificationListScreen
import com.senoti.app.ui.screens.PublishLogScreen
import com.senoti.app.ui.screens.SettingsScreen
import com.senoti.app.viewmodel.NotificationViewModel
import com.senoti.app.viewmodel.PublishLogViewModel
import com.senoti.app.viewmodel.SettingsViewModel

object Routes {
    const val NOTIFICATION_LIST = "notification_list"
    const val NOTIFICATION_DETAIL = "notification_detail/{notificationId}"
    const val SETTINGS = "settings"
    const val PUBLISH_LOG = "publish_log"

    fun notificationDetail(id: Long) = "notification_detail/$id"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: NotificationViewModel,
    settingsViewModel: SettingsViewModel,
    publishLogViewModel: PublishLogViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.NOTIFICATION_LIST
    ) {
        composable(Routes.NOTIFICATION_LIST) {
            NotificationListScreen(
                viewModel = viewModel,
                onNotificationClick = { id ->
                    navController.navigate(Routes.notificationDetail(id))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onPublishLogClick = {
                    navController.navigate(Routes.PUBLISH_LOG)
                }
            )
        }

        composable(
            route = Routes.NOTIFICATION_DETAIL,
            arguments = listOf(
                navArgument("notificationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getLong("notificationId") ?: return@composable
            NotificationDetailScreen(
                notificationId = notificationId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PUBLISH_LOG) {
            PublishLogScreen(
                viewModel = publishLogViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
