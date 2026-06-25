package com.opencode.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.opencode.mobile.data.PreferencesManager
import com.opencode.mobile.ui.screens.chat.ChatScreen
import com.opencode.mobile.ui.screens.connect.ConnectScreen
import com.opencode.mobile.ui.screens.local.LocalModeScreen
import com.opencode.mobile.ui.screens.scan.ScanScreen
import com.opencode.mobile.ui.screens.sessions.SessionsScreen
import com.opencode.mobile.ui.screens.settings.SettingsScreen

object Routes {
    const val CONNECT = "connect"
    const val SCAN = "scan"
    const val LOCAL = "local"
    const val SESSIONS = "sessions"
    const val CHAT = "chat/{sessionId}"
    const val SETTINGS = "settings"

    fun chatRoute(sessionId: String) = "chat/$sessionId"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    NavHost(navController = navController, startDestination = Routes.CONNECT) {
        composable(Routes.CONNECT) { backStackEntry ->
            val scannedUrl = backStackEntry.savedStateHandle.get<String>("scanned_url")
            ConnectScreen(
                preferencesManager = preferencesManager,
                scannedUrl = scannedUrl,
                onConnected = {
                    navController.navigate(Routes.SESSIONS) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                },
                onScanClick = { navController.navigate(Routes.SCAN) },
                onLocalClick = { navController.navigate(Routes.LOCAL) }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onScanResult = { url ->
                    navController.popBackStack()
                    navController.currentBackStackEntry?.savedStateHandle?.set("scanned_url", url)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.LOCAL) {
            LocalModeScreen(
                onUseLocal = { url ->
                    navController.popBackStack()
                    navController.currentBackStackEntry?.savedStateHandle?.set("scanned_url", url)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SESSIONS) {
            SessionsScreen(
                preferencesManager = preferencesManager,
                onSessionSelected = { sessionId ->
                    navController.navigate(Routes.chatRoute(sessionId))
                },
                onDisconnect = {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CHAT) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            ChatScreen(
                sessionId = sessionId,
                preferencesManager = preferencesManager,
                onBack = { navController.popBackStack() },
                onDisconnect = {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                preferencesManager = preferencesManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
