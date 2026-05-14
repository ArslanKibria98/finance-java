package com.ksa.financing.ledger.adapter.rest.proxy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal (no-JWT) twin of {@link SavingsProxyController}. Used by Temporal activities
 * and other service-to-service flows where a JWT is not available.
 * <p>
 * Caller MUST pass {@code X-Tenant-Id} and SHOULD pass {@code X-Caller-Service}.
 * All paths under {@code /internal/**} are permit-all in {@code WebConfig.securityFilterChain}.
 */
@Slf4j
@RestController
@RequestMapping("/internal/fineract-proxy/savings")
@RequiredArgsConstructor
@Tag(name = "Internal Fineract Savings Proxy",
        description = "Service-to-service Fineract savings operations — no JWT required")
public class InternalSavingsProxyController {

    private final FineractProxyDispatcher dispatcher;

    @GetMapping("/clients/by-external/{externalId}")
    @Operation(summary = "Lookup Fineract client by externalId (internal)")
    public ResponseEntity<Map<String, Object>> lookupClient(
            @PathVariable String externalId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, null,
                "savings.client.lookup", HttpMethod.GET,
                "/clients?externalId=" + externalId, null);
    }

    @PostMapping("/clients")
    @Operation(summary = "Create a Fineract client (internal)")
    public ResponseEntity<Map<String, Object>> createClient(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.client.create", HttpMethod.POST, "/clients", body);
    }

    @PostMapping("/accounts")
    @Operation(summary = "Create a Fineract savings account (internal)")
    public ResponseEntity<Map<String, Object>> createSavings(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.create", HttpMethod.POST, "/savingsaccounts", body);
    }

    @PostMapping("/accounts/{savingsId}/approve")
    @Operation(summary = "Approve a Fineract savings account (internal)")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.approve", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "?command=approve", body);
    }

    @PostMapping("/accounts/{savingsId}/activate")
    @Operation(summary = "Activate a Fineract savings account (internal)")
    public ResponseEntity<Map<String, Object>> activate(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.activate", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "?command=activate", body);
    }

    @DeleteMapping("/accounts/{savingsId}")
    @Operation(summary = "Delete a Fineract savings account (internal compensation)")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long savingsId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, null,
                "savings.account.delete", HttpMethod.DELETE,
                "/savingsaccounts/" + savingsId, null);
    }

    @GetMapping("/accounts/{savingsId}")
    @Operation(summary = "Fetch Fineract savings account info (internal)")
    public ResponseEntity<Map<String, Object>> getAccount(
            @PathVariable Long savingsId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, null,
                "savings.account.get", HttpMethod.GET,
                "/savingsaccounts/" + savingsId, null);
    }

    @GetMapping("/accounts/{savingsId}/transactions")
    @Operation(summary = "Fetch Fineract savings transactions (internal)")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @PathVariable Long savingsId,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, null,
                "savings.account.transactions", HttpMethod.GET,
                "/savingsaccounts/" + savingsId + "?associations=transactions", null);
    }

    @PostMapping("/accounts/{savingsId}/deposit")
    @Operation(summary = "Deposit funds (internal)")
    public ResponseEntity<Map<String, Object>> deposit(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.deposit", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "/transactions?command=deposit", body);
    }

    @PostMapping("/accounts/{savingsId}/withdraw")
    @Operation(summary = "Withdraw funds (internal)")
    public ResponseEntity<Map<String, Object>> withdraw(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.withdraw", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "/transactions?command=withdrawal", body);
    }

    @PostMapping("/accounts/{savingsId}/hold")
    @Operation(summary = "Place a hold on savings (internal)")
    public ResponseEntity<Map<String, Object>> hold(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.hold", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "?command=block", body);
    }

    @PostMapping("/accounts/{savingsId}/release")
    @Operation(summary = "Release a hold (internal)")
    public ResponseEntity<Map<String, Object>> release(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.release", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "?command=unblock", body);
    }

    @PostMapping("/transactions/{txnId}/undo")
    @Operation(summary = "Undo a savings transaction (internal)")
    public ResponseEntity<Map<String, Object>> undoTransaction(
            @PathVariable Long txnId,
            @RequestParam Long savingsId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.transaction.undo", HttpMethod.POST,
                "/savingsaccounts/" + savingsId + "/transactions/" + txnId + "?command=undo",
                body == null ? Map.of() : body);
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer funds between savings accounts (internal)")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchInternal(tenantId, callerService, correlationId, idempotencyKey,
                "savings.account.transfer", HttpMethod.POST, "/accounttransfers", body);
    }
}
