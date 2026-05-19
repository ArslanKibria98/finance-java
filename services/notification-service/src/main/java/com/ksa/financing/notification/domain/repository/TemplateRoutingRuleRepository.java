package com.ksa.financing.notification.domain.repository;

import com.ksa.financing.notification.domain.model.TemplateRoutingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateRoutingRuleRepository extends JpaRepository<TemplateRoutingRule, UUID> {
    List<TemplateRoutingRule> findByTenantIdAndEventTypeAndActiveTrue(UUID tenantId, String eventType);
}
