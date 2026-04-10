package com.factory.securesnapvault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.factory.securesnapvault.billing.BillingManager
import com.factory.securesnapvault.billing.PremiumManager
import com.factory.securesnapvault.ui.screens.auth.AuthScreen
import com.factory.securesnapvault.ui.screens.paywall.PaywallScreen
import com.factory.securesnapvault.ui.screens.settings.SettingsScreen
import com.factory.securesnapvault.ui.screens.vault.VaultScreen
import com.factory.securesnapvault.ui.screens.vault.VaultViewModel
import com.factory.securesnapvault.ui.screens.viewer.MediaViewerScreen

object Routes {
    const val AUTH = "auth"
    const val VAULT = "vault"
    const val VIEWER = "viewer/{mediaId}"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"

    fun viewerRoute(mediaId: Long) = "viewer/$mediaId"
}

@Composable
fun VaultNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val premiumManager = remember { PremiumManager(context) }
    val billingManager = remember { BillingManager(context, premiumManager) }
    val isPremium by premiumManager.isPremium.collectAsState(initial = false)

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.VAULT) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.VAULT) {
            val vaultViewModel: VaultViewModel = viewModel()
            VaultScreen(
                viewModel = vaultViewModel,
                isPremium = isPremium,
                onMediaClick = { mediaId ->
                    navController.navigate(Routes.viewerRoute(mediaId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onPaywallClick = {
                    navController.navigate(Routes.PAYWALL)
                }
            )
        }

        composable(
            route = Routes.VIEWER,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: return@composable
            MediaViewerScreen(
                mediaId = mediaId,
                isPremium = isPremium,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onPaywallClick = { navController.navigate(Routes.PAYWALL) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                isPremium = isPremium,
                onBack = { navController.popBackStack() },
                onUpgradeClick = { navController.navigate(Routes.PAYWALL) }
            )
        }

        composable(Routes.PAYWALL) {
            PaywallScreen(
                billingManager = billingManager,
                onClose = { navController.popBackStack() },
                onPurchaseSuccess = { navController.popBackStack() }
            )
        }
    }
}
