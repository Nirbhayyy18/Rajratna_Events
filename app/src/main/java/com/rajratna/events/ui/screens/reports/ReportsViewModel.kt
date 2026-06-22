package com.rajratna.events.ui.screens.reports

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.events.RajratnaApp
import com.rajratna.events.data.dao.ItemIncome
import com.rajratna.events.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ReportData(
    val totalIncome: Double = 0.0,
    val receivedAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val totalOrders: Int = 0,
    val transportRent: Double = 0.0,
    val itemWiseIncome: List<ItemIncome> = emptyList()
)

data class ReportsState(
    val isLoading: Boolean = true,
    val selectedPeriod: String = "Today",
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val report: ReportData = ReportData(),
    val dateRangeLabel: String = "Today",
    // Month navigation
    val selectedMonthYear: Calendar = Calendar.getInstance(),
    // Range validation
    val rangeError: String? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as RajratnaApp).repository
    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val periodOptions = listOf("Today", "Week", "Month", "Range")

    init { selectPeriod("Today") }

    fun selectPeriod(period: String) {
        if (period == "Range") {
            // Switch UI to Range mode without loading a report yet;
            // report loads once the user picks both From and To dates.
            _state.value = _state.value.copy(
                selectedPeriod = "Range",
                rangeError = null
            )
            // If both dates are already selected, reload the report
            val start = _state.value.customStart
            val end = _state.value.customEnd
            if (start != null && end != null) {
                loadReport(start, end)
            }
            return
        }

        val (start, end, label) = when (period) {
            "Today" -> Triple(
                DateUtils.startOfToday(),
                DateUtils.endOfToday(),
                "Report for ${DateUtils.formatDate(DateUtils.startOfToday())}"
            )
            "Week" -> Triple(
                DateUtils.startOfThisWeek(),
                DateUtils.endOfThisWeek(),
                "${DateUtils.formatDate(DateUtils.startOfThisWeek())} - ${DateUtils.formatDate(DateUtils.endOfThisWeek() - 1)}"
            )
            "Month" -> {
                val cal = _state.value.selectedMonthYear
                val monthStart = getMonthStart(cal)
                val monthEnd = getMonthEnd(cal)
                Triple(monthStart, monthEnd, monthYearFormat.format(cal.time))
            }
            else -> return
        }
        _state.value = _state.value.copy(
            selectedPeriod = period,
            customStart = start,
            customEnd = end,
            dateRangeLabel = label,
            rangeError = null
        )
        loadReport(start, end)
    }

    fun navigateMonth(delta: Int) {
        val cal = _state.value.selectedMonthYear.clone() as Calendar
        cal.add(Calendar.MONTH, delta)
        _state.value = _state.value.copy(selectedMonthYear = cal)
        selectPeriod("Month")
    }

    fun selectCurrentMonth() {
        _state.value = _state.value.copy(selectedMonthYear = Calendar.getInstance())
        selectPeriod("Month")
    }

    fun selectPreviousMonth() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        _state.value = _state.value.copy(selectedMonthYear = cal)
        selectPeriod("Month")
    }

    fun getMonthLabel(): String = monthYearFormat.format(_state.value.selectedMonthYear.time)

    fun selectCustomRange(start: Long, end: Long) {
        if (end < start) {
            _state.value = _state.value.copy(rangeError = "To Date cannot be earlier than From Date")
            return
        }
        val label = "${DateUtils.formatDate(start)} - ${DateUtils.formatDate(end)}"
        _state.value = _state.value.copy(
            selectedPeriod = "Range",
            customStart = start,
            customEnd = end + 1, // include end of day
            dateRangeLabel = label,
            rangeError = null
        )
        loadReport(start, end + 1)
    }

    fun selectRangeStart(start: Long) {
        _state.value = _state.value.copy(
            selectedPeriod = "Range",
            customStart = start,
            rangeError = null
        )
    }

    fun selectRangeEnd(end: Long) {
        val start = _state.value.customStart ?: return
        selectCustomRange(start, end)
    }

    fun applyQuickRange(label: String) {
        val now = System.currentTimeMillis()
        val (start, end) = when (label) {
            "Last 7 Days" -> {
                val s = DateUtils.startOfDay(now - 6 * 24 * 60 * 60 * 1000L)
                val e = DateUtils.endOfDay(now)
                s to e
            }
            "Last 30 Days" -> {
                val s = DateUtils.startOfDay(now - 29 * 24 * 60 * 60 * 1000L)
                val e = DateUtils.endOfDay(now)
                s to e
            }
            "This Month" -> DateUtils.startOfThisMonth() to DateUtils.endOfThisMonth()
            else -> return
        }
        selectCustomRange(start, end)
    }

    private fun loadReport(start: Long, end: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val totalIncome = repository.getTotalIncomeByDelivery(start, end)
            val received = repository.getTotalPaymentReceived(start, end)
            val pending = repository.getPendingBalanceByDelivery(start, end)
            val orderCount = repository.getOrderCountByDelivery(start, end)
            val itemWise = repository.getItemWiseIncome(start, end)
            val transportRent = repository.getTotalTransportRent(start, end)

            _state.value = _state.value.copy(
                isLoading = false,
                report = ReportData(
                    totalIncome = totalIncome,
                    receivedAmount = received,
                    pendingAmount = pending,
                    totalOrders = orderCount,
                    transportRent = transportRent,
                    itemWiseIncome = itemWise
                )
            )
        }
    }

    fun loadReports() { selectPeriod(_state.value.selectedPeriod) }

    private fun getMonthStart(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getMonthEnd(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis + 1
    }

    fun generateWhatsAppReport(): String {
        val s = _state.value
        val r = s.report
        val sb = StringBuilder()
        sb.appendLine("*Rajratna Events*")
        sb.appendLine("Report: ${s.dateRangeLabel}")
        sb.appendLine()
        sb.appendLine("Total Income: ₹${r.totalIncome.toInt()}")
        sb.appendLine("Received: ₹${r.receivedAmount.toInt()}")
        sb.appendLine("Pending: ₹${r.pendingAmount.toInt()}")
        sb.appendLine("Total Orders: ${r.totalOrders}")
        sb.appendLine()
        sb.appendLine("Item Income:")
        r.itemWiseIncome.forEach { item ->
            sb.appendLine("${item.itemName}: ₹${item.totalIncome.toInt()}")
        }
        if (r.transportRent > 0) {
            sb.appendLine("Transport Rent: ₹${r.transportRent.toInt()}")
        }
        return sb.toString()
    }

    fun generateAndSharePdf(context: Context) {
        viewModelScope.launch {
            val s = _state.value
            val r = s.report

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = android.graphics.Color.parseColor("#E65100") }
            val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
            val bodyPaint = Paint().apply { textSize = 14f }
            val subtlePaint = Paint().apply { textSize = 12f; color = android.graphics.Color.GRAY }
            val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }

            var y = 50f
            val left = 40f

            // Title
            canvas.drawText("Rajratna Events", left, y, titlePaint)
            y += 30f
            canvas.drawText("Income Report", left, y, headerPaint)
            y += 24f
            canvas.drawText("Period: ${s.dateRangeLabel}", left, y, bodyPaint)
            y += 30f
            canvas.drawLine(left, y, 555f, y, linePaint)
            y += 24f

            // Summary
            canvas.drawText("Summary", left, y, headerPaint)
            y += 24f
            canvas.drawText("Total Income:", left, y, bodyPaint)
            canvas.drawText("₹${String.format("%,.0f", r.totalIncome)}", 300f, y, bodyPaint)
            y += 20f
            canvas.drawText("Received Amount:", left, y, bodyPaint)
            canvas.drawText("₹${String.format("%,.0f", r.receivedAmount)}", 300f, y, bodyPaint)
            y += 20f
            canvas.drawText("Pending Amount:", left, y, bodyPaint)
            canvas.drawText("₹${String.format("%,.0f", r.pendingAmount)}", 300f, y, bodyPaint)
            y += 20f
            canvas.drawText("Total Orders:", left, y, bodyPaint)
            canvas.drawText("${r.totalOrders}", 300f, y, bodyPaint)
            y += 30f
            canvas.drawLine(left, y, 555f, y, linePaint)
            y += 24f

            // Item-wise
            canvas.drawText("Item-wise Income", left, y, headerPaint)
            y += 24f
            r.itemWiseIncome.forEach { item ->
                canvas.drawText(item.itemName, left, y, bodyPaint)
                canvas.drawText("₹${String.format("%,.0f", item.totalIncome)}", 300f, y, bodyPaint)
                y += 20f
            }
            if (r.transportRent > 0) {
                canvas.drawText("Transport Rent", left, y, bodyPaint)
                canvas.drawText("₹${String.format("%,.0f", r.transportRent)}", 300f, y, bodyPaint)
                y += 20f
            }
            y += 20f
            canvas.drawLine(left, y, 555f, y, linePaint)
            y += 30f

            // Timestamp
            canvas.drawText("Generated: ${DateUtils.formatDateTime(System.currentTimeMillis())}", left, y, subtlePaint)

            document.finishPage(page)

            // Save
            val file = File(context.cacheDir, "Rajratna_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            // Share
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Report PDF"))
        }
    }
}
