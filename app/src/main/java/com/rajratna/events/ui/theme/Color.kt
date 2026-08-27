package com.rajratna.events.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
// Primary Brand Colors — Professional Teal
// ═══════════════════════════════════════════════════════════

val Teal80  = Color(0xFF4DB6AC)   // Light (dark-mode primary)
val Teal60  = Color(0xFF26A69A)   // Mid tone
val Teal40  = Color(0xFF00695C)   // Dark (light-mode primary)

val TealContainer     = Color(0xFFE0F2F1)   // Light-mode primary container
val OnTealContainer   = Color(0xFF003D33)   // On light-mode primary container
val TealContainerDark = Color(0xFF00504A)   // Dark-mode primary container
val OnTealContainerDark = Color(0xFFB2DFDB) // On dark-mode primary container

// ═══════════════════════════════════════════════════════════
// Secondary Colors — Warm Amber/Orange
// ═══════════════════════════════════════════════════════════

val Orange80        = Color(0xFFFFB74D)
val Orange40        = Color(0xFFE65100)
val OrangeContainer = Color(0xFFFFF3E0)
val OrangeContainerDark = Color(0xFF4A2200)

// ═══════════════════════════════════════════════════════════
// Tertiary Colors — Slate Gray
// ═══════════════════════════════════════════════════════════

val Slate80 = Color(0xFFB0BEC5)
val Slate40 = Color(0xFF455A64)

// ═══════════════════════════════════════════════════════════
// Intentionally Designed Surface Palette — Light
// ═══════════════════════════════════════════════════════════
// Not plain white/grey — uses a warm, off-white tinted base

val LightBackground     = Color(0xFFF5F6FA)  // App background
val LightSurface        = Color(0xFFFFFFFF)  // Cards, sheets, dialogs
val LightSurfaceVariant = Color(0xFFEEF0F8)  // Subtle tinted container surface
val LightSurfaceElev    = Color(0xFFFFFFFF)  // Elevated cards (same, elevated by shadow)
val LightOutline        = Color(0xFF9196A5)
val LightOutlineVariant = Color(0xFFE2E4EC)  // Dividers
val LightOnBackground   = Color(0xFF111318)  // Primary text
val LightOnSurface      = Color(0xFF111318)
val LightOnSurfaceVar   = Color(0xFF5C6070)  // Secondary text

// ═══════════════════════════════════════════════════════════
// Intentionally Designed Surface Palette — Dark
// ═══════════════════════════════════════════════════════════
// OLED-friendly dark blue-grey — NOT pure black.
// Uses layered surfaces to communicate elevation.

val DarkBackground      = Color(0xFF111318)  // Base layer (OLED-friendly deep blue-grey)
val DarkSurface         = Color(0xFF1C1F26)  // Cards, sheets — slightly lighter
val DarkSurfaceVariant  = Color(0xFF252932)  // Elevated cards — clearly distinct
val DarkSurfaceElev     = Color(0xFF2E3340)  // Modals, dialogs — most elevated
val DarkOutline         = Color(0xFF484E62)  // Subtle borders
val DarkOutlineVariant  = Color(0xFF2A2E38)  // Dividers — very subtle
val DarkOnBackground    = Color(0xFFE8EAF0)  // Primary text (off-white, not harsh)
val DarkOnSurface       = Color(0xFFE8EAF0)
val DarkOnSurfaceVar    = Color(0xFF8E93A6)  // Secondary text (muted blue-grey)

// ═══════════════════════════════════════════════════════════
// Status Colors — Order Status Chips (Light)
// ═══════════════════════════════════════════════════════════

val StatusPending       = Color(0xFFFFA726)  // Amber
val StatusPendingBg     = Color(0xFFFFF8E1)
val StatusConfirmed     = Color(0xFF1E88E5)  // Blue
val StatusConfirmedBg   = Color(0xFFE3F2FD)
val StatusDelivered     = Color(0xFF26A69A)  // Teal
val StatusDeliveredBg   = Color(0xFFE0F2F1)
val StatusCompleted     = Color(0xFF43A047)  // Green
val StatusCompletedBg   = Color(0xFFE8F5E9)
val StatusCancelled     = Color(0xFFEF5350)  // Red
val StatusCancelledBg   = Color(0xFFFFEBEE)

// Alert icon backgrounds (Light) — used in Dashboard AlertRow
val AlertOverdueIconBg    = Color(0xFFFFEFEF)
val AlertOverdueIconColor = Color(0xFFD63939)
val AlertPaymentIconBg    = Color(0xFFFFF6EA)
val AlertPaymentIconColor = Color(0xFFDB7A00)
val AlertStockIconBg      = Color(0xFFEFF4FF)
val AlertStockIconColor   = Color(0xFF1E5CC8)
val AlertBookingIconBg    = Color(0xFFEFFAF3)
val AlertBookingIconColor = Color(0xFF2F9B50)

// ═══════════════════════════════════════════════════════════
// Status Colors — Order Status Chips (Dark)
// ═══════════════════════════════════════════════════════════
// Not just lighter — carefully adjusted for dark surface readability

val StatusPendingDark       = Color(0xFFFFCC80)
val StatusPendingBgDark     = Color(0xFF3A2500)
val StatusConfirmedDark     = Color(0xFF90CAF9)
val StatusConfirmedBgDark   = Color(0xFF0A2744)
val StatusDeliveredDark     = Color(0xFF80CBC4)
val StatusDeliveredBgDark   = Color(0xFF003831)
val StatusCompletedDark     = Color(0xFFA5D6A7)
val StatusCompletedBgDark   = Color(0xFF0A2E0B)
val StatusCancelledDark     = Color(0xFFEF9A9A)
val StatusCancelledBgDark   = Color(0xFF3B0A0A)

// Alert icon backgrounds (Dark) — appropriately muted for dark surfaces
val AlertOverdueIconBgDark    = Color(0xFF3B1212)
val AlertPaymentIconBgDark    = Color(0xFF3A2000)
val AlertStockIconBgDark      = Color(0xFF0E2040)
val AlertBookingIconBgDark    = Color(0xFF0A2B14)

// ═══════════════════════════════════════════════════════════
// Payment Status Colors
// ═══════════════════════════════════════════════════════════

val PaymentUnpaid       = Color(0xFFE53935)
val PaymentUnpaidBg     = Color(0xFFFFEBEE)
val PaymentPartial      = Color(0xFFFB8C00)
val PaymentPartialBg    = Color(0xFFFFF3E0)
val PaymentPaid         = Color(0xFF43A047)
val PaymentPaidBg       = Color(0xFFE8F5E9)

val PaymentUnpaidDark   = Color(0xFFEF9A9A)
val PaymentUnpaidBgDark = Color(0xFF3B0A0A)
val PaymentPartialDark  = Color(0xFFFFCC80)
val PaymentPartialBgDark = Color(0xFF3A2500)
val PaymentPaidDark     = Color(0xFFA5D6A7)
val PaymentPaidBgDark   = Color(0xFF0A2E0B)
