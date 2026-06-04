package com.ksa.financing.wallet.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ibft_reconciliation_logs")
public class IbftReconciliationLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_at", nullable = false) private OffsetDateTime runAt;
    @Column(name = "scanned", nullable = false) private int scanned;
    @Column(name = "settled", nullable = false) private int settled;
    @Column(name = "failed", nullable = false) private int failed;
    @Column(name = "still_pending", nullable = false) private int stillPending;
    @Column(name = "errors", nullable = false) private int errors;
    @Column(name = "duration_ms", nullable = false) private long durationMs;

    @PrePersist
    void onCreate() { if (runAt == null) runAt = OffsetDateTime.now(); }

    public void setScanned(int v) { this.scanned = v; }
    public void setSettled(int v) { this.settled = v; }
    public void setFailed(int v) { this.failed = v; }
    public void setStillPending(int v) { this.stillPending = v; }
    public void setErrors(int v) { this.errors = v; }
    public void setDurationMs(long v) { this.durationMs = v; }
    public int getScanned() { return scanned; }
    public int getSettled() { return settled; }
    public int getFailed() { return failed; }
    public int getStillPending() { return stillPending; }
    public int getErrors() { return errors; }
}
