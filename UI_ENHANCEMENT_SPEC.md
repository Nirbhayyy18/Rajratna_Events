# Rajratna Events — UI Enhancement & Redesign Specification

This document provides a comprehensive, production-ready visual design specification for rebuilding and elevating the **Rajratna Events** Android application UI. It is designed to be directly actionable for AI coding agents and developers using **Jetpack Compose** and **Material Design 3 (M3)**.

---

## 1. Design Vision & Core Principles

* **Executive Elegance**: Clean, professional, high-contrast aesthetic tailored for event rental and inventory management.
* **Effortless Scannability**: Important figures (revenue, pending balance, event dates, stock levels) stand out instantly through strong typography and color contrast.
* **Surface Layering over Heavy Shadows**: Use subtle 1.dp borders with 50–60% opacity outline variants on tonal surface containers (`surfaceContainerLow`, `surfaceContainerHighest`) instead of muddy drop shadows.
* **Tinted Icon Pods**: Place all icons in softly tinted rounded containers to provide visual anchors and improve visual rhythm.
* **Zero Layout Clutter**: Maintain generous 16dp–20dp padding, an 8dp baseline grid, and clear visual hierarchy.

---

## 2. Color System & Theme Palette

### Light Theme
* **Primary (Deep Jewel Emerald)**: `#0E3B36` (OnPrimary: `#FFFFFF`)
* **Primary Container**: `#E6F4F1` (OnPrimaryContainer: `#052B27`)
* **Secondary (Slate Indigo)**: `#2C3E50` (OnSecondary: `#FFFFFF`)
* **Secondary Container**: `#E8ECF2` (OnSecondaryContainer: `#1A2530`)
* **Tertiary / Accent (Warm Champagne Gold)**: `#B8860B` / `#D4AF37`
* **Tertiary Container**: `#FFF8E7` (OnTertiaryContainer: `#5A4200`)
* **Background**: `#F8FAF9`
* **Surface**: `#FFFFFF`
* **Surface Container Lowest**: `#FFFFFF`
* **Surface Container Low**: `#F3F6F5`
* **Surface Container**: `#EDF2F0`
* **Surface Container High**: `#E7ECE9`
* **Surface Container Highest**: `#E0E6E3`
* **Outline**: `#D0D9D5`
* **Outline Variant**: `#E4EAE7`

### Semantic & Status Colors (Soft Tint Badges)
* **Success / Paid / Returned**:
  * Text/Icon: `#166534` (Dark Green)
  * Container Background: `#DCFCE7` (12% green tint)
  * Border: `#BBF7D0`
* **Warning / Pending / Partial**:
  * Text/Icon: `#B45309` (Amber Brown)
  * Container Background: `#FEF3C7` (12% amber tint)
  * Border: `#FDE68A`
* **Critical / Overdue / Unpaid**:
  * Text/Icon: `#991B1B` (Crimson)
  * Container Background: `#FEE2E2` (12% red tint)
  * Border: `#FECACA`
* **Info / In-Progress / Dispatched**:
  * Text/Icon: `#1E40AF` (Navy Blue)
  * Container Background: `#DBEAFE` (12% blue tint)
  * Border: `#BFDBFE`

### Dark Theme
* **Primary**: `#72D7C7` (OnPrimary: `#003731`)
* **Primary Container**: `#004F47` (OnPrimaryContainer: `#8EF4E3`)
* **Background**: `#0F1413`
* **Surface**: `#131A18`
* **Surface Container Low**: `#17201E`
* **Surface Container**: `#1C2624`
* **Surface Container High**: `#222E2B`
* **Outline Variant**: `#2D3D39`

---

## 3. Typography Hierarchy

* **Hero Metric / Total Revenue**: `headlineLarge` or `displaySmall` — `FontWeight.Bold`, letter spacing `-0.5.sp`.
* **Section Titles**: `titleMedium` / `titleLarge` — `FontWeight.SemiBold`, 18–20sp.
* **Card Primary Labels**: `titleMedium` — `FontWeight.Medium`, 16sp.
* **Secondary / Body Text**: `bodyMedium` — `FontWeight.Normal`, 14sp, `color = MaterialTheme.colorScheme.onSurfaceVariant`.
* **Micro Labels & Badges**: `labelSmall` / `labelMedium` — `FontWeight.SemiBold`, 11–12sp, uppercase tracking `0.5.sp`.
* **Currency Values (₹)**: Always use medium/bold weights to maintain consistent prominence.

---

## 4. Reusable UI Component Specifications

### 4.1. Tinted Icon Pod
Every icon inside cards, list items, and action tiles should use a consistent icon container:
```kotlin
@Composable
fun IconPod(
    icon: ImageVector,
    contentDescription: String?,
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
```

### 4.2. Status Badge / Pill
Consistent rounded badges for order status, payment status, and return state:
```kotlin
@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null
) {
    Surface(
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
```

### 4.3. Premium Card Container
Base card styling used throughout the app:
* **Shape**: `RoundedCornerShape(16.dp)` to `RoundedCornerShape(20.dp)`.
* **Colors**: `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)` or `surfaceContainerLow`.
* **Border**: `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))`.
* **Content Padding**: `16.dp` minimum (never less than 14dp).

---

## 5. Screen-by-Screen UI Transformation Guide

### 5.1. Dashboard Screen
1. **Top Header**:
   * Brand avatar or stylized monogram icon with business name ("Rajratna Events").
   * Date chip indicator ("Today, 24 Oct") + quick theme toggle button.
2. **Hero Financial Card**:
   * Subtle gradient background (`Brush.linearGradient(listOf(primary, primaryContainer.copy(alpha = 0.8f)))`).
   * Primary metric: **Total Monthly / Active Revenue** in large bold text.
   * Secondary metrics row: **Pending Balance** (amber chip) and **Today's Deliveries** (blue chip).
3. **Quick Actions Matrix (2x2 or 4x1 Horizontal Row)**:
   * "New Order", "Add Payment", "Record Return", "Quick Water Jar".
   * Each action with distinct icon pod background colors and clean title labels.
4. **Recent Orders & Today's Schedule**:
   * Clean card list with customer name, event date, item count badge, and financial status pill.
   * Prominent "View All" link with arrow indicator.

### 5.2. Orders & Order Details Screen
1. **Filter Tabs**:
   * Pill-shaped scrollable chips (`All`, `Active Today`, `Upcoming`, `Completed`, `Pending Payment`).
2. **Order Cards**:
   * Left: Calendar/Date Pod with day number and month.
   * Center: Customer Name (bold), Event Type / Venue, Item count.
   * Right: Total Amount (₹), Status Badge (Paid / Partial / Due).
   * Bottom bar inside card: Quick "Call" and "WhatsApp" icon buttons for immediate contact.
3. **Order Details Screen**:
   * Clean receipt-style card with dashed/perforated separator.
   * Itemized breakdown with quantity, unit rate, and total.
   * Payment timeline showing advance paid, payment method, and outstanding dues.

### 5.3. Equipment & Items Screen
1. **Category Filter Rail/Row**:
   * Visual icons for *Mandap*, *Sound & Lights*, *Utensils*, *Water Jars*, *Furniture*, *Generators*.
2. **Item Cards**:
   * Clean item name and category tag.
   * Stock status indicator:
     * Green dot + "In Stock (45 available)"
     * Amber dot + "Low Stock (3 available)"
     * Red dot + "All Booked"
   * Daily / Event rental rate clearly displayed.

### 5.4. Customer Management Screen
1. **Search Bar**:
   * Outlined search bar with rounded 16.dp corners, clear search button, and leading search icon.
2. **Customer Cards**:
   * Circular name initials avatar with soft primary tint.
   * Customer Name + Contact Number.
   * Sub-info: Total Orders placed, Total Outstanding Due (highlighted in red if > 0).
   * Action icons: Direct Call & WhatsApp buttons.

### 5.5. Returns & Equipment Tracking Screen
1. **Return Checklist**:
   * Grouped by Order / Event.
   * Item rows with visual checkbox and quantity returned vs dispatched counter.
   * Damage / Missing item counter with instant remark field.
2. **Quick Water Jar Tracker**:
   * Delivery count vs Empty jar return counter with large `+` / `−` steppers.

### 5.6. Bill Preview & Receipt Screen
1. **Receipt Layout**:
   * Clean header with Rajratna Events logo/typography, GST/Contact details, and Invoice number.
   * High-contrast table for items, rental duration, rates, and amounts.
   * Total calculation breakdown (Subtotal, Discount, Tax, Advance, Net Balance Due).
2. **Action Bar**:
   * "Share on WhatsApp" (Green filled button).
   * "Download PDF" (Outlined button).
   * "Print Bill" (Icon button).

---

## 6. Spacing & Layout Rules

* **Screen Edge Margin**: `16.dp` horizontal margin on phones, `24.dp` on tablets.
* **Card Inner Padding**: `16.dp` default.
* **Item Spacing in Lists**: `12.dp` between cards.
* **Section Spacing**: `20.dp` to `24.dp` between distinct sections.
* **Touch Target Size**: All interactive buttons, icon buttons, and chips must have a minimum touch target of `48.dp x 48.dp`.

---

## 7. Animations & Micro-Interactions

* **List Item Transitions**: Use `Modifier.animateItemPlacement()` in `LazyColumn`.
* **State Expansion**: Use `AnimatedVisibility` with `fadeIn() + expandVertically()` for expandable details.
* **Tab Selection**: Smooth slide indicators on category switchers.
* **Button Haptics & Ripples**: Standard M3 ripple on all clickable surfaces.

---

## 8. Step-by-Step Implementation Checklist

1. [ ] **Update Color & Theme Files**:
   * Align `Color.kt` and `Theme.kt` with the Jewel Emerald palette and semantic status colors.
2. [ ] **Implement Core Components**:
   * Create `IconPod`, `StatusBadge`, and unified `PremiumCard` in `ui/components/CommonComponents.kt`.
3. [ ] **Upgrade Dashboard Screen**:
   * Refactor revenue hero card, quick actions matrix, and recent order items.
4. [ ] **Refine Orders & Details**:
   * Apply receipt styling, filter chips, and one-tap call/message actions.
5. [ ] **Polish Inventory & Customer Lists**:
   * Standardize avatars, stock indicators, and customer financial summary chips.
6. [ ] **Verify Layout on Light & Dark Modes**:
   * Ensure high readability, proper contrast ratios, and consistent padding across all screens.
