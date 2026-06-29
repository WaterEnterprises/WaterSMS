package jv.watersms.enterprises.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jv.watersms.enterprises.ui.SmsSettingsScreen
import jv.watersms.enterprises.ui.SmsCreateCampaignScreen
import jv.watersms.enterprises.ui.SmsDashboardScreen
import jv.watersms.enterprises.ui.SmsViewModel
import jv.watersms.enterprises.ui.components.WaterSmsLogo

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Route
)

val bottomNavItems = listOf(
    BottomNavItem("Launch", Icons.AutoMirrored.Filled.Send, Route.Launch),
    BottomNavItem("Campaigns", Icons.Default.Campaign, Route.Campaigns),
    BottomNavItem("Settings", Icons.Default.Settings, Route.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    viewModel: SmsViewModel,
    onNavigateToCampaigns: () -> Unit = {}
) {
    val campaigns by viewModel.campaigns.collectAsState()
    val isAnyCampaignSending = campaigns.any { it.status == "SENDING" }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WaterSmsLogo(size = 36.dp)
                Text(
                    text = "WaterSMS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White
        ),
        actions = {
            if (isAnyCampaignSending) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            "Sending Bulk...",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: SmsViewModel = hiltViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route.route }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (showBottomBar) {
                AppTopBar(
                    viewModel = viewModel,
                    onNavigateToCampaigns = {
                        navController.navigate(Route.Campaigns.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.Black.copy(alpha = 0.4f),
                    tonalElevation = 0.dp,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Launch.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
        ) {
            composable(Route.Launch.route) {
                SmsCreateCampaignScreen(
                    viewModel = viewModel,
                    onCampaignCreated = {
                        navController.navigate(Route.Campaigns.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Route.Campaigns.route) {
                SmsDashboardScreen(
                    viewModel = viewModel,
                    onCreateNewCampaign = {
                        navController.navigate(Route.Launch.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCampaignClick = { campaignId ->
                        navController.navigate(Route.CampaignDetail.createRoute(campaignId))
                    }
                )
            }
            composable(
                route = Route.CampaignDetail.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.LongType })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getLong("campaignId") ?: return@composable
                LaunchedEffect(campaignId) {
                    viewModel.selectCampaign(campaignId)
                }
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.selectCampaign(null)
                    }
                }
                SmsDashboardScreen(
                    viewModel = viewModel,
                    onCreateNewCampaign = { },
                    onCampaignClick = { },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.Settings.route) {
                SmsSettingsScreen(viewModel = viewModel)
            }
        }
    }
}
