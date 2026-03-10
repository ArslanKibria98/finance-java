package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.port.in.ManageProductDocumentsUseCase;
import com.ksa.financing.product.domain.port.out.ProductDocumentRepository;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageProductDocumentsUseCaseImpl implements ManageProductDocumentsUseCase {

    private final ProductRepository productRepository;
    private final ProductDocumentRepository documentRepository;

    @Override
    @Transactional
    public void addDocument(UUID tenantId, UUID productId, AddDocumentCommand command) {
        log.info("Adding document '{}' to product {}", command.nameEn(), productId);

        productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        documentRepository.saveDocument(
            tenantId, productId,
            command.nameEn(), command.nameAr(),
            command.documentType(), command.fileUrl(),
            command.fileSizeBytes(), command.fileVersion(),
            command.createdByName(), command.required()
        );

        // Advance wizard to step 5 if not already there
        productRepository.findById(tenantId, productId).ifPresent(product -> {
            if (product.getWizardStep() < 5) {
                product.advanceWizardStep(5);
                productRepository.save(product);
            }
        });
    }

    @Override
    @Transactional
    public void removeDocument(UUID tenantId, UUID productId, UUID documentId) {
        log.info("Removing document {} from product {}", documentId, productId);

        productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        if (!documentRepository.existsDocument(tenantId, productId, documentId)) {
            throw new NotFoundException(
                ErrorCodes.Product.DOCUMENT_NOT_FOUND,
                "Product document not found: " + documentId,
                documentId.toString());
        }

        documentRepository.deleteDocument(tenantId, productId, documentId);
    }
}
