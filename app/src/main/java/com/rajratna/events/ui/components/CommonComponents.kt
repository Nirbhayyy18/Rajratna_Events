package com.rajratna.events.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajratna.events.data.entity.OrderStatus
import com.rajratna.events.data.entity.PaymentStatusType
import com.rajratna.events.ui.theme.*
import com.rajratna.events.util.toRupee

// ═══════════════════════════════════════════════════════════
// ICON POD — Tinted icon container
// ═══════════════════════════════════════════════════════════

@Composable
fun IconPod(
    icon: ImageVector,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(containerColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

// ═══════════════════════════════════════════════════════════
// STATUS BADGE — Pill-shaped status indicator
// ═══════════════════════════════════════════════════════════

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = borderColor?.let { BorderStroke(1.dp, it) }
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════
// STATUS CHIP — Backward-compatible wrapper using StatusBadge
// ═══════════════════════════════════════════════════════════

@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.3f
    val (bgColor, textColor) = getStatusColors(status, isDark)
    val borderColor = getStatusBorderColor(status)
    StatusBadge(
        text = status,
        containerColor = bgColor,
        contentColor = textColor,
        borderColor = borderColor,
        modifier = modifier
    )
}

fun getStatusColors(status: String, isDark: Boolean = false): Pair<Color, Color> {
    return when (status) {
        OrderStatus.PENDING       -> if (isDark) StatusPendingBgDark   to StatusPendingDark
                                     else         StatusPendingBg        to StatusPending
        OrderStatus.CONFIRMED     -> if (isDark) StatusConfirmedBgDark  to StatusConfirmedDark
                                     else         StatusConfirmedBg      to StatusConfirmed
        OrderStatus.DELIVERED     -> if (isDark) StatusDeliveredBgDark  to StatusDeliveredDark
                                     else         StatusDeliveredBg      to StatusDelivered
        OrderStatus.COMPLETED     -> if (isDark) StatusCompletedBgDark  to StatusCompletedDark
                                     else         StatusCompletedBg      to StatusCompleted
        OrderStatus.CANCELLED     -> if (isDark) StatusCancelledBgDark  to StatusCancelledDark
                                     else         StatusCancelledBg      to StatusCancelled
        PaymentStatusType.UNPAID         -> if (isDark) PaymentUnpaidBgDark    to PaymentUnpaidDark
                                             else         PaymentUnpaidBg        to PaymentUnpaid
        PaymentStatusType.PARTIALLY_PAID -> if (isDark) PaymentPartialBgDark   to PaymentPartialDark
                                             else         PaymentPartialBg       to PaymentPartial
        PaymentStatusType.PAID           -> if (isDark) PaymentPaidBgDark      to PaymentPaidDark
                                             else         PaymentPaidBg          to PaymentPaid
        else -> Color.LightGray to Color.DarkGray
    }
}

fun getStatusBorderColor(status: String): Color? {
    return when (status) {
        OrderStatus.PENDING, PaymentStatusType.PARTIALLY_PAID  -> StatusPendingBorder
        OrderStatus.CONFIRMED                                   -> StatusConfirmedBorder
        OrderStatus.COMPLETED, PaymentStatusType.PAID          -> StatusCompletedBorder
        OrderStatus.CANCELLED, PaymentStatusType.UNPAID        -> StatusCancelledBorder
        else -> null
    }
}

// ═══════════════════════════════════════════════════════════
// CUSTOMER INITIALS AVATAR
// ═══════════════════════════════════════════════════════════

@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════
// PREMIUM CARD CONTAINER
// ═══════════════════════════════════════════════════════════

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 2.dp),
            content = { Column(Modifier.padding(16.dp), content = content) }
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = { Column(Modifier.padding(16.dp), content = content) }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// STAT CARD (Dashboard) — upgraded with IconPod
// ═══════════════════════════════════════════════════════════

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier) {
        IconPod(
            icon = icon,
            iconColor = iconTint,
            containerColor = iconTint.copy(alpha = 0.12f),
            size = 40.dp,
            iconSize = 20.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════
// AMOUNT ROW (for detail screens)
// ═══════════════════════════════════════════════════════════

@Composable
fun AmountRow(
    label: String,
    amount: Double,
    isBold: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(
            text = amount.toRupee(),
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

// ═══════════════════════════════════════════════════════════
// QUICK ACTION BUTTON
// ═══════════════════════════════════════════════════════════

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        action?.invoke()
    }
}

// ═══════════════════════════════════════════════════════════
// EMPTY STATE — improved with icon pod
// ═══════════════════════════════════════════════════════════

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconPod(
            icon = icon,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconColor = MaterialTheme.colorScheme.outline,
            size = 72.dp,
            iconSize = 36.dp,
            shape = RoundedCornerShape(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
