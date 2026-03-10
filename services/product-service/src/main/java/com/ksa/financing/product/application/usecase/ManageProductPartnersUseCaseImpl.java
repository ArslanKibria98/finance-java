package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.port.in.ManageProductPartnersUseCase;
import com.ksa.financing.product.domain.port.out.ProductPartnerRepository;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageProductPartnersUseCaseImpl implements ManageProductPartnersUseCase {

    private final ProductRepository productRepository;
    private final ProductPartnerRepository partnerRepository;

    @Override
    @Transactional
    public void addPartnerAffiliation(UUID tenantId, UUID productId, AddPartnerAffiliationCommand command) {
        log.info("Adding partner {} to product {}", command.partnerId(), productId);

        productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        if (partnerRepository.existsAffiliation(productId, command.partnerId())) {
            throw new BusinessException(
                ErrorCodes.Product.PARTNER_DUPLICATE,
                "Partner is already affiliated with this product: " + command.partnerId(),
                command.partnerId().toString());
        }

        partnerRepository.saveAffiliation(
            tenantId, productId, command.partnerId(),
            command.affiliationType(), command.commissionPercentage()
        );

        // Advance wizard to step 4 if not already past it
        productRepository.findById(tenantId, productId).ifPresent(product -> {
            if (product.getWizardStep() < 4) {
                product.advanceWizardStep(4);
                productRepository.save(product);
            }
        });
    }

    @Override
    @Transactional
    public void removePartnerAffiliation(UUID tenantId, UUID productId, UUID partnerId) {
        log.info("Removing partner {} from product {}", partnerId, productId);

        productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));

        if (!partnerRepository.existsAffiliation(productId, partnerId)) {
            throw new NotFoundException(
                ErrorCodes.Product.PARTNER_NOT_FOUND,
                "Partner affiliation not found: " + partnerId,
                partnerId.toString());
        }

        partnerRepository.deleteAffiliation(productId, partnerId);
    }
}
