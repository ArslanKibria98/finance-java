package com.ksa.financing.service.template.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for ExampleAggregate.
 * This is the persistence model, separate from the domain model.
 *
 * Key features:
 * - Multi-tenancy via tenant_id column
 * - Audit fields (created/updated timestamps)
 * - Optimistic locking via @Version
 * - Soft delete support
 */
@Entity
@Table(
    name = "example_aggregate",
    indexes = {
        @Index(name = "idx_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_tenant_status", columnList = "tenant_id, status")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_aggregate_id_tenant",
            columnNames = {"aggregate_id", "tenant_id"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
// NOTE: Filename should be ExampleAggregateJpaEntity.java - renamed class per naming conventions
public class ExampleAggregateJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "example_aggregate_seq")
    @SequenceGenerator(name = "example_aggregate_seq", sequenceName = "example_aggregate_seq", allocationSize = 1)
    private Long id;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    @UpdateTimestamp
    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @OneToMany(
        mappedBy = "aggregate",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<ExampleEntityJpaEntity> entities = new ArrayList<>();

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Helper method to maintain bidirectional relationship.
     */
    public void addEntity(ExampleEntityJpaEntity entity) {
        entities.add(entity);
        entity.setAggregate(this);
    }

    /**
     * Helper method to maintain bidirectional relationship.
     */
    public void removeEntity(ExampleEntityJpaEntity entity) {
        entities.remove(entity);
        entity.setAggregate(null);
    }
}