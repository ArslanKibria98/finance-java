package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.customer.domain.model.CustomerPepAnswer;
import com.ksa.financing.customer.domain.port.out.CustomerPepAnswerRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerPepAnswerJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CustomerPepAnswerRepositoryImpl implements CustomerPepAnswerRepository {

    private final JpaCustomerPepAnswerRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CustomerPepAnswer save(CustomerPepAnswer answer) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var entity = jpaRepository.findByCustomerIdAndTenantId(answer.customerId(), answer.tenantId())
                .orElseGet(() -> {
                    var fresh = new CustomerPepAnswerJpaEntity();
                    fresh.setTenantId(answer.tenantId());
                    fresh.setCustomerId(answer.customerId());
                    fresh.setCreatedAt(now);
                    return fresh;
                });

        entity.setIsPep(answer.isPep());
        entity.setPoliticalPosition(answer.politicalPosition());
        entity.setGovernmentBody(answer.governmentBody());
        entity.setCountryOfInfluence(answer.countryOfInfluence());
        entity.setPositionStartDate(answer.positionStartDate());
        entity.setPositionEndDate(answer.positionEndDate());
        entity.setPrimarySourceOfWealth(answer.primarySourceOfWealth());
        entity.setEstimatedNetWorth(answer.estimatedNetWorth());
        entity.setSourceOfWealthDescription(answer.sourceOfWealthDescription());
        entity.setSourceOfFunds(answer.sourceOfFunds());
        entity.setSourceOfFundsDetails(answer.sourceOfFundsDetails());
        entity.setOccupation(answer.occupation());
        entity.setOccupationDetails(answer.occupationDetails());
        entity.setRelatedPersonsJson(writeJson(answer.relatedPersons()));
        entity.setAdditionalNotes(answer.additionalNotes());
        entity.setSubmittedVia(answer.submittedVia() != null ? answer.submittedVia().name() : "POST_LOGIN");
        entity.setSubmittedAt(answer.submittedAt() != null
                ? OffsetDateTime.ofInstant(answer.submittedAt(), ZoneOffset.UTC) : now);
        entity.setSubmittedBy(answer.submittedBy());
        entity.setUpdatedAt(now);

        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<CustomerPepAnswer> findByCustomer(UUID tenantId, UUID customerId) {
        return jpaRepository.findByCustomerIdAndTenantId(customerId, tenantId).map(this::toDomain);
    }

    private CustomerPepAnswer toDomain(CustomerPepAnswerJpaEntity e) {
        return new CustomerPepAnswer(
                e.getId(),
                e.getTenantId(),
                e.getCustomerId(),
                Boolean.TRUE.equals(e.getIsPep()),
                e.getPoliticalPosition(),
                e.getGovernmentBody(),
                e.getCountryOfInfluence(),
                e.getPositionStartDate(),
                e.getPositionEndDate(),
                e.getPrimarySourceOfWealth(),
                e.getEstimatedNetWorth(),
                e.getSourceOfWealthDescription(),
                e.getSourceOfFunds(),
                e.getSourceOfFundsDetails(),
                e.getOccupation(),
                e.getOccupationDetails(),
                readJson(e.getRelatedPersonsJson()),
                e.getAdditionalNotes(),
                parseSubmittedVia(e.getSubmittedVia()),
                e.getSubmittedAt() != null ? e.getSubmittedAt().toInstant() : null,
                e.getSubmittedBy()
        );
    }

    private CustomerPepAnswer.SubmittedVia parseSubmittedVia(String raw) {
        if (raw == null) return CustomerPepAnswer.SubmittedVia.POST_LOGIN;
        try {
            return CustomerPepAnswer.SubmittedVia.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return CustomerPepAnswer.SubmittedVia.POST_LOGIN;
        }
    }

    private String writeJson(List<CustomerPepAnswer.RelatedPerson> persons) {
        if (persons == null || persons.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(persons);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize relatedPersons, storing null: {}", ex.getMessage());
            return null;
        }
    }

    private List<CustomerPepAnswer.RelatedPerson> readJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readerForListOf(CustomerPepAnswer.RelatedPerson.class).readValue(json);
        } catch (Exception ex) {
            log.warn("Failed to deserialize relatedPersons, returning empty list: {}", ex.getMessage());
            return List.of();
        }
    }
}
