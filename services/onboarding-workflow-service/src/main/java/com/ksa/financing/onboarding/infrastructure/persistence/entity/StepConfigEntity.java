package com.ksa.financing.onboarding.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "step_configs")
@Data
public class StepConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "api_method")
    private String apiMethod = "POST";

    @Column(name = "success_condition_jsonpath")
    private String successConditionJsonPath;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "api_headers")
    private String apiHeaders;

    @Column(name = "api_params_mapping")
    private String apiParamsMapping;

    @Column(name = "api_body_mapping")
    private String apiBodyMapping;

    @OneToMany(mappedBy = "step", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("orderIndex ASC")
    private java.util.List<FieldConfigEntity> fields = new java.util.ArrayList<>();
}
