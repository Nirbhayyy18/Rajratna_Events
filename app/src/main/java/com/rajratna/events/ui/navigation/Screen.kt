package com.rajratna.events.ui.navigation

/**
 * All navigation routes in the app.
 * IDs are now String-based (Firestore document IDs).
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object NewOrder : Screen("new_order")
    data object EditOrder : Screen("edit_order/{orderId}") {
        fun createRoute(orderId: String) = "edit_order/$orderId"
    }
    data object OrdersList : Screen("orders_list")
    data object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: String) = "order_details/$orderId"
    }
    data object Customers : Screen("customers")
    data object CustomerDetails : Screen("customer_details/{customerId}") {
        fun createRoute(customerId: String) = "customer_details/$customerId"
    }
    data object Payments : Screen("payments")
    data object Reports : Screen("reports")
    data object ItemsRates : Screen("items_rates")
    data object Backup : Screen("backup")
    data object Returns : Screen("returns")
    data object More : Screen("more")
    data object BillPreview : Screen("bill_preview/{orderId}") {
        fun createRoute(orderId: String) = "bill_preview/$orderId"
    }
}
