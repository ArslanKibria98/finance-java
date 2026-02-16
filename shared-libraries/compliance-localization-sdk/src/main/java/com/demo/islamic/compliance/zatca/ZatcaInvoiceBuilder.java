package com.demo.islamic.compliance.zatca;

import com.demo.islamic.compliance.model.*;
import com.demo.islamic.compliance.vat.VatCalculator;
import com.demo.islamic.payment.model.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for creating ZATCA-compliant invoices
 * Ensures all required fields are present and properly formatted
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZatcaInvoiceBuilder {

    private static final String KSA_TIME_ZONE = "Asia/Riyadh";
    private static final DateTimeFormatter ZATCA_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final VatCalculator vatCalculator;

    /**
     * Build a ZATCA-compliant invoice
     */
    public ZatcaInvoice buildInvoice(InvoiceRequest request) {
        ZatcaInvoice invoice = new ZatcaInvoice();

        // Set invoice metadata
        invoice.setInvoiceUuid(UUID.randomUUID().toString());
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        invoice.setInvoiceType(request.getInvoiceType()); // Standard, Simplified, etc.
        invoice.setInvoiceTypeCode(getInvoiceTypeCode(request));
        invoice.setIssueDate(formatDate(LocalDateTime.now(ZoneId.of(KSA_TIME_ZONE))));
        invoice.setIssueTime(formatTime(LocalDateTime.now(ZoneId.of(KSA_TIME_ZONE))));

        // Set seller information (platform)
        invoice.setSeller(buildSellerInfo(request.getSeller()));

        // Set buyer information (customer)
        invoice.setBuyer(buildBuyerInfo(request.getBuyer()));

        // Build invoice lines with VAT calculation
        List<InvoiceLineItem> lineItems = buildLineItems(request.getLineItems());
        invoice.setLineItems(lineItems);

        // Calculate totals
        calculateInvoiceTotals(invoice);

        // Set payment information
        invoice.setPaymentMeans(request.getPaymentMeans());
        invoice.setPaymentTerms(request.getPaymentTerms());

        // Set additional ZATCA-required fields
        invoice.setPreviousInvoiceHash(request.getPreviousInvoiceHash());
        invoice.setInvoiceCounter(request.getInvoiceCounter());

        log.info("Built ZATCA invoice: invoiceNumber={}, type={}, totalAmount={}",
            invoice.getInvoiceNumber(), invoice.getInvoiceType(), invoice.getTotalAmount());

        return invoice;
    }

    /**
     * Build seller (platform) information
     */
    private PartyInfo buildSellerInfo(SellerInfo seller) {
        PartyInfo partyInfo = new PartyInfo();

        partyInfo.setPartyIdentification(seller.getCommercialRegistrationNumber());
        partyInfo.setPartyName(seller.getName());
        partyInfo.setPostalAddress(buildAddress(seller.getAddress()));
        partyInfo.setPartyTaxScheme(buildTaxScheme(seller.getVatNumber()));
        partyInfo.setPartyLegalEntity(buildLegalEntity(seller));

        return partyInfo;
    }

    /**
     * Build buyer (customer) information
     */
    private PartyInfo buildBuyerInfo(BuyerInfo buyer) {
        PartyInfo partyInfo = new PartyInfo();

        partyInfo.setPartyIdentification(buyer.getNationalId());
        partyInfo.setPartyName(buyer.getName());
        partyInfo.setPostalAddress(buildAddress(buyer.getAddress()));

        // Only set VAT info if buyer is VAT registered (e.g., SME)
        if (buyer.getVatNumber() != null) {
            partyInfo.setPartyTaxScheme(buildTaxScheme(buyer.getVatNumber()));
        }

        return partyInfo;
    }

    /**
     * Build address information
     */
    private AddressInfo buildAddress(Address address) {
        AddressInfo addressInfo = new AddressInfo();

        addressInfo.setStreetName(address.getStreetName());
        addressInfo.setBuildingNumber(address.getBuildingNumber());
        addressInfo.setPlotIdentification(address.getPlotIdentification());
        addressInfo.setCitySubdivisionName(address.getDistrict());
        addressInfo.setCityName(address.getCity());
        addressInfo.setPostalZone(address.getPostalCode());
        addressInfo.setCountrySubentity(address.getRegion());

        // Country info
        CountryInfo country = new CountryInfo();
        country.setIdentificationCode("SA"); // Saudi Arabia
        addressInfo.setCountry(country);

        return addressInfo;
    }

    /**
     * Build tax scheme information
     */
    private TaxScheme buildTaxScheme(String vatNumber) {
        TaxScheme taxScheme = new TaxScheme();

        taxScheme.setCompanyId(vatNumber);
        taxScheme.setTaxSchemeId("VAT");

        return taxScheme;
    }

    /**
     * Build legal entity information
     */
    private LegalEntity buildLegalEntity(SellerInfo seller) {
        LegalEntity legalEntity = new LegalEntity();

        legalEntity.setRegistrationName(seller.getLegalName());

        return legalEntity;
    }

    /**
     * Build invoice line items with VAT calculation
     */
    private List<InvoiceLineItem> buildLineItems(List<LineItemRequest> items) {
        List<InvoiceLineItem> lineItems = new ArrayList<>();

        for (LineItemRequest itemRequest : items) {
            InvoiceLineItem lineItem = new InvoiceLineItem();

            lineItem.setId(String.valueOf(lineItems.size() + 1));
            lineItem.setDescription(itemRequest.getDescription());
            lineItem.setQuantity(itemRequest.getQuantity());
            lineItem.setUnitCode(itemRequest.getUnitCode());
            lineItem.setUnitPrice(itemRequest.getUnitPrice());

            // Calculate line total
            BigDecimal lineExtensionAmount = itemRequest.getUnitPrice()
                .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            lineItem.setLineExtensionAmount(lineExtensionAmount);

            // Calculate VAT for the line item
            if (itemRequest.isVatable()) {
                Money lineAmount = Money.of(lineExtensionAmount, "SAR");
                Money vatAmount = vatCalculator.calculateVat(lineAmount);

                lineItem.setTaxAmount(vatAmount.getAmount());
                lineItem.setTaxPercent(vatCalculator.getVatRate().multiply(BigDecimal.valueOf(100)));
                lineItem.setTaxCategory("S"); // Standard rate
            } else {
                lineItem.setTaxAmount(BigDecimal.ZERO);
                lineItem.setTaxPercent(BigDecimal.ZERO);
                lineItem.setTaxCategory("E"); // Exempt
                lineItem.setTaxExemptionReason(itemRequest.getExemptionReason());
            }

            // Calculate gross amount (including VAT)
            BigDecimal grossAmount = lineExtensionAmount.add(lineItem.getTaxAmount());
            lineItem.setGrossAmount(grossAmount);

            lineItems.add(lineItem);
        }

        return lineItems;
    }

    /**
     * Calculate invoice totals from line items
     */
    private void calculateInvoiceTotals(ZatcaInvoice invoice) {
        BigDecimal totalExcludingVat = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalIncludingVat = BigDecimal.ZERO;

        for (InvoiceLineItem item : invoice.getLineItems()) {
            totalExcludingVat = totalExcludingVat.add(item.getLineExtensionAmount());
            totalVat = totalVat.add(item.getTaxAmount());
            totalIncludingVat = totalIncludingVat.add(item.getGrossAmount());
        }

        // Set allowances/charges if any
        BigDecimal allowanceTotal = invoice.getAllowanceTotal() != null ?
            invoice.getAllowanceTotal() : BigDecimal.ZERO;
        BigDecimal chargeTotal = invoice.getChargeTotal() != null ?
            invoice.getChargeTotal() : BigDecimal.ZERO;

        // Calculate final amounts
        BigDecimal taxExclusiveAmount = totalExcludingVat
            .subtract(allowanceTotal)
            .add(chargeTotal);

        BigDecimal payableAmount = taxExclusiveAmount.add(totalVat);

        // Set invoice totals
        invoice.setLineExtensionAmount(totalExcludingVat);
        invoice.setTaxExclusiveAmount(taxExclusiveAmount);
        invoice.setTaxInclusiveAmount(payableAmount);
        invoice.setAllowanceTotal(allowanceTotal);
        invoice.setChargeTotal(chargeTotal);
        invoice.setPayableAmount(payableAmount);
        invoice.setTotalAmount(payableAmount);

        // Set tax totals
        TaxTotal taxTotal = new TaxTotal();
        taxTotal.setTaxAmount(totalVat);

        // Add tax subtotals by category
        List<TaxSubtotal> taxSubtotals = calculateTaxSubtotals(invoice.getLineItems());
        taxTotal.setTaxSubtotals(taxSubtotals);

        invoice.setTaxTotal(taxTotal);
    }

    /**
     * Calculate tax subtotals by category
     */
    private List<TaxSubtotal> calculateTaxSubtotals(List<InvoiceLineItem> lineItems) {
        List<TaxSubtotal> subtotals = new ArrayList<>();

        // Group by tax category
        BigDecimal standardRateBase = BigDecimal.ZERO;
        BigDecimal standardRateTax = BigDecimal.ZERO;
        BigDecimal exemptBase = BigDecimal.ZERO;

        for (InvoiceLineItem item : lineItems) {
            if ("S".equals(item.getTaxCategory())) {
                standardRateBase = standardRateBase.add(item.getLineExtensionAmount());
                standardRateTax = standardRateTax.add(item.getTaxAmount());
            } else if ("E".equals(item.getTaxCategory())) {
                exemptBase = exemptBase.add(item.getLineExtensionAmount());
            }
        }

        // Add standard rate subtotal if applicable
        if (standardRateBase.compareTo(BigDecimal.ZERO) > 0) {
            TaxSubtotal standardSubtotal = new TaxSubtotal();
            standardSubtotal.setTaxableAmount(standardRateBase);
            standardSubtotal.setTaxAmount(standardRateTax);
            standardSubtotal.setTaxCategory("S");
            standardSubtotal.setTaxPercent(vatCalculator.getVatRate().multiply(BigDecimal.valueOf(100)));
            subtotals.add(standardSubtotal);
        }

        // Add exempt subtotal if applicable
        if (exemptBase.compareTo(BigDecimal.ZERO) > 0) {
            TaxSubtotal exemptSubtotal = new TaxSubtotal();
            exemptSubtotal.setTaxableAmount(exemptBase);
            exemptSubtotal.setTaxAmount(BigDecimal.ZERO);
            exemptSubtotal.setTaxCategory("E");
            exemptSubtotal.setTaxPercent(BigDecimal.ZERO);
            subtotals.add(exemptSubtotal);
        }

        return subtotals;
    }

    /**
     * Get invoice type code based on invoice type
     */
    private String getInvoiceTypeCode(InvoiceRequest request) {
        // ZATCA invoice type codes
        // 388 - Tax Invoice
        // 381 - Credit Note
        // 383 - Debit Note
        // 386 - Prepayment Invoice

        switch (request.getInvoiceCategory()) {
            case "STANDARD":
                return "388";
            case "CREDIT_NOTE":
                return "381";
            case "DEBIT_NOTE":
                return "383";
            case "PREPAYMENT":
                return "386";
            default:
                return "388";
        }
    }

    /**
     * Format date for ZATCA
     */
    private String formatDate(LocalDateTime dateTime) {
        return dateTime.toLocalDate().toString();
    }

    /**
     * Format time for ZATCA
     */
    private String formatTime(LocalDateTime dateTime) {
        return dateTime.toLocalTime().toString();
    }
}