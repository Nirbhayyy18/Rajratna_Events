# Rajratna Events - Quick Jar Entry Feature

## Goal

Add a fast water jar entry workflow inside the current app.

This feature is for repeat or random jar customers where:

- Customer may come again and again.
- Dates are not fixed.
- Quantity is not fixed.
- Customer may take 1 jar today and 2 jars next time.
- Customer may take jars from home/shop.
- Some customers may take jars for several days and later ask:
  - total jars किती झाले?
  - total amount किती झाले?
  - payment किती झाले?
  - balance किती आहे?

The app should save time by avoiding repeated customer name, mobile number, and address entry.

## Important Direction

Do not create a separate complex module.
Do not add a new bottom navigation tab.
Do not add village/route selection.
Do not add fixed repeat schedules.
Do not auto-generate future orders.

Use the current app structure:

- Customers
- Orders
- Returns
- Payments
- Reports
- Inventory

The new feature should work as a shortcut over the existing order system.

## Core Idea

Add a `+ Jar` action for existing customers.

When the owner taps `+ Jar`, open a small bottom sheet. The owner selects jar quantity, jar source, payment, and saves.

Behind the scenes, the app creates a normal water jar order using existing order logic.

This keeps reports, payments, returns, stock, and customer history consistent.

## Required Tech Stack

- Kotlin
- Jetpack Compose
- Native Android app
- Existing Room / SQLite database
- Existing MVVM structure
- Existing order, payment, return, stock, and report logic

Do not convert this into a web app, Flutter app, or React Native app.

## Where To Add

### Customers Screen

Each customer card should show:

- Customer name
- Mobile number
- Pending amount
- This month jar count
- Pending jar return count, if any
- Last jar entry, if available
- Quick action: `+ Jar`

Example:

```text
Abhay Raut
9689159776

This Month: 12 jars
Pending: ₹210
Pending Return: 2 jars
Last: 2 jars on 24 Jun

[Call] [+ Jar]
```

If there are no jar entries:

```text
This Month: 0 jars
No pending jars
```

### Customer Detail / Existing Customer History Area

If the app already opens a customer detail screen or expanded customer history, add a jar summary there.

Do not add a new main screen.

Show:

- Total jars this month
- Total jar amount this month
- Paid amount
- Pending amount
- Pending return jars
- Recent jar entries

Example:

```text
Jar Summary - This Month

Jars: 12
Amount: ₹360
Paid: ₹150
Balance: ₹210
Pending Return: 2 jars

Recent Jar Entries
26 Jun - 2 jars - ₹60 - unpaid
24 Jun - 3 jars - ₹90 - paid
22 Jun - 1 jar - ₹30 - paid
```

Add actions:

- `+ Jar`
- `Return Jar` if pending return exists
- `Record Payment` if pending amount exists
- `WhatsApp Reminder`

## Quick Jar Entry Bottom Sheet

Open this bottom sheet when owner taps `+ Jar`.

```text
Add Jar Entry

Customer: Abhay Raut
Mobile: 9689159776
Balance: ₹210
Pending Return: 2 jars
Last: 2 jars on 24 Jun

Date
[Today]

Quantity
[1] [2] [3] [Custom]

Jar Source
[Our Jar] [Customer Jar]

Rate
₹30 per jar

Paid Now
[₹0] [Full] [Custom]

[Save Jar Entry]
```

### Default Values

- Date: Today
- Quantity: empty until selected
- Jar Source: Our Jar
- Rate: current Water Jar rate
- Paid Now: ₹0
- Order status after save: Delivered

### Why Status Should Be Delivered

This shortcut is used when jars are actually given to the customer immediately.

So the generated order should not wait for `Mark Delivered`.

It should directly behave like a delivered water jar order.

## Normal Order Created Behind The Scenes

When owner saves quick jar entry, create a normal order:

```text
Customer = selected existing customer
Item = Water Jar
Quantity = selected quantity
Rate = water jar rate
Delivery date = selected date, default today
Expected return date = selected date or next day, based on current app default
Transport rent = 0
Status = Delivered
Payment = entered paid amount
```

The order should appear in:

- Orders screen
- Customer history
- Reports
- Payments
- Returns if Our Jar has pending return
- Dashboard stock

## Jar Return Tracking

Jar return must be trackable.

When an `Our Jar` quick entry is saved:

```text
quantitySent = selected quantity
quantityReturned = 0
quantityPending = selected quantity
```

Example:

```text
Sent: 2 jars
Returned: 0
Pending Return: 2 jars
```

Those pending jars must count as out stock.

## Return Jar Action

If customer has pending jars, show `Return Jar`.

This can open a bottom sheet:

```text
Return Jar

Customer: Abhay Raut
Pending Return: 2 jars

Returned Now
[1] [2] [Custom]

Damaged/Missing
[0] [1] [Custom]

[Save Return]
```

After saving:

```text
quantityReturned += returnedNow
quantityPending -= returnedNow
```

If damaged/missing is recorded, keep it visible on the order and customer history.

### Partial Return Example

Before:

```text
Sent: 2
Returned: 0
Pending: 2
```

Owner records:

```text
Returned Now: 1
```

After:

```text
Sent: 2
Returned: 1
Pending: 1
```

Only 1 jar comes back into available stock.

## Customer Jar Rule

If jar source is `Customer Jar`:

- It can be billed if needed.
- It should appear in customer history.
- It should appear in reports/income.
- It must not reduce Rajratna Events stock.
- It must not appear in pending returns.
- It does not need `Return Jar` tracking for owner stock.

Show clearly in history:

```text
26 Jun - 2 customer jars - ₹60
```

## Payment Handling

Many jar customers may pay after several entries.

Do not force owner to open each order.

### Quick Payment From Customer

On customer card/detail, if pending amount exists, show:

```text
Record Payment
```

Payment bottom sheet:

```text
Record Payment

Customer: Abhay Raut
Pending: ₹210

Amount
[Full ₹210] [Custom]

Mode
[Cash] [UPI] [Other]

[Save Payment]
```

### Payment Allocation Rule

When customer pays a lump sum:

- Apply payment to the oldest unpaid orders first.
- Continue until entered amount is fully used.
- If amount is more than pending balance, show confirmation before saving.

This keeps existing order-level payment logic consistent.

Example:

```text
Pending orders:
26 Jun - ₹60 unpaid
28 Jun - ₹30 unpaid
30 Jun - ₹90 unpaid

Customer pays ₹100
```

Expected:

```text
26 Jun order paid ₹60
28 Jun order paid ₹30
30 Jun order paid ₹10
Remaining pending on 30 Jun = ₹80
```

Reports should count received amount by payment date.

## Customer Summary Logic

For each customer, calculate:

```text
This month jars = sum of Water Jar quantity from non-cancelled orders in current month
Jar amount = sum of Water Jar amount from non-cancelled orders
Paid amount = sum of payments for customer
Pending amount = total order amount - paid amount
Pending return jars = sum of Our Jar quantityPending
Last jar entry = latest Water Jar order for customer
```

Do not count cancelled orders.

Customer-owned jars count in jar amount but not in stock or pending returns.

## Stock Logic

Quick Jar Entry must use the same shared stock engine as normal orders.

For Our Jar:

```text
Available stock = total stock - pending Our Jar quantities - other active pending quantities
```

For Customer Jar:

```text
No stock change
```

Before saving an Our Jar entry:

- Check available water jar stock for selected date.
- If selected quantity exceeds available stock, block save.
- Show clear error:

```text
Only 3 water jars available today.
```

Do not block Customer Jar entries due to Rajratna stock.

## Search Support

Customers screen search should support:

- Name
- Mobile number
- Address

This helps owner quickly find repeat customers from anywhere.

No village/route grouping is required.

## UI Rules

Keep UI simple and consistent with current app.

Do:

- Use bottom sheets for quick actions.
- Keep the customer card compact.
- Show only practical information.
- Use red for pending money or pending return warnings.
- Use green for paid/no pending.
- Use current app card style and colors.

Do not:

- Add new bottom nav tab.
- Add complicated CRM fields.
- Add route planning.
- Add fixed schedules.
- Add auto-generated future entries.
- Make owner choose too many options before saving.

## Suggested Customer Card Layout

```text
Abhay Raut                      [Call]
9689159776

This Month: 12 jars
Pending: ₹210
Return: 2 jars pending
Last: 2 jars on 24 Jun

[+ Jar] [Return]
```

If no pending return:

```text
[+ Jar]
```

If no pending amount:

```text
Paid clear
```

## Suggested Quick Save Confirmation

For quick quantity buttons, confirm before saving:

```text
Add 2 water jars for Abhay today?

Amount: ₹60
Paid Now: ₹0
Balance: ₹60

[Cancel] [Save]
```

This prevents accidental entries.

## WhatsApp Summary For Customer

From customer detail, owner should be able to share a jar/payment summary:

```text
Rajratna Events

Abhay Raut
This Month Jar Summary

Total Jars: 12
Total Amount: ₹360
Paid: ₹150
Balance: ₹210
Pending Return: 2 jars
```

This solves the customer question:

```text
Kiti jar zale total?
Payment kiti zale?
Balance kiti aahe?
```

## Test Cases

### Case 1 - Existing Customer Takes 1 Jar

Action:

```text
Customer card -> + Jar -> Quantity 1 -> Our Jar -> Paid ₹0 -> Save
```

Expected:

- Normal order created
- Status Delivered
- Water Jar sent = 1
- Pending return = 1
- Customer pending amount increases by ₹30
- Stock decreases by 1

### Case 2 - Same Customer Takes Different Quantity Next Time

Action:

```text
Same customer -> + Jar -> Quantity 2 -> Save
```

Expected:

- New order created
- No name/mobile re-entry needed
- Customer month jar total increases correctly

### Case 3 - Customer-Owned Jar

Action:

```text
+ Jar -> Quantity 2 -> Customer Jar -> Save
```

Expected:

- Order/history entry created
- Amount counted if charged
- Stock does not decrease
- Pending return does not increase

### Case 4 - Partial Return

Starting state:

```text
Customer pending return = 2 jars
```

Action:

```text
Return Jar -> Returned Now 1 -> Save
```

Expected:

- Pending return becomes 1
- Stock restores by 1

### Case 5 - Lump Sum Payment

Starting state:

```text
Customer pending = ₹180 across multiple jar orders
```

Action:

```text
Record Payment -> ₹100 -> Save
```

Expected:

- Payment applies to oldest unpaid orders first
- Customer pending becomes ₹80
- Reports show ₹100 received on payment date

### Case 6 - Stock Not Available

Starting state:

```text
Available water jars today = 1
```

Action:

```text
+ Jar -> Our Jar -> Quantity 3
```

Expected:

- Save blocked
- Error shown:
  `Only 1 water jar available today.`

### Case 7 - Customer Asks Total After 7-8 Days

Action:

```text
Open customer detail/history
```

Expected:

- Shows total jars
- Shows total amount
- Shows paid amount
- Shows balance
- Shows pending return jars

## Ready Prompt For Antigravity

```text
Implement Quick Jar Entry for existing customers in the current Kotlin Jetpack Compose Android app.

Do not rebuild the app.
Do not add a new bottom navigation tab.
Do not add route/village selection.
Do not add fixed schedules or auto-repeat orders.
Do not create a separate complex jar module.

Use existing Customers, Orders, Returns, Payments, Reports, and Inventory logic.

Feature:
- Add + Jar action on customer cards and customer detail/history.
- Tapping + Jar opens a bottom sheet.
- Owner selects quantity, jar source, paid amount, and saves.
- Behind the scenes create a normal delivered Water Jar order for that customer.

Quick Jar bottom sheet:
- Customer name/mobile shown
- Current balance shown
- Pending return jars shown
- Last jar entry shown
- Date default Today
- Quantity chips: 1, 2, 3, Custom
- Jar Source: Our Jar / Customer Jar
- Rate uses current Water Jar rate
- Paid Now: ₹0, Full, Custom
- Save Jar Entry button

Logic:
- Our Jar should reduce stock and create pending return quantity.
- Customer Jar should not reduce stock and should not require return tracking.
- All jar entries should appear in Orders, Customer history, Reports, and Payments.
- Save should be blocked if Our Jar quantity exceeds available Water Jar stock.
- Customer-owned jar should not be blocked by Rajratna stock.

Return:
- If customer has pending jars, show Return Jar action.
- Return Jar bottom sheet should allow returned quantity and damaged/missing quantity.
- Partial returns must update pending return quantity correctly.

Payment:
- Add Record Payment from customer detail/card if pending amount exists.
- If customer pays lump sum, allocate payment to oldest unpaid orders first.
- Reports should count received amount by payment date.

Customer card should show:
- Name
- Mobile
- This month jar count
- Pending amount
- Pending return jars
- Last jar entry
- + Jar action
- Return action only if jars are pending return

Customer detail/history should show:
- Jar total for current month
- Jar amount
- Paid
- Balance
- Pending return jars
- Recent jar entries
- WhatsApp summary option

Keep UI consistent with current app style.
Do not remove existing working features.
```

