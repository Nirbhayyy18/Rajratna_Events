package com.rajratna.events.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem

/**
 * Utility to generate WhatsApp messages and share/call actions.
 */
object WhatsAppUtils {

    /**
     * Generate bill message for WhatsApp sharing.
     */
    fun generateBillMessage(order: Order, items: List<OrderItem>): String {
        val sb = StringBuilder()
        sb.appendLine("Bill No: ${order.billNumber}")
        sb.appendLine()
        sb.appendLine("Customer: ${order.customerName}")
        sb.appendLine("Mobile: ${order.customerMobile}")
        sb.appendLine()
        sb.appendLine("Order Date: ${DateUtils.formatDate(order.orderDate)}")
        sb.appendLine("Delivery Date: ${DateUtils.formatDate(order.deliveryDate)}")
        sb.appendLine("Return Date: ${DateUtils.formatDate(order.returnDate)}")
        sb.appendLine("Rental Days: ${order.rentalDays}")
        sb.appendLine()
        sb.appendLine("Items:")

        items.forEach { item ->
            sb.appendLine("${item.itemName}: ${item.quantity} x ${item.ratePerDay.toInt()} x ${item.rentalDays} days = ${item.totalAmount.toInt()}")
        }

        sb.appendLine()
        sb.appendLine("Items Total: ${order.itemsTotal.toInt()} rs")
        if (order.transportRent > 0) {
            sb.appendLine("Transport Rent: ${order.transportRent.toInt()} rs")
        }
        sb.appendLine("Grand Total: ${order.grandTotal.toInt()} rs")
        sb.appendLine()
        sb.appendLine("Paid: ${(order.grandTotal - order.balanceAmount).toInt()} rs")
        sb.appendLine("Balance: ${order.balanceAmount.toInt()} rs")

        return sb.toString()
    }

    /**
     * Generate payment reminder message.
     */
    fun generatePaymentReminder(order: Order): String {
        return """
Hello ${order.customerName},

Your pending balance for Bill No. ${order.billNumber} is ${order.balanceAmount.toInt()} rs.

Please make payment when possible.
        """.trimIndent()
    }

    /**
     * Generate order confirmation message.
     */
    fun generateOrderConfirmation(order: Order): String {
        return """
Hello ${order.customerName},

Your order has been confirmed.

Bill No: ${order.billNumber}
Delivery Date: ${DateUtils.formatDate(order.deliveryDate)}
Return Date: ${DateUtils.formatDate(order.returnDate)}
Grand Total: ${order.grandTotal.toInt()} rs
Balance: ${order.balanceAmount.toInt()} rs
        """.trimIndent()
    }

    /**
     * Generate return reminder message for pending items.
     */
    fun generateReturnReminder(order: Order, pendingItems: List<OrderItem>): String {
        val sb = StringBuilder()
        sb.appendLine("Hello ${order.customerName},")
        sb.appendLine()
        sb.appendLine("Your rented items for Bill No. ${order.billNumber} are pending return.")
        sb.appendLine()
        sb.appendLine("Pending items:")
        pendingItems.forEach { item ->
            val pending = item.quantity - item.returnedQuantity
            if (pending > 0) {
                sb.appendLine("${item.itemName}: $pending")
            }
        }
        sb.appendLine()
        sb.appendLine("Return Date: ${DateUtils.formatDate(order.returnDate)}")
        sb.appendLine()
        sb.appendLine("Please return the items as soon as possible.")

        return sb.toString()
    }

    /**
     * Open WhatsApp with a pre-filled message to the customer's number.
     */
    fun shareOnWhatsApp(context: Context, phoneNumber: String, message: String) {
        try {
            // Format phone number - add India country code if not present
            val formattedNumber = if (phoneNumber.startsWith("+")) {
                phoneNumber.replace("+", "").replace(" ", "")
            } else if (phoneNumber.length == 10) {
                "91$phoneNumber"
            } else {
                phoneNumber.replace(" ", "")
            }

            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed, use regular share
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, message)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }
    }

    /**
     * Initiate a phone call.
     */
    fun callCustomer(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
