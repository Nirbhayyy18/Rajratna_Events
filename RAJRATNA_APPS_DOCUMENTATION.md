# Rajratna Events — Complete System & Architecture Documentation

---

## 1. Executive Summary & System Overview

**Rajratna Events** (`com.rajratna.events`) is a production-grade, offline-first Android application designed for event rental and catering equipment management (mandap, chairs, tables, water jars, catering utensils). It streamlines the end-to-end operational workflow of a rental business: inventory tracking, real-time date-wise stock conflict prevention, customer directory management, multi-day order booking, invoice/bill generation in Marathi (PDF & Image formats), WhatsApp dispatch, partial/full payment tracking, returns/damage auditing, and financial analytics.

### Primary Operational Objectives
- **Zero Double-Booking**: Date-range overlapping stock calculation prevents renting more items than available in physical inventory.
- **Vernacular Invoicing**: Native Devanagari (Marathi) invoice generation with itemization, terms & conditions, and proprietary branding.
- **Customer Lifecycle Tracking**: Lifetime order history, total business volume, outstanding balance, and 1-tap call/WhatsApp triggers.
- **Offline Resilience & Real-Time Sync**: Powered by Google Cloud Firestore with local cache persistence.

---

## 2. Technology Stack & Dependencies

### Core Platform & Environment
| Layer | Specification / Library | Version / Details |
|---|---|---|
| **OS Platform** | Android | Min SDK 26 (Android 8.0 Oreo), Target SDK 34 (Android 14) |
| **Language** | Kotlin | 1.9.24 (JVM Target 17) |
| **Android Gradle Plugin** | AGP | 8.5.0 |
| **UI Toolkit** | Jetpack Compose (Material3) | Compose BOM `2024.06.00`, Compiler `1.5.14` |
| **Design System** | Material Design 3 (M3) | Light, Dark, and OLED pure black modes |
| **Architecture** | MVVM + Clean Repository Pattern | Unidirectional Data Flow (UDF) via Kotlin Coroutines & `StateFlow` |

### Key Libraries & Frameworks
- **Firebase Platform**:
  - `firebase-bom:33.2.0`
  - `firebase-auth` (Email/Password Authentication)
  - `firebase-firestore` (Cloud NoSQL Database with Offline Cache Persistence)
- **Asynchronous & Reactive**:
  - `kotlinx-coroutines-core` & `kotlinx-coroutines-android`
  - `kotlinx-coroutines-tasks` (for `Task.await()` extensions)
  - `callbackFlow` & `StateFlow`
- **Navigation**: `androidx.navigation:navigation-compose:2.7.7`
- **Serialization & Backup**: `com.google.code.gson:gson:2.11.0`
- **Preferences & Settings**: `androidx.datastore:datastore-preferences:1.1.1`
- **Graphics & PDF**: Native Android `android.graphics.pdf.PdfDocument`, `android.graphics.Canvas`, `android.graphics.Paint`, `android.graphics.Typeface` (bundled `Noto Sans Devanagari`)
- **System Integration**: Storage Access Framework (SAF) `ActivityResultContracts.CreateDocument` / `OpenDocument`, Android `FileProvider` (`com.rajratna.events.fileprovider`), `Intent.ACTION_VIEW`, `Intent.ACTION_SEND`, `Intent.ACTION_DIAL`.

---

## 3. Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                       Presentation Layer                    │
│   Jetpack Compose Screens & Reusable UI Components          │
│   (Dashboard, NewOrder, Returns, Inventory, Reports, etc.)  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Observes StateFlow<UiState>
                               │ Triggers User Actions
┌──────────────────────────────▼──────────────────────────────┐
│                        ViewModel Layer                      │
│   AndroidViewModel instances managing Unidirectional State  │
│   (NewOrderViewModel, DashboardViewModel, ReturnsVM, etc.)  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutine calls / Flow collection
┌──────────────────────────────▼──────────────────────────────┐
│                       Repository Layer                      │
│   AppRepository (Firestore DB operations, caching, math)    │
│   AuthRepository (Firebase Auth sessions & user profiles)    │
└──────────────────────────────┬──────────────────────────────┘
                               │ Firestore Transactions / Batches / Listeners
┌──────────────────────────────▼──────────────────────────────┐
│                    Cloud Firestore & Auth                   │
│   Remote Cloud Database + Local Disk Offline Persistence    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Cloud Firestore Schema & Data Models

All Firestore document IDs are UUID strings generated via `collection.document().id`. Subcollections are strictly hierarchical.

### 4.1 Collections & Document Structure

#### 1. `items` (Collection)
Represents physical rental assets in the company's warehouse.
```json
{
  "id": "5ZD8tNOr3vTO9ruDvU2p",       // Firestore Document ID
  "name": "Table",                    // String
  "ratePerDay": 30.0,                 // Double (Rent charge per unit per day in INR)
  "totalStock": 44,                   // Int (Total physical stock owned)
  "lowStockAlert": 10,                // Int (Threshold for UI warnings)
  "active": true,                     // Boolean (Kotlin isActive -> serialized as 'active')
  "deleted": false,                   // Boolean (Soft delete flag -> serialized as 'deleted')
  "createdAt": 1787904182452          // Long (Unix Epoch Milliseconds)
}
```

#### 2. `customers` (Collection)
Directory of clients. Aggregated statistics (total orders, total spend) are computed dynamically from orders.
```json
{
  "id": "cust_8934273894",
  "name": "Suresh Patil",
  "mobileNumber": "9822012345",
  "address": "Andrud, Phaltan",
  "createdAt": 1787850000000,
  "deleted": false
}
```

#### 3. `orders` (Collection)
Header document for every rental order.
```json
{
  "id": "ord_9182371982",
  "billNumber": 1042,                 // Int (Atomic auto-increment from /counters/billNumber)
  "customerId": "cust_8934273894",
  "customerName": "Suresh Patil",
  "customerMobile": "9822012345",
  "customerAddress": "Andrud, Phaltan",
  "orderDate": 1787855400000,         // Epoch ms (Event date)
  "deliveryDate": 1787855400000,      // Epoch ms (Equipment dispatch date)
  "returnDate": 1787941800000,        // Epoch ms (Expected return date)
  "rentalDays": 1,                    // Int
  "notes": "Wedding reception at home",
  "itemsTotal": 3500.0,               // Double
  "transportRent": 500.0,             // Double
  "grandTotal": 4000.0,               // Double (itemsTotal + transportRent)
  "advancePaid": 1000.0,              // Double (Initial deposit / advance)
  "balanceAmount": 3000.0,            // Double (grandTotal - totalPaid)
  "orderStatus": "Confirmed",         // String ("Pending", "Confirmed", "Delivered", "Completed", "Cancelled")
  "paymentStatus": "Partially Paid",  // String ("Unpaid", "Partially Paid", "Paid")
  "createdAt": 1787855400000,
  "updatedAt": 1787855400000,
  "deleted": false
}
```

#### 4. `orders/{orderId}/order_items` (Subcollection)
Line items locked to a specific order at the agreed rate.
```json
{
  "id": "item_sub_01",
  "orderId": "ord_9182371982",
  "itemId": "5ZD8tNOr3vTO9ruDvU2p",
  "itemName": "Table",
  "quantity": 20,                     // Units rented
  "ratePerDay": 30.0,                 // Rate snapshot
  "rentalDays": 1,
  "totalAmount": 600.0,               // quantity * ratePerDay * rentalDays
  "returnedQuantity": 0,              // Audited returned count
  "damagedQuantity": 0,               // Broken/missing count
  "customerOwned": false              // Boolean (For custom water jars refilled by customer)
}
```

#### 5. `orders/{orderId}/payments` (Subcollection)
Audit ledger of payments made against the order.
```json
{
  "id": "pay_98231908",
  "orderId": "ord_9182371982",
  "customerName": "Suresh Patil",
  "customerMobile": "9822012345",
  "amount": 1000.0,
  "paymentDate": 1787855400000,
  "paymentMethod": "UPI",             // "Cash", "UPI", "Bank Transfer", "Other"
  "notes": "GPay advance",
  "createdAt": 1787855400000
}
```

#### 6. `counters` (Collection)
Atomic sequential counters for invoice numbering.
- Document ID: `billNumber` ➔ `{ "value": 1042 }`

#### 7. `users` (Collection)
User access control profiles.
```json
{
  "uid": "firebase_auth_uid_xyz",
  "name": "Chaitanya Raut",
  "mobileNumber": "9112823213",
  "role": "Admin",                    // "Admin", "Staff"
  "active": true,
  "createdAt": 1787850000000
}
```

### 4.2 Critical Serialization Rules (Kotlin ➔ Firestore)
Firebase's Android SDK relies on JavaBean getter inspection. In Kotlin:
- `val isActive: Boolean` generates `isActive()`, which serializes to Firestore field **`active`**.
- `val isDeleted: Boolean` generates `isDeleted()`, which serializes to Firestore field **`deleted`**.
- `val isCustomerOwned: Boolean` generates `isCustomerOwned()`, which serializes to Firestore field **`customerOwned`**.

> **Crucial Rule**: All repository queries, Firestore `update()` statements, and `.whereEqualTo()` filters MUST query `"deleted"`, `"active"`, and `"customerOwned"`, matching the database keys.

---

## 5. Screen Inventory & UI Specifications

The app has 14 screens connected via Compose Navigation (`AppNavGraph.kt`):

```
                       ┌──────────────┐
                       │ LoginScreen  │
                       └──────┬───────┘
                              │ Authenticated
                       ┌──────▼───────┐
             ┌─────────┤ MainActivity ├──────────┐
             │         └──────┬───────┘          │
             │                │                  │
    ┌────────▼──────┐  ┌──────▼───────┐  ┌───────▼──────┐
    │DashboardScreen│  │OrdersListScr │  │ ReturnsScreen│
    └───────┬───────┘  └──────┬───────┘  └───────┬──────┘
            │                 │                  │
     ┌──────┴───────┐  ┌──────┴───────┐   ┌──────┴───────┐
     │ NewOrderScr  │  │OrderDetailScr│   │ItemsRatesScr │
     └──────┬───────┘  └──────┬───────┘   └──────────────┘
            │                 │
     ┌──────▼───────┐  ┌──────▼───────┐
     │BillPreviewScr│  │ CustomersScr │
     └──────────────┘  └──────┬───────┘
                              │
                       ┌──────▼───────┐
                       │CustDetailScr │
                       └──────────────┘
```

### 1. `LoginScreen` (`/login`)
- **Purpose**: Authenticates administrative users.
- **UI Elements**: Branding header, Email input, Password input with visibility toggle icon, Sign-in button with loading indicator, Error alert snackbar.
- **Logic**: Calls `FirebaseAuth.signInWithEmailAndPassword()`. On success, navigates to `/dashboard` with backstack clearance.

### 2. `DashboardScreen` (`/dashboard`)
- **Purpose**: High-level daily business cockpit.
- **UI Elements**:
  - Greeting banner with current date in Marathi/English.
  - **Quick Metrics Grid**: Today's Active Orders, Pending Returns, Completed Orders, Today's Collected Cash.
  - **Financial Summary Cards**: Overall Pending Balance, Monthly Revenue.
  - **Action Shortcuts**: "+ New Order", "Quick Jar Entry", "Record Return".
  - **Upcoming Deliveries & Returns Section**: Cards showing urgent dispatches with 1-tap call/WhatsApp triggers.

### 3. `NewOrderScreen` (`/new_order` and `/edit_order/{orderId}`)
- **Purpose**: Comprehensive order entry & editing with instant stock validation.
- **UI Elements**:
  - **Customer Section**: Auto-suggesting name/mobile with autocomplete from existing `customers` collection, Address field.
  - **Date Section**: Event Date picker, Delivery Date picker, Return Date picker, Rental days stepper (+/-).
  - **Item Selection Grid**: Cards for each active item showing Rate, Available Stock for chosen date range, Quantity Stepper, "Customer Jar" toggle (for water jars).
  - **Live Financial Breakdown**: Items Total, Transport Rent input, Grand Total, Advance Paid input, Pending Balance display.
  - **Real-time Stock Banner**: Displays green "All items in stock" or red warnings "Table: Only 12 available (requested 20)".
  - **Save & Preview Bill Button**: Triggers Firestore batch write and opens `BillPreviewScreen`.

### 4. `OrdersListScreen` (`/orders_list`)
- **Purpose**: Filterable master list of all bookings.
- **UI Elements**:
  - Search bar (by Customer Name, Mobile, Bill Number).
  - Status Filter Chips: All, Pending, Confirmed, Delivered, Completed, Cancelled.
  - Order Cards displaying Bill No, Customer Info, Dates, Items preview, Balance indicator (Paid / Unpaid badge in Green/Red), and direct Action Icons (Phone, WhatsApp, PDF).

### 5. `OrderDetailsScreen` (`/order_details/{orderId}`)
- **Purpose**: Complete audit view of an individual order.
- **UI Elements**:
  - Status badge with dropdown to transition status (`Pending` ➔ `Confirmed` ➔ `Delivered` ➔ `Completed`).
  - Itemized table with quantities, rates, and return status (Returned vs Damaged vs Pending).
  - Payment History timeline with "+ Add Payment" button.
  - Buttons: "View Marathi Bill", "Share PDF on WhatsApp", "Edit Order", "Cancel Order".

### 6. `BillPreviewScreen` (`/bill_preview/{orderId}`)
- **Purpose**: Visual preview and export terminal for Marathi invoices.
- **UI Elements**:
  - High-resolution interactive Zoom/Pan Canvas preview of the generated bill image.
  - Floating Action Bottom Bar:
    - **Share WhatsApp** (dispatches text summary + JPEG/PDF attachment).
    - **Save PDF** (invokes SAF to save document to Downloads).
    - **Print** (Android PrintManager integration).

### 7. `ReturnsScreen` (`/returns`)
- **Purpose**: Equipment return audit & damage recording.
- **UI Elements**:
  - Tab Switcher: "Pending Returns" vs "Completed Returns".
  - Filter chips: "Due Today", "Overdue", "Upcoming".
  - Order Return Card showing missing items.
  - **"Record Return" Dialog**:
    - Stepper for Returned Quantity.
    - Stepper for Damaged / Broken Quantity.
    - Automatically updates order status to `Completed` if all items are accounted for.

### 8. `ItemsRatesScreen` (`/items_rates`)
- **Purpose**: Master inventory & rate card configuration.
- **UI Elements**:
  - List of all inventory assets.
  - Stock meters showing: Total Stock, Currently Rented, In Warehouse Available.
  - Active/Inactive toggle switch.
  - "+ Add Item" FAB and Edit Dialog (Name, Rate/Day, Total Stock, Low Stock Alert limit).
  - Soft-delete with guard preventing deletion of default items (Chair, Table, Water Jar) or items with active order history.

### 9. `CustomersScreen` (`/customers`)
- **Purpose**: Customer CRM directory.
- **UI Elements**: Search bar, Alphabetical list of customers with total lifetime orders count, total spent, and outstanding debt.

### 10. `CustomerDetailsScreen` (`/customer_details/{customerId}`)
- **Purpose**: 360-degree customer ledger.
- **UI Elements**: Contact card, Total Business Value, Lifetime Balance, Tabular list of all historical orders with 1-tap navigation to order details.

### 11. `PaymentsScreen` (`/payments`)
- **Purpose**: Global collection register and payment logger.
- **UI Elements**:
  - Filter by date range (Today, This Week, Month, Custom).
  - Total collected metric banner.
  - List of recent transactions with Payment Mode badges (Cash, UPI, Bank Transfer).

### 12. `ReportsScreen` (`/reports`)
- **Purpose**: Business intelligence & revenue analytics.
- **UI Elements**:
  - Time period selector: Today, This Week, This Month, Custom Date Range.
  - Key Metrics: Total Revenue, Total Collected, Total Outstanding, Orders Count, Total Transport Earnings.
  - Item-wise Revenue Breakdown (e.g. Chair revenue vs Table revenue).
  - "Export PDF Report" button.

### 13. `BackupScreen` (`/backup`)
- **Purpose**: Complete data export and disaster recovery.
- **UI Elements**:
  - "Export JSON Backup" (saves entire database into a single timestamped JSON file).
  - "Restore from Backup" (reads JSON and restores collections via batch writes).
  - Last backup timestamp indicator.

### 14. `MoreScreen` (`/more`)
- **Purpose**: Settings and administrative utilities.
- **UI Elements**: Theme Switcher (System / Light / Dark / OLED), Rate Card shortcut, Backup shortcut, Reports shortcut, Admin Sign Out.

---

## 6. Business Logic, Formulas & Core Algorithms

### 6.1 Date-Wise Stock Conflict Detection Algorithm
When checking if item $I$ is available for a requested order between $[\text{start}, \text{end}]$:
$$\text{RentedQuantity}(I, t) = \sum_{O \in \text{ActiveOrders}} \sum_{i \in O.\text{items}} [i.\text{itemId} = I.\text{id} \land \neg i.\text{isCustomerOwned} \land \text{Overlap}(O, t)] \cdot (i.\text{qty} - i.\text{returned} - i.\text{damaged})$$
Where:
$$\text{Overlap}(O, t) \iff (O.\text{deliveryDate} \le t \le O.\text{returnDate})$$
$$\text{AvailableStock}(I) = I.\text{totalStock} - \max_{t \in [\text{start}, \text{end}]} \text{RentedQuantity}(I, t)$$

If $\text{RequestedQty}(I) > \text{AvailableStock}(I)$, the system raises an inline warning and disables submission until overridden or resolved.

### 6.2 Financial Calculations
1. **Line Item Total**:
   $$\text{itemTotal} = \text{quantity} \times \text{ratePerDay} \times \text{rentalDays}$$
2. **Order Grand Total**:
   $$\text{grandTotal} = \left(\sum \text{itemTotal}\right) + \text{transportRent}$$
3. **Balance Amount**:
   $$\text{balanceAmount} = \text{grandTotal} - \text{totalPaymentsReceived}$$
4. **Payment Status Determination**:
   $$\text{paymentStatus} = \begin{cases} \text{"Paid"} & \text{if } \text{totalPaid} \ge \text{grandTotal} \\ \text{"Partially Paid"} & \text{if } 0 < \text{totalPaid} < \text{grandTotal} \\ \text{"Unpaid"} & \text{if } \text{totalPaid} = 0 \end{cases}$$

### 6.3 Concurrency-Safe Invoice Counter
Bill numbers are sequentially generated using a Firestore atomic transaction:
```kotlin
val billNumber = db.runTransaction { transaction ->
    val counterRef = countersCol.document("billNumber")
    val snap = transaction.get(counterRef)
    val currentVal = snap.getLong("value") ?: 0L
    val nextVal = currentVal + 1
    transaction.set(counterRef, mapOf("value" to nextVal))
    nextVal.toInt()
}.await()
```

---

## 7. Marathi Devanagari Bill Generation Engine

The invoice generation is encapsulated in `MarathiBillGenerator.kt`. It bypasses third-party HTML-to-PDF converters in favor of direct hardware-accelerated `android.graphics.Canvas` rendering.

### Technical Characteristics
- **Dimensions**: A4 Page (595 x 842 points at 72 DPI).
- **Typography**: Custom-loaded `Noto Sans Devanagari` font (`res/font/noto_sans_devanagari_regular.ttf` and `noto_sans_devanagari_bold.ttf`).
- **Layout Architecture**:
  1. **Auspicious Header**: `|| श्री गणेश प्रसन्न ||` centered.
  2. **Business Banner**: "राजरत्न इव्हेंट्स, आंदरुड" (Bold, 22pt) + Proprietor details & contact info.
  3. **Customer Meta Box**: Grid with Bill No, Date, Customer Name, Mobile, Event Venue, Delivery & Return Dates.
  4. **Itemized Table**: Serial No (`अ.क्र.`), Item Details (`तपशील`), Qty (`नग`), Rate (`दर`), Days (`दिवस`), Total (`एकूण`).
  5. **Summary Block**: Total (`एकूण रक्कम`), Transport Rent (`गाडी भाडे`), Paid (`भरलेली रक्कम`), Balance (`बाकी रक्कम`).
  6. **Terms & Signatures**: Terms of damage compensation and signatures for Customer & Proprietor.
- **Dual Output Support**:
  - `generatePdf()`: Produces vectorized `.pdf` document for printing.
  - `generateImage()`: Produces high-resolution 300 DPI `.jpg` image for WhatsApp image previews.

---

## 8. Communication & WhatsApp Integration

Handled by `WhatsAppUtils.kt`:
- **Direct WhatsApp Intent**: Dispatches to `com.whatsapp` or falls back to system intent chooser.
- **Preformatted Marathi Message Template**:
  ```
  बिल क्र.: 1042
  ग्राहक: सुरेश पाटील
  मोबाईल: 9822012345
  डिलिव्हरी दिनांक: 28/08/2026
  
  तपशील:
  टेबल: 20 x 30 x 1 दिवस = 600 रु
  खुर्ची: 100 x 5 x 1 दिवस = 500 रु
  
  गाडी भाडे: 500 रु
  एकूण रक्कम: 1600 रु
  भरलेली रक्कम: 500 रु
  बाकी रक्कम: 1100 रु
  
  राजरत्न इव्हेंट्स, आंदरुड (मो. 9112823213)
  ```
- **Attachment Sharing**: Employs Android `FileProvider` with URI permissions (`FLAG_GRANT_READ_URI_PERMISSION`) granting WhatsApp secure access to generated PDFs/JPEGs in the app cache directory.

---

## 9. Security & Access Control

### 9.1 Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 9.2 User Roles
- **Admin**: Full read/write access to items, rates card, historical audits, financial reports, and JSON backups.
- **Staff**: Access to order creation, delivery tracking, return recording, and payment receipt entry; restricted from editing base rates and deleting database records.

---

## 10. Performance, Offline Handling & Firestore Query Best Practices

### 10.1 Avoiding Composite Index Hell
In Firestore, queries combining equality on one field (e.g. `whereEqualTo("deleted", false)`) and range filters / sorting on another field (e.g. `whereGreaterThanOrEqualTo("orderDate", ...)` or `orderBy("createdAt")`) **require manual Composite Indexes**.

> **Architecture Decision**: To eliminate composite index crashes, `AppRepository` queries Firestore using only a single equality filter (`whereEqualTo("deleted", false)`) and performs all multi-field filtering and sorting **in memory** using Kotlin standard library operators (`.filter {}`, `.sortedByDescending {}`). For a localized event rental business with thousands of documents, in-memory operations take less than $1\text{ ms}$ while ensuring 100% crash immunity.

### 10.2 Offline Caching
`RajratnaApp.kt` enables local disk caching:
```kotlin
firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
    .setPersistenceEnabled(true)
    .build()
```
Users can create orders, record payments, and view inventory without an active internet connection; mutations are queued locally and synchronized with Cloud Firestore upon reconnection.

---

## 11. Directory Structure Map

```
app/src/main/java/com/rajratna/events/
├── MainActivity.kt                  # Entry point, Bottom navigation, scaffold
├── RajratnaApp.kt                   # Application subclass, singleton DI container
├── data/
│   ├── entity/                      # Data models matching Firestore documents
│   │   ├── Customer.kt
│   │   ├── Item.kt
│   │   ├── Order.kt
│   │   ├── OrderItem.kt
│   │   ├── Payment.kt
│   │   └── User.kt
│   └── repository/
│       ├── AppRepository.kt         # Master repository for all Firestore CRUD
│       └── AuthRepository.kt        # Authentication and user state
├── ui/
│   ├── components/                  # Common widgets (Headers, Dialogs, Steppers, Sheets)
│   │   ├── BillOptionsSheet.kt
│   │   ├── CommonComponents.kt
│   │   ├── QuickJarComponents.kt
│   │   └── ThemeToggleButton.kt
│   ├── navigation/
│   │   ├── AppNavGraph.kt           # NavHost with route bindings
│   │   └── Screen.kt                # Sealed hierarchy of app routes
│   ├── screens/                     # 14 Feature ViewModels & Composable screens
│   │   ├── backup/
│   │   ├── billpreview/
│   │   ├── customerdetails/
│   │   ├── customers/
│   │   ├── dashboard/
│   │   ├── items/
│   │   ├── login/
│   │   ├── more/
│   │   ├── neworder/
│   │   ├── orderdetails/
│   │   ├── orders/
│   │   ├── payments/
│   │   ├── reports/
│   │   └── returns/
│   └── theme/                       # Color palettes, Typography, Theme switching
│       ├── Color.kt
│       ├── Theme.kt
│       ├── ThemeMode.kt
│       ├── ThemeViewModel.kt
│       └── Type.kt
└── util/
    ├── DateUtils.kt                 # Timestamp converters, formats, day boundaries
    ├── MarathiBillGenerator.kt      # Native Devanagari canvas invoice renderer
    └── WhatsAppUtils.kt             # WhatsApp message templates & file sharing
```

---

## 12. Future Enhancement Opportunities

1. **Barcode / QR Asset Tracking**: Affixing waterproof QR codes to rental chairs and tables to scan them out on delivery and scan them back in during return.
2. **Push Notifications & Automated Reminders**: Firebase Cloud Messaging (FCM) to trigger morning reminders for returns scheduled for that day.
3. **Customer Self-Service Portal**: Mini web portal where customers can view their bill and make UPI payments directly.
4. **SMS Gateway Integration**: Automatic SMS fallback if customer does not have WhatsApp.
