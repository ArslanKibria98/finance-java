package com.ksa.financing.customer.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_block_codes")
@Getter
@Setter
public class CustomerBlockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ManyToOne
    @JoinColumn(name = "block_code_id", nullable = false)
    private BlockCodeJpaEntity blockCode;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "reason")
    private String reason;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
