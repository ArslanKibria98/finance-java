package com.ksa.financing.customer.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_pep_answers")
@Getter
@Setter
public class CustomerPepAnswerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    @Column(name = "is_pep", nullable = false)
    private Boolean isPep;

    @Column(name = "political_position", length = 100)
    private String politicalPosition;

    @Column(name = "government_body", length = 200)
    private String governmentBody;

    @Column(name = "country_of_influence", length = 10)
    private String countryOfInfluence;

    @Column(name = "position_start_date", length = 20)
    private String positionStartDate;

    @Column(name = "position_end_date", length = 20)
    private String positionEndDate;

    @Column(name = "primary_source_of_wealth", length = 50)
    private String primarySourceOfWealth;

    @Column(name = "estimated_net_worth", length = 50)
    private String estimatedNetWorth;

    @Column(name = "source_of_wealth_description", columnDefinition = "TEXT")
    private String sourceOfWealthDescription;

    @Column(name = "source_of_funds", length = 50)
    private String sourceOfFunds;

    @Column(name = "source_of_funds_details", columnDefinition = "TEXT")
    private String sourceOfFundsDetails;

    @Column(name = "occupation", length = 50)
    private String occupation;

    @Column(name = "occupation_details", columnDefinition = "TEXT")
    private String occupationDetails;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_persons", columnDefinition = "jsonb")
    private String relatedPersonsJson;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "submitted_via", nullable = false, length = 20)
    private String submittedVia;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
