package com.rajratna.events.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import java.io.File

/**
 * Utility to generate WhatsApp messages and share/call actions.
 */
object WhatsAppUtils {

    /**
     * Generate bill message for WhatsApp sharing.
     */
    fun generateBillMessage(order: Order, items: List<OrderItem>): String {
        val sb = StringBuilder()
        sb.appendLine("॥ श्री ॥")
        sb.appendLine("*राजरत्न इव्हेंट्स अँड वॉटर सप्लायर्स*")
        sb.appendLine("अंद्रुड, ता. फलटण, जि. सातारा")
        sb.appendLine("मो. 9922676759 / 9011929394")
        sb.appendLine("──────────────────────")
        sb.appendLine("*बिल पावती क्र.:* ${order.billNumber}")
        sb.appendLine("*ग्राहक:* ${order.customerName}")
        if (order.customerMobile.isNotBlank()) {
            sb.appendLine("*मोबाईल:* ${order.customerMobile}")
        }
        sb.appendLine("*ऑर्डर दिनांक:* ${DateUtils.formatDate(order.orderDate)}")
        sb.appendLine("*पोहोच दिनांक:* ${DateUtils.formatDate(order.deliveryDate)}")
        sb.appendLine("*परत दिनांक:* ${DateUtils.formatDate(order.returnDate)}")
        sb.appendLine("*भाडे दिवस:* ${order.rentalDays}")
        sb.appendLine("──────────────────────")
        sb.appendLine("*साहित्य तपशील:*")

        items.forEach { item ->
            val suffix = if (item.isCustomerOwned) " (स्वतःचा जार)" else ""
            sb.appendLine("• ${item.itemName}$suffix: ${item.quantity} नग x ₹${item.ratePerDay.toInt()} x ${item.rentalDays} दिवस = ₹${item.totalAmount.toInt()}")
        }

        sb.appendLine("──────────────────────")
        sb.appendLine("*एकूण भाडे:* ₹${order.itemsTotal.toInt()}")
        if (order.transportRent > 0) {
            sb.appendLine("*वाहतूक भाडे:* ₹${order.transportRent.toInt()}")
        }
        if (order.discountAmount > 0) {
            sb.appendLine("*सूट:* ₹${order.discountAmount.toInt()}")
        }
        sb.appendLine("*एकूण देय रक्कम:* ₹${order.grandTotal.toInt()}")
        sb.appendLine("*जमा रक्कम:* ₹${(order.grandTotal - order.balanceAmount).toInt()}")
        sb.appendLine("*उर्वरित बाकी:* ₹${order.balanceAmount.toInt()}")
        sb.appendLine("──────────────────────")
        sb.appendLine("धन्यवाद! पुन्हा सेवेची संधी द्यावी.")

        return sb.toString()
    }

    /**
     * Generate payment reminder message.
     */
    fun generatePaymentReminder(order: Order): String {
        return """
॥ श्री ॥
*राजरत्न इव्हेंट्स अँड वॉटर सप्लायर्स*
अंद्रुड, ता. फलटण, जि. सातारा
मो. 9922676759 / 9011929394
──────────────────────
सस्नेह नमस्कार *${order.customerName}* जी,

आपल्या बिल पावती क्र. *${order.billNumber}* ची शिल्लक बाकी रक्कम *₹${order.balanceAmount.toInt()}* येणे बाकी आहे.

कृपया आपल्या सोयीनुसार रक्कम जमा करावी ही नम्र विनंती.
धन्यवाद!
        """.trimIndent()
    }

    /**
     * Generate order confirmation message.
     */
    fun generateOrderConfirmation(order: Order): String {
        return """
॥ श्री ॥
*राजरत्न इव्हेंट्स अँड वॉटर सप्लायर्स*
अंद्रुड, ता. फलटण, जि. सातारा
मो. 9922676759 / 9011929394
──────────────────────
सस्नेह नमस्कार *${order.customerName}* जी,

आपली ऑर्डर यशस्वीरीत्या नोंदवण्यात आली आहे.

*बिल पावती क्र.:* ${order.billNumber}
*पोहोच दिनांक:* ${DateUtils.formatDate(order.deliveryDate)}
*परत दिनांक:* ${DateUtils.formatDate(order.returnDate)}
*एकूण देय रक्कम:* ₹${order.grandTotal.toInt()}
*ऍडव्हान्स जमा:* ₹${order.advancePaid.toInt()}
*उर्वरित बाकी:* ₹${order.balanceAmount.toInt()}
──────────────────────
धन्यवाद! राजरत्न इव्हेंट्स सदैव आपल्या सेवेत.
        """.trimIndent()
    }

    /**
     * Generate return reminder message for pending items.
     */
    fun generateReturnReminder(order: Order, pendingItems: List<OrderItem>): String {
        val sb = StringBuilder()
        sb.appendLine("॥ श्री ॥")
        sb.appendLine("*राजरत्न इव्हेंट्स अँड वॉटर सप्लायर्स*")
        sb.appendLine("मो. 9922676759 / 9011929394")
        sb.appendLine("──────────────────────")
        sb.appendLine("सस्नेह नमस्कार *${order.customerName}* जी,")
        sb.appendLine()
        sb.appendLine("आपल्या बिल पावती क्र. *${order.billNumber}* मधील खालील साहित्य परत जमा करणे बाकी आहे:")
        sb.appendLine()
        pendingItems.forEach { item ->
            val pending = item.quantity - item.returnedQuantity - item.damagedQuantity
            if (pending > 0) {
                sb.appendLine("• *${item.itemName}:* $pending नग")
            }
        }
        sb.appendLine()
        sb.appendLine("*नियोजित परत दिनांक:* ${DateUtils.formatDate(order.returnDate)}")
        sb.appendLine()
        sb.appendLine("कृपया साहित्य लवकरात लवकर जमा करावे ही नम्र विनंती.")
        sb.appendLine("धन्यवाद!")

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
     * Share a file (PDF/image) on WhatsApp with an optional text message.
     * Falls back to generic share sheet if WhatsApp is not installed.
     */
    fun shareFileOnWhatsApp(context: Context, phoneNumber: String, file: File, mimeType: String, message: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        try {
            val formattedNumber = if (phoneNumber.startsWith("+")) {
                phoneNumber.replace("+", "").replace(" ", "")
            } else if (phoneNumber.length == 10) {
                "91$phoneNumber"
            } else {
                phoneNumber.replace(" ", "")
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("jid", "$formattedNumber@s.whatsapp.net")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share
            shareFile(context, file, mimeType, message)
        }
    }

    /**
     * Share a file via the generic Android share sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String, message: String = "") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (message.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, message)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Bill"))
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

    /**
     * Generate jar/payment summary for WhatsApp sharing.
     * Answers customer questions like "kiti jar zale total?", "balance kiti aahe?"
     */
    fun generateCustomerJarSummary(
        customerName: String,
        thisMonthJarCount: Int,
        thisMonthJarAmount: Double,
        paidAmount: Double,
        pendingBalance: Double,
        pendingReturnJars: Int,
        advanceBalance: Double = 0.0
    ): String {
        val sb = StringBuilder()
        sb.appendLine("॥ श्री ॥")
        sb.appendLine("*राजरत्न वॉटर सप्लायर्स*")
        sb.appendLine("अंद्रुड, ता. फलटण, जि. सातारा")
        sb.appendLine("मो. 9922676759 / 9011929394")
        sb.appendLine("──────────────────────")
        sb.appendLine("*चालू महिन्याचा पाणी जार हिशोब*")
        sb.appendLine("ग्राहक: *$customerName*")
        sb.appendLine("──────────────────────")
        sb.appendLine("• एकूण घेतलेले जार: *$thisMonthJarCount नग*")
        sb.appendLine("• एकूण बिल रक्कम: *₹${thisMonthJarAmount.toInt()}*")
        sb.appendLine("• जमा रक्कम: *₹${paidAmount.toInt()}*")
        sb.appendLine("• उर्वरित बाकी: *₹${pendingBalance.toInt()}*")
        if (advanceBalance > 0) {
            sb.appendLine("• जादा जमा (Advance): *₹${advanceBalance.toInt()}*")
        }
        if (pendingReturnJars > 0) {
            sb.appendLine("• रिकामे जार परत येणे बाकी: *$pendingReturnJars जार*")
        }
        sb.appendLine("──────────────────────")
        sb.appendLine("काही शंका असल्यास संपर्क साधावा.")
        sb.appendLine("धन्यवाद!")
        return sb.toString()
    }
}

