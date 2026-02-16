# 🛡️ Prompt 06: Compliance & Localization SDK Implementation

**Objective**: Implement the `compliance-localization-sdk` for ZATCA e-invoicing, SAMA compliance, VAT, and Hijri calendar.

**Prerequisites**:
- ✅ Prompt 00-05 complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/03_SECURITY_DATA_RESIDENCY.md`
  - SAMA compliance requirements
  - Data localization rules

- `/var/www/docs/islamic-financing/master-blueprint/08_KSA_GOVERNMENT_APIS.md`
  - ZATCA e-invoicing integration
  - VAT calculation rules (15%)

- `/var/www/docs/islamic-financing/master-blueprint/17_LOAN_SERVICING_RESTRUCTURING.md`
  - VAT on profit invoicing

---

## 🎯 Implementation Requirements

### Technologies
- **ZATCA SDK**: Latest (check ZATCA developer portal)
- **Hijri Calendar**: Java `java.time.chrono.HijrahChronology`

### What to Implement

#### 1. ZATCA E-Invoicing (in `zatca/`)
- `ZatcaClient` - REST client for ZATCA API
  - `generateInvoiceHash()` - SHA-256 hash
  - `signInvoice()` - Digital signature with CSR
  - `submitInvoice()` - Submit to ZATCA for clearance/reporting
  - `getInvoiceStatus()` - Check clearance status
- `ZatcaInvoiceBuilder` - Build compliant invoices
- `ZatcaQRCodeGenerator` - Generate TLV QR codes
- `ZatcaMapper` - Map domain invoices to ZATCA XML format

ZATCA API Integration:
- **Clearance**: Real-time for B2B invoices (> 1000 SAR)
- **Reporting**: Batch for B2C invoices
- **Format**: UBL 2.1 XML with specific extensions

#### 2. SAMA Compliance (in `sama/`)
- `SamaAuditLogger` - Audit log for SAMA requirements
  - 7-year retention
  - Immutable event log
  - User action tracking
- `SamaReportGenerator` - Generate regulatory reports
  - Monthly NPL report
  - Quarterly risk report
  - Annual compliance report
- `SamaDataResidency` - Ensure data stays in KSA region

#### 3. VAT Calculation (in `vat/`)
- `VatCalculator` - 15% VAT on profit portion
  - `calculateVatOnProfit(Money profit)` → `Money vat`
  - `calculateTotalWithVat(Money amount)` → `Money total`
- `VatInvoice` - VAT-compliant invoice structure
- `VatExemptionChecker` - Check if transaction is VAT-exempt

Formula: VAT = Profit × 15%
(Principal is NOT subject to VAT in Islamic finance)

#### 4. Hijri Date Handling (in `date/`)
- `HijriCalendar` - Wrapper around `HijrahChronology`
  - `getCurrentHijriDate()` → `HijriDate`
  - `convertGregorianToHijri(LocalDate)` → `HijriDate`
  - `convertHijriToGregorian(HijriDate)` → `LocalDate`
  - `formatArabic(HijriDate)` → `String` (e.g., "15 رمضان 1445")
- `IslamicHolidayChecker` - Identify Islamic holidays
  - Eid al-Fitr, Eid al-Adha, Ramadan

#### 5. Configuration (in `config/`)
- `ZatcaConfig` - ZATCA API credentials, CSR keys
- `ComplianceConfig` - Compliance settings per tenant
- `LocalizationConfig` - Language, currency, timezone settings

#### 6. Exception Handling (in `exception/`)
- `ZatcaException` - ZATCA API errors
- `ComplianceViolationException` - Regulatory violations
- `VatCalculationException` - VAT calculation errors

---

## 🧪 Testing Requirements

- Mock ZATCA API using WireMock
- Test VAT calculations on various profit scenarios
- Test Hijri ↔ Gregorian conversions
- Test QR code generation (validate with ZATCA validator)
- Test invoice hash and signature generation

---

## ✅ Success Criteria

- [ ] E-invoices submit successfully to ZATCA
- [ ] QR codes validate with ZATCA validator
- [ ] VAT calculates correctly (15% on profit only)
- [ ] Hijri dates convert accurately
- [ ] SAMA audit logs capture all required events
- [ ] Data residency checks enforce KSA-only storage
- [ ] Tests pass: `mvn test -pl shared-libraries/compliance-localization-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/compliance-localization-sdk`

---

## 🔄 Next Step

After completing this SDK, proceed to:
- **Prompt 07**: `reporting-projection-sdk` implementation
