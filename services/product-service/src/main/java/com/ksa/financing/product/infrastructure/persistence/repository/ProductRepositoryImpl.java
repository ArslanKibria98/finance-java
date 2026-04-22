package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.*;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import com.ksa.financing.product.infrastructure.persistence.mapper.CountryPersistenceMapper;
import com.ksa.financing.product.infrastructure.persistence.mapper.ProductPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure implementation of the {@link ProductRepository} output port.
 * Delegates to JPA repositories and uses {@link ProductPersistenceMapper}
 * for domain/JPA entity translation.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {

    private final JpaProductRepository jpaProductRepository;
    private final JpaAdminFeeSlabRepository jpaAdminFeeSlabRepository;
    private final JpaTermsConditionsRepository jpaTermsConditionsRepository;
    private final JpaFeeSettingsRepository jpaFeeSettingsRepository;
    private final JpaProductDurationSettingsRepository jpaProductDurationSettingsRepository;
    private final JpaApplicationStepRepository jpaApplicationStepRepository;
private final JpaProductEnvironmentConfigRepository jpaProductEnvironmentConfigRepository;
    private final JpaApprovalWorkflowRepository jpaApprovalWorkflowRepository;
    private final JpaApprovalConditionRepository jpaApprovalConditionRepository;
    private final JpaApprovalActionRepository jpaApprovalActionRepository;
    private final JpaCountryRepository jpaCountryRepository;
    private final JpaMasterCategoryRepository jpaMasterCategoryRepository;
    private final JpaSubCategoryRepository jpaSubCategoryRepository;
    private final ProductDocumentRepositoryImpl productDocumentRepository;

    @Override
    public Product save(Product product) {
        log.debug("Persisting product: code={}, tenantId={}", product.getProductCode(), product.getTenantId());

        var jpaEntity = ProductPersistenceMapper.toEntity(product);
        var saved = jpaProductRepository.save(jpaEntity);

        log.debug("Product persisted successfully: id={}", saved.getId());
        return ProductPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID tenantId, UUID id) {
        log.debug("Finding product by id={}, tenantId={}", id, tenantId);

        return jpaProductRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .map(entity -> {
                    var product = ProductPersistenceMapper.toDomain(entity);
                    loadSettings(product, tenantId, id);
                    return product;
                });
    }

    @Override
    public Optional<Product> findById(UUID id) {
        log.debug("Finding product by id={} (cross-tenant)", id);

        return jpaProductRepository.findByIdAndDeletedAtIsNull(id)
                .map(entity -> {
                    var product = ProductPersistenceMapper.toDomain(entity);
                    loadSettings(product, entity.getTenantId(), id);
                    return product;
                });
    }

    @Override
    public Optional<Product> findByProductCode(UUID tenantId, String productCode) {
        log.debug("Finding product by code={}, tenantId={}", productCode, tenantId);

        return jpaProductRepository.findByProductCodeAndTenantIdAndDeletedAtIsNull(productCode, tenantId)
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public List<Product> findAllByTenant(UUID tenantId) {
        log.debug("Listing all products for tenantId={}", tenantId);

        var products = jpaProductRepository.findAllByTenantIdAndDeletedAtIsNull(tenantId)
                .stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();

        // Enrich with country, category names, and admin fee slabs for listing
        products.forEach(product -> {
            if (product.getCountryId() != null) {
                jpaCountryRepository.findById(product.getCountryId())
                        .map(CountryPersistenceMapper::toDomain)
                        .ifPresent(product::setCountry);
            }
            if (product.getMasterCategoryId() != null) {
                jpaMasterCategoryRepository.findById(product.getMasterCategoryId())
                        .ifPresent(cat -> {
                            product.setMasterCategoryNameEn(cat.getNameEn());
                            product.setMasterCategoryNameAr(cat.getNameAr());
                        });
            }
            if (product.getSubCategoryId() != null) {
                jpaSubCategoryRepository.findById(product.getSubCategoryId())
                        .ifPresent(sub -> {
                            product.setSubCategoryNameEn(sub.getNameEn());
                            product.setSubCategoryNameAr(sub.getNameAr());
                        });
            }
            // Load admin fee slabs for deriving min/max financing and tenure
            var slabs = jpaAdminFeeSlabRepository
                    .findByProductIdAndTenantIdOrderBySortOrder(product.getId(), product.getTenantId())
                    .stream()
                    .map(s -> new AdminFeeSlab(s.getId(), s.getMinAmount(), s.getMaxAmount(),
                            s.getProfitPercentage(), s.getProcessingFee(), s.getAdminFee(),
                            s.getPartnerScope(), s.getStatus(), s.getSortOrder(),
                            s.getMinTenure(), s.getMaxTenure()))
                    .toList();
            product.setAdminFeeSlabs(slabs);
        });

        return products;
    }

    @Override
    public boolean existsByProductCode(UUID tenantId, String productCode) {
        return jpaProductRepository.existsByProductCodeAndTenantIdAndDeletedAtIsNull(productCode, tenantId);
    }

    private void loadSettings(Product product, UUID tenantId, UUID productId) {
        // Admin fee slabs
        var slabs = jpaAdminFeeSlabRepository
                .findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId)
                .stream()
                .map(s -> new AdminFeeSlab(
                        s.getId(), s.getMinAmount(), s.getMaxAmount(),
                        s.getProfitPercentage(), s.getProcessingFee(),
                        s.getAdminFee(), s.getPartnerScope(), s.getStatus(),
                        s.getSortOrder(), s.getMinTenure(), s.getMaxTenure()))
                .toList();
        product.setAdminFeeSlabs(slabs);

        // Terms & Conditions
        jpaTermsConditionsRepository.findByProductIdAndTenantId(productId, tenantId)
                .ifPresent(tc -> product.setTermsConditions(
                        new TermsConditions(tc.getId(), tc.getTermsEn(), tc.getTermsAr())));

        // Fee Settings
        jpaFeeSettingsRepository.findByProductIdAndTenantId(productId, tenantId)
                .ifPresent(fs -> product.setFeeSettings(new FeeSettings(
                        fs.getId(), fs.getRevenueEligibilityThreshold(),
                        fs.getMaxDbrPercentage(),
                        fs.getDbrCalculationMethod(), fs.getDbrExceptions(),
                        fs.getMaxDti(), fs.getMinAge(), fs.getMaxAge(),
                        fs.getGdbrPercentage())));

        // Duration Settings
        jpaProductDurationSettingsRepository.findByProductIdAndTenantId(productId, tenantId)
                .ifPresent(ds -> product.setDurationSettings(new DurationSettings(
                        ds.getId(),
                        ds.getRequestDurationDays(),
                        ds.getApprovalDurationDays(),
                        ds.getDisbursementDurationHours(),
                        ds.getRepaymentDurationDays())));

        // Application Steps
        var steps = jpaApplicationStepRepository
                .findByProductIdAndTenantIdOrderBySortOrder(productId, tenantId)
                .stream()
                .map(s -> new ApplicationStep(
                        s.getId(), s.getStepNumber(), s.getTitleEn(), s.getTitleAr(),
                        s.getDescription(), s.isRequired(), s.getSortOrder()))
                .toList();
        product.setApplicationSteps(steps);

        // Environment Configs
        var envConfigs = jpaProductEnvironmentConfigRepository
                .findByProductIdAndTenantId(productId, tenantId)
                .stream()
                .map(ec -> new EnvironmentConfigLink(
                        ec.getId(), ec.getEnvironmentConfigId(), ec.isActive(), ec.getSortOrder()))
                .toList();
        product.setEnvironmentConfigs(envConfigs);

        // Approval Workflows (with conditions and actions)
        var workflows = jpaApprovalWorkflowRepository
                .findByProductIdAndTenantIdOrderByPriority(productId, tenantId)
                .stream()
                .map(wf -> {
                    var conditions = jpaApprovalConditionRepository
                            .findByWorkflowIdOrderBySortOrder(wf.getId())
                            .stream()
                            .map(c -> new ApprovalWorkflow.ApprovalCondition(
                                    c.getId(), c.getField(), c.getOperator(),
                                    c.getValue(), c.getSortOrder()))
                            .toList();

                    var actions = jpaApprovalActionRepository
                            .findByWorkflowIdOrderBySortOrder(wf.getId())
                            .stream()
                            .map(a -> new ApprovalWorkflow.ApprovalAction(
                                    a.getId(), a.getActionType(), a.getConfiguration(),
                                    a.getSortOrder()))
                            .toList();

                    return new ApprovalWorkflow(
                            wf.getId(), wf.getWorkflowType(), wf.getNameEn(), wf.getNameAr(),
                            wf.getDescription(), wf.getTemplateSource(), wf.isActive(),
                            wf.getPriority(), conditions, actions);
                })
                .toList();
        product.setApprovalWorkflows(workflows);

        // Documents
        product.setDocuments(productDocumentRepository.findByProductId(tenantId, productId));

        // Country (if countryId is set)
        if (product.getCountryId() != null) {
            jpaCountryRepository.findById(product.getCountryId())
                    .map(CountryPersistenceMapper::toDomain)
                    .ifPresent(product::setCountry);
        }
    }
}
