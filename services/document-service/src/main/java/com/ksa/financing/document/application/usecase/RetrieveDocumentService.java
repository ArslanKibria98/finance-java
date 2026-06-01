package com.ksa.financing.document.application.usecase;

import com.ksa.financing.document.domain.model.Document;
import com.ksa.financing.document.domain.port.in.RetrieveDocumentUseCase;
import com.ksa.financing.document.domain.port.out.DocumentCryptoPort;
import com.ksa.financing.document.domain.port.out.DocumentRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.storage.port.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieveDocumentService implements RetrieveDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final DocumentCryptoPort crypto;
    private final FileStoragePort fileStorage;

    @Override
    @Transactional(readOnly = true)
    public RetrievedDocument retrieve(UUID tenantId, UUID documentId) {
        Document doc = documentRepository.findById(tenantId, documentId)
                .orElseThrow(() -> NotFoundException.forEntity("Document", documentId.toString()));

        byte[] ciphertext;
        try (InputStream in = fileStorage.download(doc.getObjectKey())) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            in.transferTo(buf);
            ciphertext = buf.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download document from storage: " + e.getMessage(), e);
        }

        byte[] plain = crypto.decrypt(ciphertext, doc.getEncryptedKey(), doc.getIv());
        log.info("Document retrieved: id={} sizePlain={}B", doc.getId(), plain.length);
        return new RetrievedDocument(doc, plain);
    }
}
