package com.demo.islamic.compliance.zatca;

import com.demo.islamic.compliance.exception.ZatcaException;
import com.demo.islamic.compliance.model.ZatcaInvoice;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates QR codes for ZATCA invoices
 * Implements TLV (Tag-Length-Value) encoding as per ZATCA specification
 */
@Slf4j
@Component
public class ZatcaQRCodeGenerator {

    private static final int QR_CODE_SIZE = 300;
    private static final DateTimeFormatter ZATCA_TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // ZATCA TLV Tags
    private static final byte TAG_SELLER_NAME = 0x01;
    private static final byte TAG_VAT_NUMBER = 0x02;
    private static final byte TAG_TIMESTAMP = 0x03;
    private static final byte TAG_TOTAL_WITH_VAT = 0x04;
    private static final byte TAG_VAT_AMOUNT = 0x05;
    private static final byte TAG_INVOICE_HASH = 0x06;
    private static final byte TAG_SIGNATURE = 0x07;
    private static final byte TAG_PUBLIC_KEY = 0x08;
    private static final byte TAG_CERTIFICATE_SIGNATURE = 0x09;

    /**
     * Generate QR code for ZATCA invoice
     * Returns Base64-encoded PNG image
     */
    public String generateQRCode(ZatcaInvoice invoice) {
        try {
            // Generate TLV data for QR code
            String tlvData = generateTLVData(invoice);

            // Generate QR code image
            byte[] qrCodeImage = generateQRCodeImage(tlvData);

            // Convert to Base64
            String base64QRCode = Base64.getEncoder().encodeToString(qrCodeImage);

            log.info("Generated QR code for invoice: invoiceNumber={}",
                invoice.getInvoiceNumber());

            return base64QRCode;
        } catch (Exception e) {
            throw new ZatcaException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate TLV (Tag-Length-Value) encoded data for QR code
     * This is the format required by ZATCA for invoice QR codes
     */
    private String generateTLVData(ZatcaInvoice invoice) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            // 1. Seller Name
            addTLVField(baos, TAG_SELLER_NAME,
                invoice.getSeller().getPartyName());

            // 2. VAT Registration Number
            addTLVField(baos, TAG_VAT_NUMBER,
                invoice.getSeller().getPartyTaxScheme().getCompanyId());

            // 3. Timestamp
            String timestamp = invoice.getIssueDate() + "T" + invoice.getIssueTime() + "Z";
            addTLVField(baos, TAG_TIMESTAMP, timestamp);

            // 4. Total Amount with VAT
            addTLVField(baos, TAG_TOTAL_WITH_VAT,
                formatAmount(invoice.getTotalAmount()));

            // 5. VAT Amount
            BigDecimal vatAmount = invoice.getTaxTotal() != null ?
                invoice.getTaxTotal().getTaxAmount() : BigDecimal.ZERO;
            addTLVField(baos, TAG_VAT_AMOUNT, formatAmount(vatAmount));

            // Phase 2 fields (for signed invoices)
            if (invoice.getInvoiceHash() != null) {
                // 6. Invoice Hash
                addTLVField(baos, TAG_INVOICE_HASH,
                    invoice.getInvoiceHash());

                // 7. Digital Signature
                if (invoice.getDigitalSignature() != null) {
                    addTLVField(baos, TAG_SIGNATURE,
                        invoice.getDigitalSignature());
                }

                // 8. Public Key (if available)
                if (invoice.getPublicKey() != null) {
                    addTLVField(baos, TAG_PUBLIC_KEY,
                        invoice.getPublicKey());
                }

                // 9. Certificate Signature (if available)
                if (invoice.getCertificateSignature() != null) {
                    addTLVField(baos, TAG_CERTIFICATE_SIGNATURE,
                        invoice.getCertificateSignature());
                }
            }

            // Convert to Base64
            byte[] tlvBytes = baos.toByteArray();
            String base64TLV = Base64.getEncoder().encodeToString(tlvBytes);

            log.debug("Generated TLV data for QR code: size={} bytes", tlvBytes.length);

            return base64TLV;
        } catch (IOException e) {
            throw new ZatcaException("Failed to generate TLV data", e);
        }
    }

    /**
     * Add a TLV field to the output stream
     */
    private void addTLVField(ByteArrayOutputStream baos, byte tag, String value)
        throws IOException {

        if (value == null || value.isEmpty()) {
            return;
        }

        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

        // Write tag
        baos.write(tag);

        // Write length
        baos.write((byte) valueBytes.length);

        // Write value
        baos.write(valueBytes);
    }

    /**
     * Format amount for QR code (2 decimal places)
     */
    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }

    /**
     * Generate QR code image from data
     */
    private byte[] generateQRCodeImage(String data) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        // Set QR code properties
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);

        // Generate QR code matrix
        BitMatrix bitMatrix = qrCodeWriter.encode(
            data,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE,
            hints
        );

        // Convert to PNG image
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        return outputStream.toByteArray();
    }

    /**
     * Validate QR code data according to ZATCA requirements
     */
    public boolean validateQRCode(String qrCodeData) {
        try {
            // Decode Base64
            byte[] tlvBytes = Base64.getDecoder().decode(qrCodeData);

            // Parse TLV structure
            ByteBuffer buffer = ByteBuffer.wrap(tlvBytes);

            boolean hasSellerName = false;
            boolean hasVatNumber = false;
            boolean hasTimestamp = false;
            boolean hasTotal = false;
            boolean hasVat = false;

            while (buffer.hasRemaining()) {
                byte tag = buffer.get();
                int length = buffer.get() & 0xFF;

                // Check if we have enough bytes for the value
                if (buffer.remaining() < length) {
                    log.error("Invalid TLV structure: insufficient bytes for value");
                    return false;
                }

                // Read value
                byte[] valueBytes = new byte[length];
                buffer.get(valueBytes);

                // Check required fields
                switch (tag) {
                    case TAG_SELLER_NAME:
                        hasSellerName = true;
                        break;
                    case TAG_VAT_NUMBER:
                        hasVatNumber = true;
                        break;
                    case TAG_TIMESTAMP:
                        hasTimestamp = true;
                        break;
                    case TAG_TOTAL_WITH_VAT:
                        hasTotal = true;
                        break;
                    case TAG_VAT_AMOUNT:
                        hasVat = true;
                        break;
                }
            }

            // Check if all required fields are present
            boolean isValid = hasSellerName && hasVatNumber && hasTimestamp
                && hasTotal && hasVat;

            log.info("QR code validation result: {}", isValid ? "VALID" : "INVALID");

            return isValid;
        } catch (Exception e) {
            log.error("Failed to validate QR code", e);
            return false;
        }
    }

    /**
     * Extract data from QR code for verification
     */
    public Map<String, String> extractQRCodeData(String qrCodeData) {
        Map<String, String> extractedData = new HashMap<>();

        try {
            byte[] tlvBytes = Base64.getDecoder().decode(qrCodeData);
            ByteBuffer buffer = ByteBuffer.wrap(tlvBytes);

            while (buffer.hasRemaining()) {
                byte tag = buffer.get();
                int length = buffer.get() & 0xFF;

                if (buffer.remaining() < length) {
                    break;
                }

                byte[] valueBytes = new byte[length];
                buffer.get(valueBytes);
                String value = new String(valueBytes, StandardCharsets.UTF_8);

                switch (tag) {
                    case TAG_SELLER_NAME:
                        extractedData.put("sellerName", value);
                        break;
                    case TAG_VAT_NUMBER:
                        extractedData.put("vatNumber", value);
                        break;
                    case TAG_TIMESTAMP:
                        extractedData.put("timestamp", value);
                        break;
                    case TAG_TOTAL_WITH_VAT:
                        extractedData.put("totalWithVat", value);
                        break;
                    case TAG_VAT_AMOUNT:
                        extractedData.put("vatAmount", value);
                        break;
                    case TAG_INVOICE_HASH:
                        extractedData.put("invoiceHash", value);
                        break;
                }
            }

            log.debug("Extracted {} fields from QR code", extractedData.size());

            return extractedData;
        } catch (Exception e) {
            log.error("Failed to extract QR code data", e);
            return extractedData;
        }
    }
}