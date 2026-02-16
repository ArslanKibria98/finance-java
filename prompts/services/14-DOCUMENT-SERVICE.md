# 📄 Prompt 14: Document Service

**Objective**: Implement the Document Service for document management, storage, and OCR.

**Prerequisites**: ✅ SDK 01-08 + Template + Services 01-13

---

## 📚 Reference Documents

- `/var/www/docs/islamic-financing/master-blueprint/04_MICROSERVICE_DECOMPOSITION.md`
- `/var/www/docs/islamic-financing/master-blueprint/10_KYC_CREDIT_DECISIONING.md` - Document verification
- `/var/www/docs/islamic-financing/erd-docs/document-service.sql`

---

## 🎯 Implementation Requirements

### Responsibilities
- Document upload (ID card, salary slip, bank statement)
- OCR (extract data from images)
- Document classification
- Version control
- Secure storage (S3-compatible)

### Database Schema
- `documents`, `document_versions`, `ocr_results`

### Document Types
- NATIONAL_ID, SALARY_SLIP, BANK_STATEMENT, ADDRESS_PROOF, CONTRACT, INVOICE

### REST API & gRPC
Design APIs for:
- Document upload and download
- OCR processing
- Document classification
- Version control
- Document queries by entity

**Note**: Design based on document types and KYC requirements.

### OCR Integration
- Extract: Name, National ID, Expiry Date from ID card
- Extract: Salary, Bank name from salary slip
- Use: Tesseract or AWS Textract

### Storage
- S3-compatible object storage (MinIO for local dev)
- Bucket: `ksa-financing-documents-{env}`
- Encryption: Server-side AES-256

### Integration
- Publishes: `DocumentUploaded`, `OcrCompleted`, `DocumentVerified`

---

## ✅ Success Criteria
- [ ] Document upload/download working
- [ ] OCR extracts data accurately (>90% accuracy)
- [ ] Documents encrypted at rest
- [ ] Version control working
- [ ] S3 integration successful

---

## 🔄 Next: **Prompt 15** - Partner Service
