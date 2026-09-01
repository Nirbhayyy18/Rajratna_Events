package com.rajratna.events.ui.screens.billpreview

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajratna.events.util.MarathiBillGenerator
import com.rajratna.events.util.WhatsAppUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPreviewScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    viewModel: BillPreviewViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(orderId) { viewModel.loadBillPreview(orderId) }

    // Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 4f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Marathi Bill Preview",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Reset zoom
                    if (scale != 1f) {
                        IconButton(onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }) {
                            Icon(Icons.Default.FitScreen, "Reset zoom")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.order != null) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Share Image
                        OutlinedButton(
                            onClick = {
                                val order = state.order ?: return@OutlinedButton
                                viewModel.generateAndShareImage { file ->
                                    val message = MarathiBillGenerator.getWhatsAppSummary(order, state.totalPaid)
                                    WhatsAppUtils.shareFileOnWhatsApp(
                                        context, order.customerMobile, file,
                                        "image/png", message
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Image", fontWeight = FontWeight.SemiBold)
                        }

                        // Share PDF
                        Button(
                            onClick = {
                                val order = state.order ?: return@Button
                                viewModel.generateAndSharePdf { file ->
                                    val message = MarathiBillGenerator.getWhatsAppSummary(order, state.totalPaid)
                                    WhatsAppUtils.shareFile(context, file, "application/pdf", message)
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF", fontWeight = FontWeight.SemiBold)
                        }

                        // WhatsApp text
                        FilledTonalButton(
                            onClick = {
                                val order = state.order ?: return@FilledTonalButton
                                WhatsAppUtils.shareOnWhatsApp(
                                    context, order.customerMobile,
                                    WhatsAppUtils.generateBillMessage(order, state.orderItems)
                                )
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Chat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Text", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Generating bill...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Error: ${state.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                state.billBitmap != null -> {
                    val scrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .transformable(transformState),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                },
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Image(
                                bitmap = state.billBitmap!!.asImageBitmap(),
                                contentDescription = "Marathi Bill Preview",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
        }
    }
}
