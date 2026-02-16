# Compliance & Localization SDK

## Overview

This SDK provides comprehensive compliance and localization support for the Islamic Financing Platform, implementing:

- **ZATCA E-Invoicing**: Full integration with Saudi tax authority e-invoicing requirements
- **SAMA Compliance**: Audit logging, regulatory reporting, and data residency enforcement
- **VAT Calculation**: KSA-compliant VAT calculation (15% on profit only, not principal)
- **Hijri Calendar**: Full Hijri-Gregorian date conversion and Islamic holiday support

## Key Features

### 1. ZATCA E-Invoicing
- **ZatcaClient**: REST client for ZATCA API integration
  - Invoice hash generation (SHA-256)
  - Digital signature with CSR (ECDSA/RSA-2048)
  - Real-time clearance for B2B invoices > 1000 SAR
  - Batch reporting for B2C invoices
  - Invoice status checking

- **ZatcaInvoiceBuilder**: Constructs compliant invoices
  - UBL 2.1 XML format
  - Automatic VAT calculation
  - Multi-language support (Arabic/English)

- **ZatcaQRCodeGenerator**: TLV-encoded QR codes
  - ZATCA-compliant format
  - Phase 2 digital signature support

### 2. SAMA Compliance
- **SamaAuditLogger**: Immutable audit logging
  - 7-year retention requirement
  - Integrity hash verification
  - User action tracking
  - Financial transaction logging

- **SamaReportGenerator**: Regulatory reports
  - Monthly NPL reports
  - Quarterly risk reports
  - Annual compliance reports
  - IFRS 9 stage classification

- **SamaDataResidency**: Data localization enforcement
  - KSA-only data storage validation
  - Cross-border transfer controls
  - IP address validation
  - DR site compliance

### 3. VAT Calculation
- **VatCalculator**: Islamic finance-compliant VAT
  - 15% VAT on profit portion only
  - Principal exempt from VAT
  - Fee VAT calculation
  - VAT extraction from totals

- **VatExemptionChecker**: Exemption validation
  - Product type exemptions
  - Customer type exemptions
  - Cross-border treatment

### 4. Hijri Calendar Support
- **HijriCalendar**: Date conversion utilities
  - Hijri ↔ Gregorian conversion
  - Arabic/English formatting
  - Leap year detection
  - Date arithmetic

- **IslamicHolidayChecker**: Holiday management
  - Ramadan detection
  - Eid dates (al-Fitr, al-Adha)
  - Business day calculation
  - Holiday period validation

## Configuration

Add to your `application.yml`:

```yaml
compliance:
  institution-id: "YOUR_INSTITUTION_ID"
  tenant-id: "DEFAULT"
  environment: "production"

  sama:
    enabled: true
    api-url: "https://api.sama.gov.sa"
    api-key: "${SAMA_API_KEY}"
    reporting-frequency: "monthly"

  zatca:
    enabled: true
    api-url: "https://gw-apic-gov.gazt.gov.sa/e-invoicing/developer-portal"
    api-token: "${ZATCA_API_TOKEN}"
    certificate: "${ZATCA_CERTIFICATE}"
    private-key: "${ZATCA_PRIVATE_KEY}"
    vat-registration-number: "300000000000003"

  vat:
    enabled: true
    vat-number: "300000000000003"

  data-residency:
    primary-data-center: "riyadh"
    backup-data-center: "jeddah"
    dr-data-center: "dammam"

  localization:
    default-language: "ar"
    hijri-calendar-enabled: true
    show-both-calendars: true
```

## Usage Examples

### ZATCA E-Invoicing

```java
@Autowired
private ZatcaClient zatcaClient;
@Autowired
private ZatcaInvoiceBuilder invoiceBuilder;

// Build invoice
InvoiceRequest request = createInvoiceRequest();
ZatcaInvoice invoice = invoiceBuilder.buildInvoice(request);

// Submit to ZATCA
ZatcaClearanceResponse response = zatcaClient.submitInvoice(invoice);

// Check status
ZatcaInvoiceStatus status = zatcaClient.getInvoiceStatus(response.getInvoiceUuid());
```

### VAT Calculation

```java
@Autowired
private VatCalculator vatCalculator;

// Calculate VAT on profit (Islamic finance)
Money profit = Money.of(new BigDecimal("1000"), "SAR");
Money vat = vatCalculator.calculateVatOnProfit(profit); // 150 SAR (15%)

// Calculate installment VAT
VatBreakdown breakdown = vatCalculator.calculateInstallmentVat(
    principal, profit, "MURABAHA"
);
```

### Hijri Calendar

```java
@Autowired
private HijriCalendar hijriCalendar;

// Convert dates
LocalDate gregorian = LocalDate.now();
HijriDate hijri = hijriCalendar.convertGregorianToHijri(gregorian);

// Format in Arabic
String arabicDate = hijriCalendar.formatArabic(hijri); // "15 رمضان 1445"

// Check Islamic holidays
boolean isRamadan = islamicHolidayChecker.isRamadan(gregorian);
```

### SAMA Audit Logging

```java
@Autowired
private SamaAuditLogger auditLogger;

// Log loan event
auditLogger.logLoanEvent(
    userId,
    loanId,
    "DISBURSEMENT",
    Map.of("amount", 50000, "product", "MURABAHA")
);

// Log compliance violation
auditLogger.logComplianceViolation(
    userId,
    "DATA_ACCESS",
    "Unauthorized PII access attempt",
    "HIGH"
);
```

## Model Classes to Create

The following model classes need to be created in the `model` package:

### ZATCA Models
- `ZatcaInvoice` - Main invoice structure
- `ZatcaClearanceResponse` - API response for clearance
- `ZatcaInvoiceStatus` - Invoice status response
- `InvoiceRequest` - Invoice creation request
- `InvoiceLineItem` - Line items in invoice
- `PartyInfo` - Buyer/seller information
- `AddressInfo` - Address details
- `TaxScheme` - VAT scheme info
- `TaxTotal` - Total tax amounts
- `TaxSubtotal` - Tax by category

### SAMA Models
- `AuditEvent` - Audit log entry
- `AuditEventType` - Enum of event types
- `NplReport` - Non-performing loan report
- `RiskReport` - Risk assessment report
- `ComplianceReport` - Annual compliance report
- `LoanData` - Loan information for reports
- `StageClassification` - IFRS 9 stages

### Common Models
- `SellerInfo` - Platform seller details
- `BuyerInfo` - Customer buyer details
- `Address` - Physical address
- `LineItemRequest` - Line item in request

## Testing

Run the comprehensive test suite:

```bash
# Run all tests
mvn test -pl shared-libraries/compliance-localization-sdk

# Run specific test categories
mvn test -Dtest=ZatcaClientTest
mvn test -Dtest=VatCalculatorTest
mvn test -Dtest=HijriCalendarTest
mvn test -Dtest=SamaAuditLoggerTest
```

## Test Coverage Requirements

- Unit tests for all calculators and converters
- Integration tests with WireMock for ZATCA API
- Hijri date conversion accuracy tests
- VAT calculation precision tests
- Data residency validation tests
- Audit log immutability tests

## Dependencies

- Spring Boot 3.x
- Apache HttpClient 5
- Bouncy Castle (cryptography)
- ZXing (QR codes)
- Jackson (JSON)
- Java Time API (Hijri chronology)

## Compliance Checklist

- ✅ ZATCA Phase 2 e-invoicing ready
- ✅ SAMA data residency compliant
- ✅ 15% VAT on profit only (Islamic finance)
- ✅ Hijri calendar with Islamic holidays
- ✅ 7-year audit log retention
- ✅ Immutable audit trail
- ✅ Arabic/English localization

## Support

For issues or questions, please refer to the master blueprint documentation:
- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
- `/var/www/docs/islamic-financing/master-blueprint/08_KSA_GOVERNMENT_APIS.md`
- `/var/www/docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md`