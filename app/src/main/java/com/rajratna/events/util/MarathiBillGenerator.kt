package com.rajratna.events.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import java.io.File
import java.io.FileOutputStream

/**
 * Generates professional Marathi bills as PDF and Image.
 * Uses bundled Noto Sans Devanagari font for correct Marathi rendering.
 * Single shared drawBill() method ensures PDF and image content are identical.
 */
object MarathiBillGenerator {

    // ── Marathi Labels ──────────────────────────────────────

    private const val HEADER_BLESSING = "|| श्री गणेश प्रसन्न ||"
    private const val BUSINESS_NAME = "राजरत्न इव्हेंट्स, आंदरुड"
    private const val BUSINESS_ADDRESS = "ता. फलटण, जि. सातारा"
    private const val BUSINESS_TAGLINE = "आमच्याकडे सर्व कार्यक्रमासाठी केटरिंग टेबल, खुर्ची व थंडगार पाण्याचे जार योग्य दरात मिळतील"
    private const val OWNER_NAME = "प्रो. चैतन्य राजेंद्र राऊत"
    private const val OWNER_MOBILE = "मो. ९११२८२३२१३"

    private const val LABEL_BILL_NO = "बिल क्र.:"
    private const val LABEL_DATE = "दिनांक:"
    private const val LABEL_CUSTOMER_NAME = "ग्राहकाचे नाव:"
    private const val LABEL_MOBILE = "मोबाईल क्रमांक:"
    private const val LABEL_ADDRESS = "पत्ता:"
    private const val LABEL_DELIVERY_DATE = "डिलिव्हरी दिनांक:"
    private const val LABEL_RETURN_DATE = "परत दिनांक:"
    private const val LABEL_RENTAL_DAYS = "भाडे दिवस:"

    // Table headers
    private const val TH_SR = "अ.क्र."
    private const val TH_DETAILS = "तपशील"
    private const val TH_QTY = "नग"
    private const val TH_RATE = "दर"
    private const val TH_DAYS = "दिवस"
    private const val TH_AMOUNT = "एकूण"

    // Totals
    private const val LABEL_TOTAL = "एकूण रक्कम"
    private const val LABEL_PAID = "भरलेली रक्कम"
    private const val LABEL_BALANCE = "बाकी रक्कम"
    private const val LABEL_TRANSPORT = "गाडी भाडे"

    // Footer
    private const val LABEL_NOTE = "टीप:"
    private const val NOTE_TEXT = "वस्तू गहाळ किंवा खराब झाल्यास भरपाई आकारली जाईल."
    private const val LABEL_CUSTOMER_SIGN = "ग्राहकाची सही"
    private const val LABEL_PROPRIETOR = "प्रोप्रायटर"

    // ── Item Name Mapping (English → Marathi) ───────────────

    private val itemNameMap = mapOf(
        "chair" to "खुर्ची",
        "table" to "टेबल",
        "water jar" to "पाण्याचे जार",
        "waterjar" to "पाण्याचे जार"
    )

    private fun getMarathiItemName(englishName: String, isCustomerOwned: Boolean): String {
        val marathiName = itemNameMap[englishName.lowercase().trim()] ?: englishName
        return if (isCustomerOwned) "$marathiName - ग्राहकाचे जार" else marathiName
    }

    // ── Page Dimensions ─────────────────────────────────────

    // A4 at 72 DPI: 595 x 842 points
    private const val PAGE_WIDTH = 595f
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    // ── Font Loading ────────────────────────────────────────

    private var regularTypeface: Typeface? = null
    private var boldTypeface: Typeface? = null

    private fun getRegularTypeface(context: Context): Typeface {
        if (regularTypeface == null) {
            regularTypeface = try {
                Typeface.createFromAsset(context.assets, "fonts/NotoSansDevanagari-Regular.ttf")
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
        return regularTypeface!!
    }

    private fun getBoldTypeface(context: Context): Typeface {
        if (boldTypeface == null) {
            boldTypeface = try {
                Typeface.createFromAsset(context.assets, "fonts/NotoSansDevanagari-Bold.ttf")
            } catch (e: Exception) {
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        }
        return boldTypeface!!
    }

    // ── Public API ──────────────────────────────────────────

    /**
     * Generate a PDF file of the Marathi bill.
     * @return the generated PDF File in cache directory
     */
    fun generatePdf(
        context: Context,
        order: Order,
        items: List<OrderItem>,
        totalPaid: Double
    ): File {
        val pageHeight = calculatePageHeight(context, order, items)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), pageHeight.toInt(), 1).create()
        val page = document.startPage(pageInfo)

        drawBill(context, page.canvas, PAGE_WIDTH, pageHeight, order, items, totalPaid)

        document.finishPage(page)

        val sanitizedName = order.customerName.replace(Regex("[^a-zA-Z0-9]"), "_").take(20)
        val file = File(context.cacheDir, "Rajratna_Bill_${order.billNumber}_${sanitizedName}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    /**
     * Generate a PNG image of the Marathi bill.
     * @return the generated PNG File in cache directory
     */
    fun generateImage(
        context: Context,
        order: Order,
        items: List<OrderItem>,
        totalPaid: Double
    ): File {
        // Use 2x scale for crisp image on phone screens
        val scale = 2f
        val width = (PAGE_WIDTH * scale).toInt()
        val pageHeight = calculatePageHeight(context, order, items)
        val height = (pageHeight * scale).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        drawBill(context, canvas, PAGE_WIDTH, pageHeight, order, items, totalPaid)

        val sanitizedName = order.customerName.replace(Regex("[^a-zA-Z0-9]"), "_").take(20)
        val file = File(context.cacheDir, "Rajratna_Bill_${order.billNumber}_${sanitizedName}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()

        return file
    }

    /**
     * Generate the short Marathi summary for WhatsApp message body.
     */
    fun getWhatsAppSummary(order: Order, totalPaid: Double): String {
        val balance = order.grandTotal - totalPaid
        return buildString {
            appendLine("राजरत्न इव्हेंट्स")
            appendLine("बिल क्र.: #${order.billNumber}")
            appendLine("एकूण रक्कम: ₹${order.grandTotal.toInt()}")
            appendLine("भरलेली रक्कम: ₹${totalPaid.toInt()}")
            appendLine("बाकी रक्कम: ₹${balance.toInt()}")
        }
    }

    /**
     * Generate a Bitmap for preview (no file saved).
     */
    fun generateBitmap(
        context: Context,
        order: Order,
        items: List<OrderItem>,
        totalPaid: Double
    ): Bitmap {
        val scale = 2f
        val pageHeight = calculatePageHeight(context, order, items)
        val width = (PAGE_WIDTH * scale).toInt()
        val height = (pageHeight * scale).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)

        drawBill(context, canvas, PAGE_WIDTH, pageHeight, order, items, totalPaid)
        return bitmap
    }

    // ── Height Calculation ───────────────────────────────────

    private fun calculatePageHeight(
        context: Context,
        order: Order,
        items: List<OrderItem>
    ): Float {
        val regular = getRegularTypeface(context)
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 11f
        }
        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9f
        }

        var h = 0f
        // Top padding
        h += 30f
        // Blessing
        h += 20f + 10f
        // Business name + address
        h += 16f + 6f + 14f + 6f
        // Tagline (wrapped)
        h += measureWrappedTextHeight(BUSINESS_TAGLINE, taglinePaint, CONTENT_WIDTH) + 6f
        // Owner name + mobile
        h += 13f + 4f + 13f + 16f
        // Thin separator
        h += 1f + 12f
        // Bill no + date row
        h += 14f + 12f
        // Customer details (name, mobile, address, delivery, return, rental)
        h += 14f + 6f // name
        h += 14f + 6f // mobile
        if (order.customerAddress.isNotBlank()) {
            h += measureWrappedTextHeight("$LABEL_ADDRESS ${order.customerAddress}", bodyPaint, CONTENT_WIDTH) + 6f
        }
        h += 14f + 6f // delivery date
        h += 14f + 6f // return date
        h += 14f + 16f // rental days
        // Separator
        h += 1f + 8f
        // Table header
        h += 14f + 8f + 1f + 8f
        // Item rows
        items.forEach { item ->
            val marathiName = getMarathiItemName(item.itemName, item.isCustomerOwned)
            val rowH = measureWrappedTextHeight(marathiName, bodyPaint, 140f)
            h += maxOf(16f, rowH) + 6f
        }
        // Transport row
        if (order.transportRent > 0) {
            h += 16f + 6f
        }
        // Separator
        h += 1f + 10f
        // Totals (3 rows)
        h += 16f + 8f // total
        h += 16f + 8f // paid
        h += 18f + 16f // balance (bold, larger)
        // Separator
        h += 1f + 16f
        // Note
        h += 12f + 4f
        h += measureWrappedTextHeight(NOTE_TEXT, bodyPaint, CONTENT_WIDTH) + 30f
        // Signature area
        h += 1f + 8f // line
        h += 14f + 30f // labels
        // Bottom padding
        h += 20f

        return maxOf(h, 600f) // minimum page height
    }

    // ── Core Draw Method ────────────────────────────────────

    private fun drawBill(
        context: Context,
        canvas: Canvas,
        width: Float,
        height: Float,
        order: Order,
        items: List<OrderItem>,
        totalPaid: Double
    ) {
        val regular = getRegularTypeface(context)
        val bold = getBoldTypeface(context)
        val balance = order.grandTotal - totalPaid

        // ── Paints ──────────────────────────────────────────
        val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC"); style = Paint.Style.STROKE; strokeWidth = 1.5f
        }
        val blessingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 16f; color = Color.parseColor("#B71C1C"); textAlign = Paint.Align.CENTER
        }
        val businessNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 14f; color = Color.parseColor("#1A1A1A"); textAlign = Paint.Align.CENTER
        }
        val businessAddrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 11f; color = Color.parseColor("#444444"); textAlign = Paint.Align.CENTER
        }
        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9f; color = Color.parseColor("#666666"); textAlign = Paint.Align.CENTER
        }
        val ownerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11f; color = Color.parseColor("#333333"); textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11f; color = Color.parseColor("#333333")
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 11f; color = Color.parseColor("#1A1A1A")
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10f; color = Color.parseColor("#1A1A1A")
        }
        val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 10f; color = Color.parseColor("#333333")
        }
        val amountCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 10f; color = Color.parseColor("#333333"); textAlign = Paint.Align.RIGHT
        }
        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 12f; color = Color.parseColor("#1A1A1A")
        }
        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 12f; color = Color.parseColor("#1A1A1A"); textAlign = Paint.Align.RIGHT
        }
        val balanceLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 13f; color = Color.parseColor("#B71C1C")
        }
        val balanceValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 13f; color = Color.parseColor("#B71C1C"); textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DDDDDD"); strokeWidth = 0.8f
        }
        val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC"); strokeWidth = 0.8f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9f; color = Color.parseColor("#666666")
        }
        val noteBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 9f; color = Color.parseColor("#666666")
        }
        val signPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 10f; color = Color.parseColor("#666666")
        }

        val centerX = width / 2f
        val rightX = width - MARGIN_RIGHT

        // ── Background & Border ─────────────────────────────
        canvas.drawRect(0f, 0f, width, height, bgPaint)
        canvas.drawRect(8f, 8f, width - 8f, height - 8f, borderPaint)

        var y = 30f

        // ══════════════════════════════════════════════════════
        // HEADER
        // ══════════════════════════════════════════════════════

        // Blessing
        canvas.drawText(HEADER_BLESSING, centerX, y + 16f, blessingPaint)
        y += 20f + 10f

        // Business name
        canvas.drawText(BUSINESS_NAME, centerX, y + 14f, businessNamePaint)
        y += 16f + 6f

        // Business address
        canvas.drawText(BUSINESS_ADDRESS, centerX, y + 11f, businessAddrPaint)
        y += 14f + 6f

        // Tagline (centered, wrapped)
        y = drawWrappedTextCentered(canvas, BUSINESS_TAGLINE, taglinePaint, centerX, y, CONTENT_WIDTH)
        y += 6f

        // Owner name
        canvas.drawText(OWNER_NAME, centerX, y + 11f, ownerPaint)
        y += 13f + 4f

        // Owner mobile
        canvas.drawText(OWNER_MOBILE, centerX, y + 11f, businessAddrPaint)
        y += 13f + 16f

        // Thin separator
        canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        y += 1f + 12f

        // ══════════════════════════════════════════════════════
        // BILL INFO & CUSTOMER DETAILS
        // ══════════════════════════════════════════════════════

        // Bill number & date on the same row
        canvas.drawText("$LABEL_BILL_NO #${order.billNumber}", MARGIN_LEFT, y + 12f, labelPaint)
        val dateText = "$LABEL_DATE ${DateUtils.formatMarathiDate(order.orderDate)}"
        canvas.drawText(dateText, rightX - valuePaint.measureText(dateText), y + 12f, valuePaint)
        y += 14f + 12f

        // Customer name
        drawLabelValue(canvas, LABEL_CUSTOMER_NAME, order.customerName, MARGIN_LEFT, y, labelPaint, valuePaint)
        y += 14f + 6f

        // Mobile
        drawLabelValue(canvas, LABEL_MOBILE, order.customerMobile, MARGIN_LEFT, y, labelPaint, valuePaint)
        y += 14f + 6f

        // Address (wrapping)
        if (order.customerAddress.isNotBlank()) {
            val addressText = "$LABEL_ADDRESS ${order.customerAddress}"
            y = drawWrappedText(canvas, addressText, valuePaint, MARGIN_LEFT, y, CONTENT_WIDTH)
            y += 6f
        }

        // Delivery date
        drawLabelValue(canvas, LABEL_DELIVERY_DATE, DateUtils.formatMarathiDate(order.deliveryDate), MARGIN_LEFT, y, labelPaint, valuePaint)
        y += 14f + 6f

        // Return date
        drawLabelValue(canvas, LABEL_RETURN_DATE, DateUtils.formatMarathiDate(order.returnDate), MARGIN_LEFT, y, labelPaint, valuePaint)
        y += 14f + 6f

        // Rental days
        drawLabelValue(canvas, LABEL_RENTAL_DAYS, "${order.rentalDays}", MARGIN_LEFT, y, labelPaint, valuePaint)
        y += 14f + 16f

        // ══════════════════════════════════════════════════════
        // ITEM TABLE
        // ══════════════════════════════════════════════════════

        // Separator
        canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        y += 1f + 8f

        // Table column positions
        val colSr = MARGIN_LEFT
        val colDetails = MARGIN_LEFT + 40f
        val colQty = MARGIN_LEFT + 200f
        val colRate = MARGIN_LEFT + 250f
        val colDays = MARGIN_LEFT + 310f
        val colAmount = rightX

        // Header row
        canvas.drawText(TH_SR, colSr, y + 10f, tableHeaderPaint)
        canvas.drawText(TH_DETAILS, colDetails, y + 10f, tableHeaderPaint)
        canvas.drawText(TH_QTY, colQty, y + 10f, tableHeaderPaint)
        canvas.drawText(TH_RATE, colRate, y + 10f, tableHeaderPaint)
        canvas.drawText(TH_DAYS, colDays, y + 10f, tableHeaderPaint)
        canvas.drawText(TH_AMOUNT, colAmount, y + 10f, Paint(amountCellPaint).apply { typeface = bold; textAlign = Paint.Align.RIGHT })
        y += 14f + 8f

        // Header separator
        canvas.drawLine(MARGIN_LEFT, y, rightX, y, dashedLinePaint)
        y += 1f + 8f

        // Item rows
        items.forEachIndexed { index, item ->
            val marathiName = getMarathiItemName(item.itemName, item.isCustomerOwned)

            canvas.drawText("${index + 1}", colSr, y + 10f, tableCellPaint)

            // Draw item name (may wrap)
            val nameWidth = 140f
            val nameLines = wrapText(marathiName, tableCellPaint, nameWidth)
            nameLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, colDetails, y + 10f + (lineIdx * 14f), tableCellPaint)
            }

            canvas.drawText("${item.quantity}", colQty, y + 10f, tableCellPaint)
            canvas.drawText("₹${item.ratePerDay.toInt()}", colRate, y + 10f, tableCellPaint)
            canvas.drawText("${item.rentalDays}", colDays, y + 10f, tableCellPaint)
            canvas.drawText("₹${item.totalAmount.toInt()}", colAmount, y + 10f, amountCellPaint)

            val rowHeight = maxOf(16f, nameLines.size * 14f)
            y += rowHeight + 6f
        }

        // Transport rent row
        if (order.transportRent > 0) {
            val transportIdx = items.size + 1
            canvas.drawText("$transportIdx", colSr, y + 10f, tableCellPaint)
            canvas.drawText(LABEL_TRANSPORT, colDetails, y + 10f, tableCellPaint)
            canvas.drawText("₹${order.transportRent.toInt()}", colAmount, y + 10f, amountCellPaint)
            y += 16f + 6f
        }

        // ══════════════════════════════════════════════════════
        // TOTALS
        // ══════════════════════════════════════════════════════

        // Separator
        canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        y += 1f + 10f

        // Total
        canvas.drawText(LABEL_TOTAL, MARGIN_LEFT, y + 12f, totalLabelPaint)
        canvas.drawText("₹${String.format("%,d", order.grandTotal.toInt())}", rightX, y + 12f, totalValuePaint)
        y += 16f + 8f

        // Paid
        canvas.drawText(LABEL_PAID, MARGIN_LEFT, y + 12f, totalLabelPaint)
        canvas.drawText("₹${String.format("%,d", totalPaid.toInt())}", rightX, y + 12f, totalValuePaint)
        y += 16f + 8f

        // Balance (highlighted)
        canvas.drawText(LABEL_BALANCE, MARGIN_LEFT, y + 14f, balanceLabelPaint)
        canvas.drawText("₹${String.format("%,d", balance.toInt())}", rightX, y + 14f, balanceValuePaint)
        y += 18f + 16f

        // ══════════════════════════════════════════════════════
        // FOOTER
        // ══════════════════════════════════════════════════════

        // Separator
        canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        y += 1f + 16f

        // Note
        canvas.drawText(LABEL_NOTE, MARGIN_LEFT, y + 10f, noteBoldPaint)
        y += 12f + 4f
        y = drawWrappedText(canvas, NOTE_TEXT, notePaint, MARGIN_LEFT, y, CONTENT_WIDTH)
        y += 30f

        // Signature line
        val signLineLeft = MARGIN_LEFT + 20f
        val signLineRight = MARGIN_LEFT + 140f
        val propLineLeft = rightX - 140f
        val propLineRight = rightX - 20f

        canvas.drawLine(signLineLeft, y, signLineRight, y, linePaint)
        canvas.drawLine(propLineLeft, y, propLineRight, y, linePaint)
        y += 1f + 8f

        // Signature labels
        val custSignX = (signLineLeft + signLineRight) / 2f
        val propSignX = (propLineLeft + propLineRight) / 2f
        val centeredSignPaint = Paint(signPaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(LABEL_CUSTOMER_SIGN, custSignX, y + 10f, centeredSignPaint)
        canvas.drawText(LABEL_PROPRIETOR, propSignX, y + 10f, centeredSignPaint)
    }

    // ── Drawing Helpers ─────────────────────────────────────

    private fun drawLabelValue(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        canvas.drawText(label, x, y + 12f, labelPaint)
        val labelWidth = labelPaint.measureText(label)
        canvas.drawText(" $value", x + labelWidth, y + 12f, valuePaint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        startY: Float,
        maxWidth: Float
    ): Float {
        val lines = wrapText(text, paint, maxWidth)
        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, x, y + 10f, paint)
            y += 13f
        }
        return y
    }

    private fun drawWrappedTextCentered(
        canvas: Canvas,
        text: String,
        paint: Paint,
        centerX: Float,
        startY: Float,
        maxWidth: Float
    ): Float {
        val measuringPaint = Paint(paint).apply { textAlign = Paint.Align.LEFT }
        val lines = wrapText(text, measuringPaint, maxWidth)
        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, centerX, y + 10f, paint)
            y += 12f
        }
        return y
    }

    private fun measureWrappedTextHeight(text: String, paint: Paint, maxWidth: Float): Float {
        val lines = wrapText(text, paint, maxWidth)
        return lines.size * 13f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (maxWidth <= 0f) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        return if (lines.isEmpty()) listOf(text) else lines
    }
}
