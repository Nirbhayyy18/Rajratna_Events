package com.rajratna.events.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajratna.events.data.entity.Order
import com.rajratna.events.data.entity.OrderItem
import com.rajratna.events.util.MarathiBillGenerator
import com.rajratna.events.util.WhatsAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet with bill sharing options:
 * 1. Preview Marathi Bill
 * 2. Share Bill Image (PNG via WhatsApp)
 * 3. Share Bill PDF (via share sheet)
 * 4. Share Text on WhatsApp (existing text-based)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillOptionsSheet(
    order: Order,
    orderItems: List<OrderItem>,
    totalPaid: Double,
    onDismiss: () -> Unit,
    onPreviewBill: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = "बिल पर्याय | Bill Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Bill #${order.billNumber} • ${order.customerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Loading indicator
            if (isGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // 1. Preview Marathi Bill
            BillOptionItem(
                icon = Icons.Default.Visibility,
                title = "Preview Marathi Bill",
                subtitle = "मराठी बिल पहा",
                enabled = !isGenerating,
                onClick = {
                    onPreviewBill()
                    onDismiss()
                }
            )

            // 2. Share Bill Image
            BillOptionItem(
                icon = Icons.Default.Image,
                title = "Share Bill Image",
                subtitle = "बिल फोटो शेअर करा (PNG)",
                enabled = !isGenerating,
                onClick = {
                    isGenerating = true
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.Default) {
                                MarathiBillGenerator.generateImage(context, order, orderItems, totalPaid)
                            }
                            val message = MarathiBillGenerator.getWhatsAppSummary(order, totalPaid)
                            WhatsAppUtils.shareFileOnWhatsApp(
                                context, order.customerMobile, file,
                                "image/png", message
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error generating image: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        isGenerating = false
                    }
                }
            )

            // 3. Share Bill PDF
            BillOptionItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Share Bill PDF",
                subtitle = "बिल PDF शेअर करा",
                enabled = !isGenerating,
                onClick = {
                    isGenerating = true
                    scope.launch {
                        try {
                            val file = withContext(Dispatchers.Default) {
                                MarathiBillGenerator.generatePdf(context, order, orderItems, totalPaid)
                            }
                            val message = MarathiBillGenerator.getWhatsAppSummary(order, totalPaid)
                            WhatsAppUtils.shareFile(context, file, "application/pdf", message)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        isGenerating = false
                    }
                }
            )

            // 4. Share Text on WhatsApp
            BillOptionItem(
                icon = Icons.Default.Chat,
                title = "Share Text on WhatsApp",
                subtitle = "टेक्स्ट WhatsApp वर शेअर करा",
                enabled = !isGenerating,
                onClick = {
                    WhatsAppUtils.shareOnWhatsApp(
                        context, order.customerMobile,
                        WhatsAppUtils.generateBillMessage(order, orderItems)
                    )
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun BillOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
