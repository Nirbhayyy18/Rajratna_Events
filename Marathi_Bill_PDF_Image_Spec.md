# Rajratna Events - Marathi Bill PDF/Image Feature

## Goal

Add a professional Marathi bill format to the existing Rajratna Events Android app.

Current bill sharing is text-based for WhatsApp. Keep that option, but add a proper bill that can be shared as:

- PDF
- Image
- WhatsApp attachment

The bill should look like a clean digital version of the existing offline printed bill book.

Do not rebuild the full app. Do not change unrelated screens or business logic.

## Required Tech Stack

- Kotlin
- Jetpack Compose
- Native Android app
- Room / SQLite existing data
- Android share intents
- FileProvider for PDF/image sharing
- Offline generation, no internet required

## Where To Add This Feature

Add bill actions in these places:

- After order confirmation success
- Order Details screen
- Optional Bill Preview screen or bottom sheet

Required actions:

- Preview Marathi Bill
- Share Marathi Bill PDF
- Share Marathi Bill Image
- Share Text on WhatsApp

PDF/image sharing should be the main professional option. Text sharing should remain as a quick fallback.

## Bill Language

The bill should be in Marathi.

Use clear and readable Marathi labels. Marathi text must render correctly in both PDF and image.

Important:

- Use a Devanagari-supported font.
- Recommended font: Noto Sans Devanagari or Noto Serif Devanagari.
- Bundle the font inside the app.
- Do not rely on a font that may not exist on every phone.
- Check that Marathi matras, half letters, and conjunct letters render correctly.

## Marathi Bill Template

```text
|| श्री गणेश प्रसन्न ||

राजरत्न इव्हेंट्स, आंदरुड
ता. फलटण, जि. सातारा

आमच्याकडे सर्व कार्यक्रमासाठी केटरिंग टेबल, खुर्ची व थंडगार पाण्याचे जार योग्य दरात मिळतील

प्रो. चैतन्य राजेंद्र राऊत
मो. ९११२८२३२१३

बिल क्र.: #12
दिनांक: 21 मे 2026

ग्राहकाचे नाव: शिवानी
मोबाईल क्रमांक: 9874561238
पत्ता: आंद्रुड

डिलिव्हरी दिनांक: 21 मे 2026
परत दिनांक: 22 मे 2026
भाडे दिवस: 1

------------------------------------------------
अ.क्र.  तपशील           नग    दर    दिवस   एकूण
------------------------------------------------
1      खुर्ची           50    ₹5     1      ₹250
2      टेबल            10    ₹30    1      ₹300
3      पाण्याचे जार     20    ₹30    1      ₹600
4      गाडी भाडे                         ₹400
------------------------------------------------
एकूण रक्कम                         ₹1,550
आगाऊ रक्कम                         ₹500
बाकी रक्कम                         ₹1,050
------------------------------------------------

टीप:
वस्तू गहाळ किंवा खराब झाल्यास भरपाई आकारली जाईल.

ग्राहकाची सही                         प्रोप्रायटर
```

## Required Bill Data

Use existing order/payment data.

Header:

- `|| श्री गणेश प्रसन्न ||`
- `राजरत्न इव्हेंट्स, आंदरुड`
- `ता. फलटण, जि. सातारा`
- Service description line
- Owner name
- Owner mobile number

Customer/order details:

- Bill number / order number
- Bill date
- Customer name
- Customer mobile number
- Customer address
- Delivery date
- Return date
- Rental days

Item table:

- Sr. No.
- Item name
- Quantity
- Rate
- Days
- Amount

Amount summary:

- Items total
- Transport rent
- Grand total
- Advance paid / paid amount
- Balance amount
- Payment status

Footer:

- Missing/damaged item note
- Customer signature area
- Proprietor/owner signature area

## Marathi Label Mapping

| English | Marathi |
|---|---|
| Bill No | बिल क्र. |
| Date | दिनांक |
| Customer Name | ग्राहकाचे नाव |
| Mobile Number | मोबाईल क्रमांक |
| Address | पत्ता |
| Delivery Date | डिलिव्हरी दिनांक |
| Return Date | परत दिनांक |
| Rental Days | भाडे दिवस |
| Sr. No. | अ.क्र. |
| Details | तपशील |
| Quantity | नग |
| Rate | दर |
| Days | दिवस |
| Amount | एकूण |
| Chair | खुर्ची |
| Table | टेबल |
| Water Jar | पाण्याचे जार |
| Transport Rent | गाडी भाडे |
| Total Amount | एकूण रक्कम |
| Advance Paid | आगाऊ रक्कम |
| Paid Amount | भरलेली रक्कम |
| Balance | बाकी रक्कम |
| Note | टीप |
| Customer Signature | ग्राहकाची सही |
| Proprietor | प्रोप्रायटर |

## Customer-Owned Water Jar Rule

Water jar can be either:

- Rajratna Events jar
- Customer-owned jar

If water jar source is customer-owned, show it clearly in the bill:

```text
पाण्याचे जार - ग्राहकाचे जार
```

Rules:

- Customer-owned jars can appear in billing if charged.
- Customer-owned jars must not reduce Rajratna Events stock.
- Customer-owned jars should not appear as pending return for owner stock.
- If the order contains both own jars and customer jars, show separate rows.

Example:

```text
1  पाण्याचे जार                  20   ₹30   1   ₹600
2  पाण्याचे जार - ग्राहकाचे जार   10   ₹10   1   ₹100
```

## PDF Design Requirements

The PDF should be printable and easy to read.

Style:

- White background
- Thin black/gray border
- Simple bill-book layout
- No decorative app cards
- No heavy colors
- Professional receipt style

Layout:

- Portrait orientation
- A4 or compact receipt-style page
- Enough padding
- Table rows must not overlap
- Long customer names/address should wrap cleanly
- Amounts should be right-aligned
- Footer should stay visible

File name example:

```text
Rajratna_Bill_12_Shivani.pdf
```

## Image Design Requirements

Generate an image version of the same bill for easy WhatsApp sharing.

Format:

- PNG preferred
- JPEG acceptable if file size is an issue
- High enough resolution for reading on phone
- Same content as PDF

File name example:

```text
Rajratna_Bill_12_Shivani.png
```

## Recommended Technical Implementation

Use one shared bill template so PDF and image do not look different.

Recommended approach:

1. Create a Compose bill preview component.
2. Render the same bill layout for preview.
3. Generate image from the rendered bill.
4. Generate PDF using the same layout measurements or by drawing the generated bill bitmap into a PDF page.

Important:

- Ensure Marathi font is applied to all bill text.
- Use FileProvider for sharing generated files.
- Store generated PDF/image in app cache or app documents folder.
- Regenerate bill every time before sharing so latest payments are reflected.
- Do not permanently store duplicate old bills unless the owner explicitly saves/exports.

Avoid:

- Broken Marathi text in PDF
- Cropped bill image
- Text overlap
- Different PDF and image content
- Sharing files without FileProvider
- Internet dependency

## WhatsApp Sharing

When sharing PDF/image, attach the file and include a short message:

```text
राजरत्न इव्हेंट्स
बिल क्र.: #12
एकूण रक्कम: ₹1,550
भरलेली रक्कम: ₹500
बाकी रक्कम: ₹1,050
```

If WhatsApp is not installed, use the normal Android share sheet.

## UI Flow

After confirming order:

```text
Order Confirmed

[Preview Marathi Bill]
[Share Bill Image]
[Share Bill PDF]
[Share Text on WhatsApp]
```

On Order Details screen:

Add a `Bill` action. Tapping it opens a bottom sheet:

```text
Bill Options

Preview Marathi Bill
Share Bill Image
Share Bill PDF
Share Text on WhatsApp
```

Keep the UI consistent with the existing app.

## Validation And Edge Cases

Handle these cases:

- No advance paid
- Fully paid order
- Partially paid order
- Pending balance
- Transport rent is empty or zero
- Customer address is empty
- Long customer name
- Multiple item rows
- Only one item row
- Customer-owned water jar
- Updated payment should reflect in generated bill
- Cancelled order should not generate a normal active bill unless opened from order history

## Test Cases

### Case 1 - Basic Bill

Order:

- Chair: 50
- Rate: ₹5
- Days: 1
- Transport: ₹400
- Advance: ₹500

Expected:

- PDF generated
- Image generated
- Total, advance, balance correct
- Marathi text readable

### Case 2 - No Advance

Expected:

- Advance/paid amount shows ₹0
- Full amount appears as balance

### Case 3 - Fully Paid

Expected:

- Balance shows ₹0
- Payment status is clear

### Case 4 - Customer-Owned Jar

Order:

- Customer-owned jars: 10

Expected:

- Bill row says `पाण्याचे जार - ग्राहकाचे जार`
- Stock is not affected

### Case 5 - Long Address

Expected:

- Address wraps
- Layout does not break

### Case 6 - WhatsApp Share

Expected:

- PDF/image attachment shares successfully
- Marathi summary message is included

## Ready Prompt For Antigravity

```text
Implement Marathi PDF/image bill sharing for the existing Kotlin Jetpack Compose Android app.

Do not rebuild the app. Do not redesign unrelated screens. Do not change existing order, payment, stock, return, or navigation logic except where needed to read data for bill generation.

Add bill actions after order confirmation and on Order Details:
- Preview Marathi Bill
- Share Bill Image
- Share Bill PDF
- Share Text on WhatsApp

Create a professional Marathi bill template inspired by the offline printed bill book.

Bill must include:
- || श्री गणेश प्रसन्न ||
- राजरत्न इव्हेंट्स, आंदरुड
- ता. फलटण, जि. सातारा
- Service description line
- Owner name and mobile
- Bill number
- Date
- Customer name
- Mobile number
- Address
- Delivery date
- Return date
- Rental days
- Item table with अ.क्र., तपशील, नग, दर, दिवस, एकूण
- Transport rent
- Total amount
- Advance/paid amount
- Balance amount
- Note about missing/damaged items
- Customer signature
- Proprietor signature

Use Marathi labels:
Chair = खुर्ची
Table = टेबल
Water Jar = पाण्याचे जार
Transport Rent = गाडी भाडे

If water jar is customer-owned, show:
पाण्याचे जार - ग्राहकाचे जार
Customer-owned jars must not affect Rajratna Events stock.

Technical requirements:
- Use Devanagari-supported font, preferably Noto Sans Devanagari.
- Bundle the font in the app.
- Generate PDF offline.
- Generate PNG/image offline.
- Share files using FileProvider and Android share intent.
- WhatsApp sharing should attach PDF/image and include short Marathi summary text.
- Generated bill must always use latest order/payment data.
- Avoid text overlap, cropped images, and broken Marathi rendering.
```

