package com.ksa.financing.document.application.usecase;

import com.ksa.financing.document.domain.model.Document;
import com.ksa.financing.document.domain.model.DocumentStatus;
import com.ksa.financing.document.domain.port.in.StoreDocumentUseCase;
import com.ksa.financing.document.domain.port.out.DocumentCryptoPort;
import com.ksa.financing.document.domain.port.out.DocumentRepository;
import com.ksa.financing.storage.model.FileCategory;
import com.ksa.financing.storage.model.FileUploadRequest;
import com.ksa.financing.storage.model.FileUploadResult;
import com.ksa.financing.storage.port.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreDocumentService implements StoreDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentCryptoPort crypto;
    private final FileStoragePort fileStorage;

    @Override
    @Transactional
    public Document store(StoreDocumentCommand command) {
        if (command.plainBytes() == null || command.plainBytes().length == 0) {
            throw new IllegalArgumentException("Document bytes must not be empty");
        }

        // Idempotency: replay safe
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            var existing = documentRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent store hit — returning documentId={}", existing.get().getId());
                return existing.get();
            }
        }

        // 1. Encrypt
        DocumentCryptoPort.EnvelopeResult envelope = crypto.encrypt(command.plainBytes());
        String sha256 = crypto.sha256Hex(command.plainBytes());

        // 2. Upload ciphertext to MinIO
        String filename = command.kind().name().toLowerCase() + ".bin";
        FileUploadResult uploadResult = fileStorage.upload(new FileUploadRequest(
                command.tenantId(),
                command.customerId() != null ? command.customerId() : command.tenantId(),
                FileCategory.KYC_DOCUMENT,
                filename,
                "application/octet-stream",
                new ByteArrayInputStream(envelope.ciphertext()),
                envelope.ciphertext().length));

        // 3. Persist metadata
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setTenantId(command.tenantId());
        doc.setCustomerId(command.customerId());
        doc.setWorkflowId(command.workflowId());
        doc.setKind(command.kind());
        doc.setSourceFlow(command.sourceFlow());
        doc.setDocumentNumber(command.documentNumber());
        doc.setFaciaReferenceId(command.faciaReferenceId());
        doc.setObjectKey(uploadResult.objectKey());
        doc.setContentType(command.contentType() != null ? command.contentType() : "image/jpeg");
        doc.setSizeBytesPlain(command.plainBytes().length);
        doc.setSizeBytesStored(envelope.ciphertext().length);
        doc.setSha256Plain(sha256);
        doc.setCipherAlgo(envelope.algo());
        doc.setEncryptedKey(envelope.encryptedKey());
        doc.setIv(envelope.iv());
        doc.setStatus(DocumentStatus.ACTIVE);
        doc.setIdempotencyKey(command.idempotencyKey());
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        doc.setCreatedBy(command.createdBy());
        doc.setVersion(1);

        Document saved = documentRepository.save(doc);
        log.info("Document stored: id={} kind={} customer={} flow={} sizePlain={}B objectKey={}",
                saved.getId(), saved.getKind(), saved.getCustomerId(),
                saved.getSourceFlow(), saved.getSizeBytesPlain(), saved.getObjectKey());
        return saved;
    }
}
