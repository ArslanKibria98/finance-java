package com.ksa.financing.ledger.adapter.rest.proxy;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authenticated Fineract Savings proxy. Used by domain services that already carry a JWT.
 * <p>
 * All wallet/savings operations against Fineract MUST go through this controller.
 * No service may call Fineract directly.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/fineract-proxy/savings")
@RequiredArgsConstructor
@Tag(name = "Fineract Savings Proxy",
        description = "Routes all Fineract savings-account operations through ledger-service")
public class SavingsProxyController {

    private final FineractProxyDispatcher dispatcher;

    @GetMapping("/clients/by-external/{externalId}")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "read")
    @Operation(summary = "Lookup Fineract client by externalId")
    public ResponseEntity<Map<String, Object>> lookupClient(
            @PathVariable String externalId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchGet(jwt, callerService, correlationId, null,
                "savings.client.lookup",
                "/clients?externalId=" + externalId);
    }

    @PostMapping("/clients")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "create")
    @Operation(summary = "Create a Fineract client (savings flow)")
    public ResponseEntity<Map<String, Object>> createClient(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.client.create", "/clients", body);
    }

    @PostMapping("/accounts")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "create")
    @Operation(summary = "Create a Fineract savings account")
    public ResponseEntity<Map<String, Object>> createSavings(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.create", "/savingsaccounts", body);
    }

    @PostMapping("/accounts/{savingsId}/approve")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "update")
    @Operation(summary = "Approve a Fineract savings account")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.approve",
                "/savingsaccounts/" + savingsId + "?command=approve", body);
    }

    @PostMapping("/accounts/{savingsId}/activate")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "update")
    @Operation(summary = "Activate a Fineract savings account")
    public ResponseEntity<Map<String, Object>> activate(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.activate",
                "/savingsaccounts/" + savingsId + "?command=activate", body);
    }

    @DeleteMapping("/accounts/{savingsId}")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "delete")
    @Operation(summary = "Delete (compensation) a Fineract savings account")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long savingsId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchDelete(jwt, callerService, correlationId,
                "savings.account.delete",
                "/savingsaccounts/" + savingsId);
    }

    @GetMapping("/accounts/{savingsId}")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "read")
    @Operation(summary = "Fetch Fineract savings account info (incl. balance)")
    public ResponseEntity<Map<String, Object>> getAccount(
            @PathVariable Long savingsId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchGet(jwt, callerService, correlationId, null,
                "savings.account.get",
                "/savingsaccounts/" + savingsId);
    }

    @GetMapping("/accounts/{savingsId}/transactions")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "read")
    @Operation(summary = "Fetch Fineract savings account transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @PathVariable Long savingsId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchGet(jwt, callerService, correlationId, null,
                "savings.account.transactions",
                "/savingsaccounts/" + savingsId + "?associations=transactions");
    }

    @PostMapping("/accounts/{savingsId}/deposit")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "create")
    @Operation(summary = "Deposit funds into a Fineract savings account")
    public ResponseEntity<Map<String, Object>> deposit(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.deposit",
                "/savingsaccounts/" + savingsId + "/transactions?command=deposit", body);
    }

    @PostMapping("/accounts/{savingsId}/withdraw")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "create")
    @Operation(summary = "Withdraw funds from a Fineract savings account")
    public ResponseEntity<Map<String, Object>> withdraw(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.withdraw",
                "/savingsaccounts/" + savingsId + "/transactions?command=withdrawal", body);
    }

    @PostMapping("/accounts/{savingsId}/hold")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "update")
    @Operation(summary = "Place a hold (block) on a Fineract savings account")
    public ResponseEntity<Map<String, Object>> hold(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.hold",
                "/savingsaccounts/" + savingsId + "?command=block", body);
    }

    @PostMapping("/accounts/{savingsId}/release")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "update")
    @Operation(summary = "Release a hold on a Fineract savings account")
    public ResponseEntity<Map<String, Object>> release(
            @PathVariable Long savingsId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.release",
                "/savingsaccounts/" + savingsId + "?command=unblock", body);
    }

    @PostMapping("/transactions/{txnId}/undo")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "update")
    @Operation(summary = "Undo a Fineract savings transaction")
    public ResponseEntity<Map<String, Object>> undoTransaction(
            @PathVariable Long txnId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam Long savingsId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.transaction.undo",
                "/savingsaccounts/" + savingsId + "/transactions/" + txnId + "?command=undo",
                body == null ? Map.of() : body);
    }

    @PostMapping("/transfers")
    @SecuredEndpoint(obj = "ledger.fineract-proxy.savings", act = "create")
    @Operation(summary = "Transfer funds between two Fineract savings accounts")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Caller-Service", required = false) String callerService,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        return dispatcher.dispatchPost(jwt, callerService, correlationId, idempotencyKey,
                "savings.account.transfer", "/accounttransfers", body);
    }
}
