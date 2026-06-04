package com.ksa.financing.wallet.application.service;

import com.ksa.financing.wallet.application.usecase.IbftSettlementService;
import com.ksa.financing.wallet.domain.model.IbftTransaction;
import com.ksa.financing.wallet.domain.port.out.IbftTransactionRepository;
import com.ksa.financing.wallet.domain.port.out.ScotiaEftPort;
import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftReconciliationLogJpaEntity;
import com.ksa.financing.wallet.infrastructure.persistence.repository.JpaIbftReconciliationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Polls Scotia EFT INQUIRE for SUBMITTED/PROCESSING transfers and finalizes (settled) or
 * releases (rejected) them. Runs every N minutes (configurable). Each transfer is settled in
 * its own transaction via {@link IbftSettlementService} — one failure never aborts the run.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IbftReconciliationCronService {

    private final IbftTransactionRepository ibftRepository;
    private final ScotiaEftPort scotiaEftPort;
    private final IbftSettlementService settlementService;
    private final JpaIbftReconciliationLogRepository reconLogRepository;

    @Value("${ksa.wallet.ibft.recon.batch-size:100}")
    private int batchSize;

    @Scheduled(cron = "${ksa.wallet.ibft.recon.cron:0 */2 * * * *}")
    public void scheduledReconcile() {
        reconcileNow();
    }

    /** Public so an internal endpoint can trigger reconciliation on demand (DEV/ops). */
    public ReconResult reconcileNow() {
        long start = System.currentTimeMillis();
        List<IbftTransaction> pending = ibftRepository.findReconcilable(batchSize);
        int settled = 0, failed = 0, stillPending = 0, errors = 0;

        for (IbftTransaction tx : pending) {
            try {
                if (tx.getScotiaSubmissionId() == null) {
                    settlementService.markPending(tx.getId(), "NO_SUBMISSION");
                    stillPending++;
                    continue;
                }
                var inq = scotiaEftPort.inquire(tx.getScotiaSubmissionId());
                if (!inq.reachable()) { stillPending++; continue; }
                if (inq.settled()) {
                    settlementService.finalizeSettlement(tx.getId());
                    settled++;
                } else if (inq.rejected()) {
                    settlementService.failSettlement(tx.getId(), "IBFT.SCOTIA.REJECTED",
                            "Scotia EFT rejected: " + inq.submissionStatus() + "/" + inq.paymentStatus());
                    failed++;
                } else {
                    settlementService.markPending(tx.getId(), inq.submissionStatus());
                    stillPending++;
                }
            } catch (Exception e) {
                errors++;
                log.error("IBFT reconcile error for {}: {}", tx.getIbftNumber(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - start;
        var logRow = new IbftReconciliationLogJpaEntity();
        logRow.setScanned(pending.size());
        logRow.setSettled(settled);
        logRow.setFailed(failed);
        logRow.setStillPending(stillPending);
        logRow.setErrors(errors);
        logRow.setDurationMs(duration);
        try { reconLogRepository.save(logRow); } catch (Exception ignore) { }

        if (!pending.isEmpty()) {
            log.info("IBFT reconciliation: scanned={} settled={} failed={} pending={} errors={} ({}ms)",
                    pending.size(), settled, failed, stillPending, errors, duration);
        }
        return new ReconResult(pending.size(), settled, failed, stillPending, errors, duration);
    }

    public record ReconResult(int scanned, int settled, int failed, int stillPending, int errors, long durationMs) {}
}
