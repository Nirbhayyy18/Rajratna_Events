package com.rajratna.events.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rajratna.events.ui.screens.backup.BackupScreen
import com.rajratna.events.ui.screens.customerdetails.CustomerDetailsScreen
import com.rajratna.events.ui.screens.customers.CustomersScreen
import com.rajratna.events.ui.screens.dashboard.DashboardScreen
import com.rajratna.events.ui.screens.items.ItemsScreen
import com.rajratna.events.ui.screens.neworder.NewOrderScreen
import com.rajratna.events.ui.screens.orderdetails.OrderDetailsScreen
import com.rajratna.events.ui.screens.orders.OrdersListScreen
import com.rajratna.events.ui.screens.payments.PaymentsScreen
import com.rajratna.events.ui.screens.reports.ReportsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToNewOrder = { navController.navigate(Screen.NewOrder.route) },
                onNavigateToOrders = { navController.navigate(Screen.OrdersList.route) },
                onNavigateToCustomers = { navController.navigate(Screen.Customers.route) },
                onNavigateToPayments = { navController.navigate(Screen.Payments.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToItems = { navController.navigate(Screen.ItemsRates.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) }
            )
        }

        // New Order
        composable(Screen.NewOrder.route) {
            NewOrderScreen(
                onNavigateBack = { navController.popBackStack() },
                onOrderSaved = { orderId ->
                    navController.popBackStack()
                    navController.navigate(Screen.OrderDetails.createRoute(orderId))
                }
            )
        }

        // Edit Order
        composable(
            route = Screen.EditOrder.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            NewOrderScreen(
                onNavigateBack = { navController.popBackStack() },
                onOrderSaved = { navController.popBackStack() },
                editOrderId = orderId
            )
        }

        // Orders List
        composable(Screen.OrdersList.route) {
            OrdersListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrder = { navController.navigate(Screen.OrderDetails.createRoute(it)) },
                onNavigateToNewOrder = { navController.navigate(Screen.NewOrder.route) }
            )
        }

        // Order Details
        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            OrderDetailsScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() },
                onEditOrder = { navController.navigate(Screen.EditOrder.createRoute(it)) }
            )
        }

        // Customers
        composable(Screen.Customers.route) {
            CustomersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCustomer = { navController.navigate(Screen.CustomerDetails.createRoute(it)) }
            )
        }

        // Customer Details
        composable(
            route = Screen.CustomerDetails.route,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            CustomerDetailsScreen(
                customerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrder = { navController.navigate(Screen.OrderDetails.createRoute(it)) },
                onNavigateToNewOrder = { navController.navigate(Screen.NewOrder.route) }
            )
        }

        // Payments
        composable(Screen.Payments.route) {
            PaymentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrder = { navController.navigate(Screen.OrderDetails.createRoute(it)) }
            )
        }

        // Reports
        composable(Screen.Reports.route) {
            ReportsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Items & Rates
        composable(Screen.ItemsRates.route) {
            ItemsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Backup & Restore
        composable(Screen.Backup.route) {
            BackupScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
