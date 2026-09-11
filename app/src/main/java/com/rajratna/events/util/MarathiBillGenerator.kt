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
    private const val LABEL_DISCOUNT = "सवलत / सूट"
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

    // A4 at 72 DPI: 595 width
    private const val PAGE_WIDTH = 595f
    private const val MARGIN_LEFT = 32f
    private const val MARGIN_RIGHT = 32f
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
            if (order.discountAmount > 0) {
                appendLine("सवलत: ₹${order.discountAmount.toInt()}")
            }
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
        var h = 0f
        // Top margin
        h += 16f
        // Header (blessing, business name, address/mobile, proprietor, tagline)
        h += 15f + 22f + 16f + 14f + 14f + 8f // ~89f
        // Customer & Bill Details Card (2-column)
        val custCardHeight = if (order.customerAddress.isNotBlank()) 62f else 48f
        h += custCardHeight + 8f
        // Table header
        h += 24f
        // Table item rows
        val regular = getRegularTypeface(context)
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = regular; textSize = 11f }
        items.forEach { item ->
            val marathiName = getMarathiItemName(item.itemName, item.isCustomerOwned)
            val lines = wrapText(marathiName, cellPaint, 240f)
            val rowH = maxOf(22f, lines.size * 14f + 6f)
            h += rowH
        }
        // Transport row if any
        if (order.transportRent > 0) {
            h += 22f
        }
        // Gap after table
        h += 10f
        // Side-by-side Totals, Note, and Signatures section
        h += 118f
        // Bottom margin
        h += 16f

        return h
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
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5"); style = Paint.Style.STROKE; strokeWidth = 1.2f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ECEFF1"); style = Paint.Style.STROKE; strokeWidth = 0.8f
        }
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9"); style = Paint.Style.FILL
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0"); strokeWidth = 0.8f
        }
        val noteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF2F2"); style = Paint.Style.FILL
        }
        val noteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FECACA"); style = Paint.Style.STROKE; strokeWidth = 0.8f
        }

        // Typography Paints
        val blessingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 12.5f; color = Color.parseColor("#B71C1C"); textAlign = Paint.Align.CENTER
        }
        val businessNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 18f; color = Color.parseColor("#0F172A"); textAlign = Paint.Align.CENTER
        }
        val businessInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10.5f; color = Color.parseColor("#334155"); textAlign = Paint.Align.CENTER
        }
        val proprietorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9.5f; color = Color.parseColor("#475569"); textAlign = Paint.Align.CENTER
        }
        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9f; color = Color.parseColor("#64748B"); textAlign = Paint.Align.CENTER
        }

        val labelBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10.5f; color = Color.parseColor("#334155")
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 10.5f; color = Color.parseColor("#0F172A")
        }
        val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11.5f; color = Color.parseColor("#0F172A")
        }

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10.5f; color = Color.parseColor("#1E293B")
        }
        val thCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10.5f; color = Color.parseColor("#1E293B"); textAlign = Paint.Align.CENTER
        }
        val thRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10.5f; color = Color.parseColor("#1E293B"); textAlign = Paint.Align.RIGHT
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 11f; color = Color.parseColor("#1E293B")
        }
        val cellCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 11f; color = Color.parseColor("#1E293B"); textAlign = Paint.Align.CENTER
        }
        val cellRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 11f; color = Color.parseColor("#1E293B"); textAlign = Paint.Align.RIGHT
        }
        val cellAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11.5f; color = Color.parseColor("#0F172A"); textAlign = Paint.Align.RIGHT
        }

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11f; color = Color.parseColor("#334155")
        }
        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11.5f; color = Color.parseColor("#0F172A"); textAlign = Paint.Align.RIGHT
        }
        val paidValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 11.5f; color = Color.parseColor("#15803D"); textAlign = Paint.Align.RIGHT
        }
        val balanceLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 12.5f; color = Color.parseColor("#DC2626")
        }
        val balanceValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 13f; color = Color.parseColor("#DC2626"); textAlign = Paint.Align.RIGHT
        }

        val noteTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9f; color = Color.parseColor("#991B1B")
        }
        val noteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 9.5f; color = Color.parseColor("#991B1B")
        }
        val signLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = regular; textSize = 9.5f; color = Color.parseColor("#475569"); textAlign = Paint.Align.CENTER
        }
        val signPropLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold; textSize = 10f; color = Color.parseColor("#0F172A"); textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val rightX = width - MARGIN_RIGHT

        // ── Outer Background & Frame ────────────────────────
        canvas.drawRect(0f, 0f, width, height, bgPaint)
        canvas.drawRect(6f, 6f, width - 6f, height - 6f, outerBorderPaint)
        canvas.drawRect(9f, 9f, width - 9f, height - 9f, innerBorderPaint)

        var y = 16f

        // ══════════════════════════════════════════════════════
        // 1. HEADER (Compact & Elegant)
        // ══════════════════════════════════════════════════════

        // Blessing
        canvas.drawText(HEADER_BLESSING, centerX, y + 11f, blessingPaint)
        y += 15f

        // Business name
        canvas.drawText(BUSINESS_NAME, centerX, y + 16f, businessNamePaint)
        y += 22f

        // Address & Mobile in one clear line
        val businessContactText = "$BUSINESS_ADDRESS   |   $OWNER_MOBILE"
        canvas.drawText(businessContactText, centerX, y + 11f, businessInfoPaint)
        y += 16f

        // Proprietor
        canvas.drawText(OWNER_NAME, centerX, y + 10f, proprietorPaint)
        y += 14f

        // Services Tagline
        canvas.drawText(BUSINESS_TAGLINE, centerX, y + 9f, taglinePaint)
        y += 14f

        // Header separator
        canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        y += 8f

        // ══════════════════════════════════════════════════════
        // 2. CUSTOMER & BILL INFO (Compact 2-Column Grid Box)
        // ══════════════════════════════════════════════════════

        val hasAddress = order.customerAddress.isNotBlank()
        val cardHeight = if (hasAddress) 62f else 48f
        val cardRect = RectF(MARGIN_LEFT, y, rightX, y + cardHeight)
        canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 6f, 6f, cardBorderPaint)

        val col1X = MARGIN_LEFT + 10f
        val col2X = centerX + 12f
        var cardRowY = y + 13f

        // Row 1: Customer Name (Left) | Bill No & Date (Right)
        canvas.drawText(LABEL_CUSTOMER_NAME, col1X, cardRowY, labelBoldPaint)
        val nameLabelWidth = labelBoldPaint.measureText(LABEL_CUSTOMER_NAME)
        canvas.drawText(" ${order.customerName}", col1X + nameLabelWidth, cardRowY, valueBoldPaint)

        val billNoText = "$LABEL_BILL_NO #${order.billNumber}   $LABEL_DATE ${DateUtils.formatMarathiDate(order.orderDate)}"
        canvas.drawText(billNoText, col2X, cardRowY, labelBoldPaint)

        // Row 2: Mobile (Left) | Delivery Date (Right)
        cardRowY += 16f
        canvas.drawText(LABEL_MOBILE, col1X, cardRowY, labelBoldPaint)
        val mobileLabelWidth = labelBoldPaint.measureText(LABEL_MOBILE)
        canvas.drawText(" ${order.customerMobile.ifBlank { "-" }}", col1X + mobileLabelWidth, cardRowY, valuePaint)

        val deliveryText = "$LABEL_DELIVERY_DATE ${DateUtils.formatMarathiDate(order.deliveryDate)}"
        canvas.drawText(deliveryText, col2X, cardRowY, valuePaint)

        // Row 3: Address (Left, if any) | Return Date & Days (Right, if applicable)
        if (hasAddress || order.rentalDays > 1 || order.returnDate != order.deliveryDate) {
            cardRowY += 16f
            if (hasAddress) {
                val addrText = "$LABEL_ADDRESS ${order.customerAddress.take(45)}"
                canvas.drawText(addrText, col1X, cardRowY, valuePaint)
            }
            if (order.rentalDays > 1 || order.returnDate != order.deliveryDate) {
                val returnText = "$LABEL_RETURN_DATE ${DateUtils.formatMarathiDate(order.returnDate)} (${order.rentalDays} दिवस)"
                canvas.drawText(returnText, col2X, cardRowY, valuePaint)
            }
        }

        y += cardHeight + 8f

        // ══════════════════════════════════════════════════════
        // 3. ITEM TABLE (Spacious Columns, Clear Headers)
        // ══════════════════════════════════════════════════════

        // Column Layout (Total: 531pt)
        val colSr = MARGIN_LEFT + 15f         // Center (~30pt)
        val colDetails = MARGIN_LEFT + 36f    // Left (~250pt)
        val colQty = MARGIN_LEFT + 295f       // Center (~40pt)
        val colRate = MARGIN_LEFT + 360f      // Right (~50pt)
        val colDays = MARGIN_LEFT + 405f      // Center (~40pt)
        val colAmount = rightX - 6f           // Right (~90pt)

        // Table Header Bar
        val headerHeight = 24f
        val headerRect = RectF(MARGIN_LEFT, y, rightX, y + headerHeight)
        canvas.drawRoundRect(headerRect, 4f, 4f, tableHeaderBgPaint)
        canvas.drawRoundRect(headerRect, 4f, 4f, cardBorderPaint)

        val headerTextY = y + 16f
        canvas.drawText(TH_SR, colSr, headerTextY, thCenterPaint)
        canvas.drawText(TH_DETAILS, colDetails, headerTextY, thPaint)
        canvas.drawText(TH_QTY, colQty, headerTextY, thCenterPaint)
        canvas.drawText(TH_RATE, colRate, headerTextY, thRightPaint)
        canvas.drawText(TH_DAYS, colDays, headerTextY, thCenterPaint)
        canvas.drawText(TH_AMOUNT, colAmount, headerTextY, thRightPaint)
        y += headerHeight

        // Item Rows
        items.forEachIndexed { index, item ->
            val marathiName = getMarathiItemName(item.itemName, item.isCustomerOwned)
            val nameLines = wrapText(marathiName, cellPaint, 240f)
            val rowHeight = maxOf(22f, nameLines.size * 14f + 6f)

            val rowBaseY = y + 15f
            canvas.drawText("${index + 1}", colSr, rowBaseY, cellCenterPaint)

            nameLines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, colDetails, y + 15f + (lineIdx * 14f), cellPaint)
            }

            canvas.drawText("${item.quantity}", colQty, rowBaseY, cellCenterPaint)
            canvas.drawText("₹${item.ratePerDay.toInt()}", colRate, rowBaseY, cellRightPaint)
            val daysDisplay = if (item.isCustomerOwned) "-" else "${item.rentalDays}"
            canvas.drawText(daysDisplay, colDays, rowBaseY, cellCenterPaint)
            canvas.drawText("₹${item.totalAmount.toInt()}", colAmount, rowBaseY, cellAmountPaint)

            y += rowHeight
            canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        }

        // Transport Row (if any)
        if (order.transportRent > 0) {
            val transportRowY = y + 15f
            val transportIdx = items.size + 1
            canvas.drawText("$transportIdx", colSr, transportRowY, cellCenterPaint)
            canvas.drawText(LABEL_TRANSPORT, colDetails, transportRowY, cellPaint)
            canvas.drawText("-", colQty, transportRowY, cellCenterPaint)
            canvas.drawText("-", colRate, transportRowY, cellRightPaint)
            canvas.drawText("-", colDays, transportRowY, cellCenterPaint)
            canvas.drawText("₹${order.transportRent.toInt()}", colAmount, transportRowY, cellAmountPaint)

            y += 22f
            canvas.drawLine(MARGIN_LEFT, y, rightX, y, linePaint)
        }

        y += 8f

        // ══════════════════════════════════════════════════════
        // 4. SIDE-BY-SIDE SUMMARY & FOOTER
        // ══════════════════════════════════════════════════════

        val splitX = centerX + 20f

        // ── Right Side: Totals Box ────────
        val totalsBoxTop = y
        val totalsBoxHeight = if (order.discountAmount > 0) 90f else 74f
        val totalsRect = RectF(splitX, totalsBoxTop, rightX, totalsBoxTop + totalsBoxHeight)
        canvas.drawRoundRect(totalsRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(totalsRect, 6f, 6f, cardBorderPaint)

        var totalsRowY = totalsBoxTop + 16f
        val totalsLabelX = splitX + 10f
        val totalsValueX = rightX - 10f

        if (order.discountAmount > 0) {
            canvas.drawText(LABEL_DISCOUNT, totalsLabelX, totalsRowY, totalLabelPaint)
            canvas.drawText("-₹${String.format("%,d", order.discountAmount.toInt())}", totalsValueX, totalsRowY, totalValuePaint)
            totalsRowY += 16f
        }

        canvas.drawText(LABEL_TOTAL, totalsLabelX, totalsRowY, totalLabelPaint)
        canvas.drawText("₹${String.format("%,d", order.grandTotal.toInt())}", totalsValueX, totalsRowY, totalValuePaint)
        totalsRowY += 17f

        canvas.drawText(LABEL_PAID, totalsLabelX, totalsRowY, totalLabelPaint)
        canvas.drawText("₹${String.format("%,d", totalPaid.toInt())}", totalsValueX, totalsRowY, paidValuePaint)
        totalsRowY += 19f

        // Balance row with subtle highlight
        canvas.drawLine(splitX + 6f, totalsRowY - 13f, rightX - 6f, totalsRowY - 13f, linePaint)
        canvas.drawText(LABEL_BALANCE, totalsLabelX, totalsRowY, balanceLabelPaint)
        canvas.drawText("₹${String.format("%,d", balance.toInt())}", totalsValueX, totalsRowY, balanceValuePaint)

        // ── Left Side: Note Box ──────────
        val noteRect = RectF(MARGIN_LEFT, totalsBoxTop, splitX - 12f, totalsBoxTop + 38f)
        canvas.drawRoundRect(noteRect, 5f, 5f, noteBgPaint)
        canvas.drawRoundRect(noteRect, 5f, 5f, noteBorderPaint)

        canvas.drawText(LABEL_NOTE, MARGIN_LEFT + 8f, totalsBoxTop + 14f, noteTitlePaint)
        val noteLineY = totalsBoxTop + 26f
        val noteLines = wrapText(NOTE_TEXT, noteTextPaint, (splitX - 12f) - (MARGIN_LEFT + 8f))
        noteLines.take(2).forEachIndexed { idx, line ->
            canvas.drawText(line, MARGIN_LEFT + 8f, noteLineY + (idx * 11f), noteTextPaint)
        }

        // ── Signatures Area ───────────────
        val signY = totalsBoxTop + totalsBoxHeight + 28f

        // Customer Signature (Left)
        val custSignStartX = MARGIN_LEFT + 15f
        val custSignEndX = MARGIN_LEFT + 140f
        canvas.drawLine(custSignStartX, signY, custSignEndX, signY, linePaint)
        val custSignCenterX = (custSignStartX + custSignEndX) / 2f
        canvas.drawText(LABEL_CUSTOMER_SIGN, custSignCenterX, signY + 11f, signLabelPaint)

        // Proprietor Signature (Right)
        val propSignStartX = rightX - 150f
        val propSignEndX = rightX - 15f
        canvas.drawLine(propSignStartX, signY, propSignEndX, signY, linePaint)
        val propSignCenterX = (propSignStartX + propSignEndX) / 2f
        canvas.drawText(BUSINESS_NAME.takeWhile { it != ',' } + " करिता", propSignCenterX, signY + 11f, signPropLabelPaint)
    }

    // ── Drawing Helpers ─────────────────────────────────────

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
