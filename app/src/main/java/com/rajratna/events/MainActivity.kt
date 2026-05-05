package com.rajratna.events

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rajratna.events.ui.navigation.AppNavGraph
import com.rajratna.events.ui.navigation.Screen
import com.rajratna.events.ui.theme.RajratnaEventsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RajratnaEventsTheme {
                val navController = rememberNavController()
                MainAppScaffold(navController = navController)
            }
        }
    }
}

/**
 * Bottom navigation item definition.
 */
data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Top-level routes that show the bottom navigation bar.
 */
val topLevelRoutes = listOf(
    Screen.Dashboard.route,
    Screen.OrdersList.route,
    Screen.Returns.route,
    Screen.ItemsRates.route,
    Screen.More.route
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Dashboard.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Orders", Screen.OrdersList.route, Icons.Filled.Receipt, Icons.Outlined.Receipt),
    BottomNavItem("Returns", Screen.Returns.route, Icons.Filled.AssignmentReturn, Icons.Outlined.AssignmentReturn),
    BottomNavItem("Inventory", Screen.ItemsRates.route, Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    BottomNavItem("More", Screen.More.route, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

@Composable
fun MainAppScaffold(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on top-level destinations
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = NavigationBarDefaults.Elevation,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        // Pop up to the start destination to avoid building up a large back stack
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
