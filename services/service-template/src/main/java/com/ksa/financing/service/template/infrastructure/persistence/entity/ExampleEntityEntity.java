package com.ksa.financing.service.template.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JPA Entity for ExampleEntity (child entity within aggregate).
 */
@Entity
@Table(name = "example_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
// NOTE: Filename should be ExampleEntityJpaEntity.java - renamed class per naming conventions
public class ExampleEntityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "example_entity_seq")
    @SequenceGenerator(name = "example_entity_seq", sequenceName = "example_entity_seq", allocationSize = 1)
    private Long id;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aggregate_id", nullable = false)
    private ExampleAggregateJpaEntity aggregate;
}