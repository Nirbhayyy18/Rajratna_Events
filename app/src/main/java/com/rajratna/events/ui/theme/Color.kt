package com.rajratna.events.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════
// Primary Brand Colors — Jewel Emerald
// ═══════════════════════════════════════════════════════════

val Emerald10  = Color(0xFF052B27)  // Deepest (on-primary-container)
val Emerald20  = Color(0xFF0A3D37)
val Emerald40  = Color(0xFF0E5247)  // Light-mode primary
val Emerald60  = Color(0xFF1A7A6A)
val Emerald80  = Color(0xFF5ABFAE)  // Dark-mode primary
val Emerald90  = Color(0xFF8ED8CC)

val EmeraldContainer     = Color(0xFFE0F4F1)  // Light-mode primary container
val OnEmeraldContainer   = Color(0xFF052B27)
val EmeraldContainerDark = Color(0xFF004F47)  // Dark-mode primary container
val OnEmeraldContainerDark = Color(0xFF8EF4E3)

// Legacy aliases kept for backward compatibility
val Teal80  = Emerald80
val Teal60  = Emerald60
val Teal40  = Emerald40

val TealContainer       = EmeraldContainer
val OnTealContainer     = OnEmeraldContainer
val TealContainerDark   = EmeraldContainerDark
val OnTealContainerDark = OnEmeraldContainerDark

// ═══════════════════════════════════════════════════════════
// Secondary Colors — Slate Indigo
// ═══════════════════════════════════════════════════════════

val SlateIndigo80        = Color(0xFF8FA8C8)
val SlateIndigo40        = Color(0xFF2C3E50)
val SlateIndigoContainer = Color(0xFFE8ECF2)
val SlateIndigoContainerDark = Color(0xFF1A2530)

// Legacy orange aliases kept for backward compatibility
val Orange80        = Color(0xFFFFB74D)
val Orange40        = Color(0xFFE65100)
val OrangeContainer = Color(0xFFFFF3E0)
val OrangeContainerDark = Color(0xFF4A2200)

// ═══════════════════════════════════════════════════════════
// Tertiary Colors — Champagne Gold
// ═══════════════════════════════════════════════════════════

val Gold80 = Color(0xFFD4AF37)   // Light-mode tertiary (Champagne Gold)
val Gold40 = Color(0xFFB8860B)   // Dark-mode tertiary
val GoldContainer     = Color(0xFFFFF8E7)
val OnGoldContainer   = Color(0xFF5A4200)
val GoldContainerDark = Color(0xFF3D2C00)

// Legacy slate aliases
val Slate80 = Color(0xFFB0BEC5)
val Slate40 = Color(0xFF455A64)

// ═══════════════════════════════════════════════════════════
// Surface Palette — Light
// ═══════════════════════════════════════════════════════════

val LightBackground     = Color(0xFFF8FAF9)  // Slightly green-tinted off-white
val LightSurface        = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDF2F0)
val LightSurfaceElev    = Color(0xFFFFFFFF)
val LightOutline        = Color(0xFF8FA09B)
val LightOutlineVariant = Color(0xFFD0D9D5)
val LightOnBackground   = Color(0xFF0D1F1C)
val LightOnSurface      = Color(0xFF0D1F1C)
val LightOnSurfaceVar   = Color(0xFF4F6360)

// ═══════════════════════════════════════════════════════════
// Surface Palette — Dark
// ═══════════════════════════════════════════════════════════

val DarkBackground      = Color(0xFF0F1413)
val DarkSurface         = Color(0xFF131A18)
val DarkSurfaceVariant  = Color(0xFF1C2624)
val DarkSurfaceElev     = Color(0xFF222E2B)
val DarkOutline         = Color(0xFF3A4F4B)
val DarkOutlineVariant  = Color(0xFF2D3D39)
val DarkOnBackground    = Color(0xFFE4EEEB)
val DarkOnSurface       = Color(0xFFE4EEEB)
val DarkOnSurfaceVar    = Color(0xFF8FA09B)

// ═══════════════════════════════════════════════════════════
// Semantic Status Colors — Success / Paid / Returned (Green)
// ═══════════════════════════════════════════════════════════

val StatusCompleted     = Color(0xFF166534)   // Dark green text/icon
val StatusCompletedBg   = Color(0xFFDCFCE7)   // Light green badge bg
val StatusCompletedBorder = Color(0xFFBBF7D0)

val StatusCompletedDark   = Color(0xFFA5D6A7)
val StatusCompletedBgDark = Color(0xFF0A2E0B)

// Delivered (teal)
val StatusDelivered     = Color(0xFF0E5247)
val StatusDeliveredBg   = Color(0xFFE0F4F1)
val StatusDeliveredDark = Color(0xFF80CBC4)
val StatusDeliveredBgDark = Color(0xFF003831)

// ═══════════════════════════════════════════════════════════
// Semantic Status Colors — Warning / Pending (Amber)
// ═══════════════════════════════════════════════════════════

val StatusPending       = Color(0xFFB45309)
val StatusPendingBg     = Color(0xFFFEF3C7)
val StatusPendingBorder = Color(0xFFFDE68A)

val StatusPendingDark   = Color(0xFFFFCC80)
val StatusPendingBgDark = Color(0xFF3A2500)

// ═══════════════════════════════════════════════════════════
// Semantic Status Colors — Critical / Overdue / Unpaid (Red)
// ═══════════════════════════════════════════════════════════

val StatusCancelled     = Color(0xFF991B1B)
val StatusCancelledBg   = Color(0xFFFEE2E2)
val StatusCancelledBorder = Color(0xFFFECACA)

val StatusCancelledDark   = Color(0xFFEF9A9A)
val StatusCancelledBgDark = Color(0xFF3B0A0A)

// ═══════════════════════════════════════════════════════════
// Semantic Status Colors — Info / Confirmed (Blue)
// ═══════════════════════════════════════════════════════════

val StatusConfirmed     = Color(0xFF1E40AF)
val StatusConfirmedBg   = Color(0xFFDBEAFE)
val StatusConfirmedBorder = Color(0xFFBFDBFE)

val StatusConfirmedDark   = Color(0xFF90CAF9)
val StatusConfirmedBgDark = Color(0xFF0A2744)

// ═══════════════════════════════════════════════════════════
// Payment Status Colors
// ═══════════════════════════════════════════════════════════

val PaymentUnpaid       = Color(0xFF991B1B)
val PaymentUnpaidBg     = Color(0xFFFEE2E2)
val PaymentPartial      = Color(0xFFB45309)
val PaymentPartialBg    = Color(0xFFFEF3C7)
val PaymentPaid         = Color(0xFF166534)
val PaymentPaidBg       = Color(0xFFDCFCE7)

val PaymentUnpaidDark   = Color(0xFFEF9A9A)
val PaymentUnpaidBgDark = Color(0xFF3B0A0A)
val PaymentPartialDark  = Color(0xFFFFCC80)
val PaymentPartialBgDark = Color(0xFF3A2500)
val PaymentPaidDark     = Color(0xFFA5D6A7)
val PaymentPaidBgDark   = Color(0xFF0A2E0B)

// ═══════════════════════════════════════════════════════════
// Alert Icon Backgrounds — Dashboard (Light)
// ═══════════════════════════════════════════════════════════

val AlertOverdueIconBg    = Color(0xFFFEE2E2)
val AlertOverdueIconColor = Color(0xFF991B1B)
val AlertPaymentIconBg    = Color(0xFFFEF3C7)
val AlertPaymentIconColor = Color(0xFFB45309)
val AlertStockIconBg      = Color(0xFFDBEAFE)
val AlertStockIconColor   = Color(0xFF1E40AF)
val AlertBookingIconBg    = Color(0xFFDCFCE7)
val AlertBookingIconColor = Color(0xFF166534)

// Alert Icon Backgrounds — Dashboard (Dark)
val AlertOverdueIconBgDark    = Color(0xFF3B0A0A)
val AlertPaymentIconBgDark    = Color(0xFF3A2500)
val AlertStockIconBgDark      = Color(0xFF0E2040)
val AlertBookingIconBgDark    = Color(0xFF0A2B14)
