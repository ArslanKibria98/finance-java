package com.ksa.financing.onboarding.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "step_submissions")
@Data
public class StepSubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private String sessionId; // GlobalUID

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false)
    private Map<String, Object> rawData; // Merged user input + API response

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
