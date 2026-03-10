package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.model.Partner;
import com.ksa.financing.product.domain.port.in.ManagePartnerUseCase;
import com.ksa.financing.product.domain.port.out.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagePartnerUseCaseImpl implements ManagePartnerUseCase {

    private final PartnerRepository partnerRepository;

    @Override
    @Transactional
    public Partner create(CreatePartnerCommand command) {
        if (partnerRepository.existsByCode(command.tenantId(), command.partnerCode())) {
            throw new BusinessException(
                    ErrorCodes.Product.PARTNER_DUPLICATE,
                    "Partner with code already exists: " + command.partnerCode(),
                    command.partnerCode());
        }

        var partner = Partner.create(command.tenantId(), command.partnerCode(),
                command.nameEn(), command.createdBy());
        partner.setNameAr(command.nameAr());
        partner.setEmail(command.email());
        partner.setPhone(command.phone());
        partner.setContactPerson(command.contactPerson());
        partner.setLogoUrl(command.logoUrl());

        var saved = partnerRepository.save(partner);
        log.info("Partner created: code={} id={} tenant={}", saved.getPartnerCode(), saved.getId(), saved.getTenantId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Partner getById(UUID tenantId, UUID partnerId) {
        return partnerRepository.findById(tenantId, partnerId)
                .orElseThrow(() -> NotFoundException.forEntity("Partner", partnerId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partner> listByTenant(UUID tenantId) {
        return partnerRepository.findAllByTenant(tenantId);
    }

    @Override
    @Transactional
    public Partner update(UUID tenantId, UUID partnerId, UpdatePartnerCommand command) {
        var partner = partnerRepository.findById(tenantId, partnerId)
                .orElseThrow(() -> NotFoundException.forEntity("Partner", partnerId.toString()));

        if (command.nameEn() != null) partner.setNameEn(command.nameEn());
        if (command.nameAr() != null) partner.setNameAr(command.nameAr());
        if (command.email() != null) partner.setEmail(command.email());
        if (command.phone() != null) partner.setPhone(command.phone());
        if (command.contactPerson() != null) partner.setContactPerson(command.contactPerson());
        if (command.logoUrl() != null) partner.setLogoUrl(command.logoUrl());
        partner.setUpdatedBy(command.updatedBy());
        partner.setUpdatedAt(Instant.now());

        var saved = partnerRepository.save(partner);
        log.info("Partner updated: id={} tenant={}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public void activate(UUID tenantId, UUID partnerId) {
        var partner = partnerRepository.findById(tenantId, partnerId)
                .orElseThrow(() -> NotFoundException.forEntity("Partner", partnerId.toString()));
        partner.activate();
        partner.setUpdatedAt(Instant.now());
        partnerRepository.save(partner);
        log.info("Partner activated: id={} tenant={}", partnerId, tenantId);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID partnerId) {
        var partner = partnerRepository.findById(tenantId, partnerId)
                .orElseThrow(() -> NotFoundException.forEntity("Partner", partnerId.toString()));
        partner.deactivate();
        partner.setUpdatedAt(Instant.now());
        partnerRepository.save(partner);
        log.info("Partner deactivated: id={} tenant={}", partnerId, tenantId);
    }

    @Override
    @Transactional
    public void suspend(UUID tenantId, UUID partnerId) {
        var partner = partnerRepository.findById(tenantId, partnerId)
                .orElseThrow(() -> NotFoundException.forEntity("Partner", partnerId.toString()));
        partner.suspend();
        partner.setUpdatedAt(Instant.now());
        partnerRepository.save(partner);
        log.info("Partner suspended: id={} tenant={}", partnerId, tenantId);
    }
}
