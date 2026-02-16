package com.ksa.financing.service.template.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ksa.financing.service.template.domain.model.ExampleAggregate;
import com.ksa.financing.service.template.domain.model.ExampleStatus;

import java.util.List;

/**
 * Domain Service: Contains business logic that doesn't naturally fit within a single aggregate.
 * Domain services orchestrate operations across multiple aggregates or contain complex calculations.
 */
@Slf4j
@Service
public class ExampleDomainService {

    /**
     * Example business logic: Validate that an aggregate can be activated
     * based on complex business rules.
     */
    public boolean canActivate(ExampleAggregate aggregate) {
        // Complex business rule example
        if (aggregate.getStatus() != ExampleStatus.DRAFT) {
            log.debug("Cannot activate: Status is not DRAFT");
            return false;
        }

        if (aggregate.getDescription() == null || aggregate.getDescription().length() < 10) {
            log.debug("Cannot activate: Description too short");
            return false;
        }

        // Could check external rules, other aggregates, etc.
        return true;
    }

    /**
     * Example calculation: Calculate a business metric
     */
    public double calculateCompletionScore(ExampleAggregate aggregate) {
        double score = 0.0;

        // Base score for having a name
        if (aggregate.getName() != null && !aggregate.getName().isEmpty()) {
            score += 20.0;
        }

        // Score for description length
        if (aggregate.getDescription() != null) {
            int descLength = aggregate.getDescription().length();
            score += Math.min(30.0, descLength * 0.5);
        }

        // Score for entities
        int entityCount = aggregate.getEntities().size();
        score += Math.min(30.0, entityCount * 5.0);

        // Score for status progression
        score += switch (aggregate.getStatus()) {
            case DRAFT -> 0.0;
            case ACTIVE -> 10.0;
            case COMPLETED -> 20.0;
            default -> 0.0;
        };

        return Math.min(100.0, score);
    }

    /**
     * Example orchestration: Merge multiple aggregates
     * This would coordinate between multiple aggregates and return a result.
     */
    public ExampleAggregate mergeAggregates(
            ExampleAggregate primary,
            List<ExampleAggregate> secondary) {

        // Validate merge is possible
        for (ExampleAggregate agg : secondary) {
            if (agg.getStatus() != ExampleStatus.DRAFT) {
                throw new IllegalStateException(
                    "Can only merge DRAFT aggregates"
                );
            }
        }

        // Business logic to merge entities from secondary into primary
        // This is just an example - actual implementation would be more complex
        log.info("Merging {} aggregates into primary {}",
            secondary.size(), primary.getId());

        return primary;
    }
}