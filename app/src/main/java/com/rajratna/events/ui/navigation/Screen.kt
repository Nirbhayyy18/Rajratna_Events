package com.rajratna.events.ui.navigation

/**
 * All navigation routes in the app.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object NewOrder : Screen("new_order")
    data object EditOrder : Screen("edit_order/{orderId}") {
        fun createRoute(orderId: Long) = "edit_order/$orderId"
    }
    data object OrdersList : Screen("orders_list")
    data object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: Long) = "order_details/$orderId"
    }
    data object Customers : Screen("customers")
    data object CustomerDetails : Screen("customer_details/{customerId}") {
        fun createRoute(customerId: Long) = "customer_details/$customerId"
    }
    data object Payments : Screen("payments")
    data object Reports : Screen("reports")
    data object ItemsRates : Screen("items_rates")
    data object Backup : Screen("backup")
    data object Returns : Screen("returns")
    data object More : Screen("more")
}
