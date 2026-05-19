package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.wallet.application.dto.TopUpRequest;
import com.ksa.financing.wallet.application.dto.TopUpResponse;
import com.ksa.financing.wallet.application.dto.WalletResponse;
import com.ksa.financing.wallet.domain.model.TopUpTransaction;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import com.ksa.financing.wallet.domain.port.in.GetTransactionHistoryUseCase;
import com.ksa.financing.wallet.domain.port.in.GetTransactionHistoryUseCase.TransactionItem;
import com.ksa.financing.wallet.domain.port.in.TopUpUseCase;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet", description = "Wallet management and top-up operations")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);

    private final CreateWalletUseCase createWalletUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final TopUpUseCase topUpUseCase;
    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final RestTemplate restTemplate;
    private final String customerServiceUrl;

    public WalletController(
            CreateWalletUseCase createWalletUseCase,
            GetBalanceUseCase getBalanceUseCase,
            TopUpUseCase topUpUseCase,
            GetTransactionHistoryUseCase getTransactionHistoryUseCase,
            RestTemplate restTemplate,
            @Value("${app.services.customer-service-url:http://localhost:8084}") String customerServiceUrl) {
        this.createWalletUseCase = createWalletUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
        this.topUpUseCase = topUpUseCase;
        this.getTransactionHistoryUseCase = getTransactionHistoryUseCase;
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/by-customer/{customerId}")
    @Operation(summary = "Get wallet by customer ID")
    public ResponseEntity<WalletResponse> getByCustomerId(
            @PathVariable UUID customerId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Getting wallet for customer: {} tenant: {}", customerId, tenantId);

        Wallet wallet = getBalanceUseCase.getByCustomerId(tenantId, customerId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/me/balance")
    @Operation(summary = "Get authenticated customer's wallet balance (resolved from JWT)")
    public ResponseEntity<WalletResponse> getMyBalance(
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID keycloakUserId = extractCustomerId(jwt);

        // JWT.sub is the Keycloak user ID; the wallet is keyed by customer-service's
        // customer.id. Resolve via customer-service /internal/customers/by-keycloak.
        UUID customerId = resolveCustomerId(keycloakUserId);
        log.info("/me/balance keycloakUserId={} customerId={} tenant={}",
                keycloakUserId, customerId, tenantId);

        Wallet wallet = getBalanceUseCase.getByCustomerId(tenantId, customerId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    private UUID resolveCustomerId(UUID keycloakUserId) {
        try {
            String url = customerServiceUrl + "/internal/customers/by-keycloak/" + keycloakUserId;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw NotFoundException.forEntity("Customer (keycloak)", keycloakUserId.toString());
            }
            Map<?, ?> body = resp.getBody();
            // Response is wrapped: { "data": { "customerId": "...", ... } }
            Object dataObj = body.get("data");
            Map<?, ?> payload = (dataObj instanceof Map) ? (Map<?, ?>) dataObj : body;
            Object cid = payload.get("customerId");
            if (cid == null) {
                throw NotFoundException.forEntity("Customer (keycloak)", keycloakUserId.toString());
            }
            return UUID.fromString(cid.toString());
        } catch (NotFoundException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("customer-service lookup failed for keycloakUserId={}: {} — falling back to keycloakUserId as customerId",
                    keycloakUserId, e.getMessage());
            return keycloakUserId;
        }
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/{walletId}/balance")
    @Operation(summary = "Get wallet balance by wallet ID")
    public ResponseEntity<WalletResponse> getByWalletId(
            @PathVariable UUID walletId,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Getting balance for wallet: {}", walletId);

        Wallet wallet = getBalanceUseCase.getByWalletId(walletId);
        return ResponseEntity.ok(toResponse(wallet));
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance by mobile number")
    public ResponseEntity<WalletResponse> getByMobile(
            @RequestParam("mobile") String mobile,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Getting balance for mobile: {}", mobile);

        Wallet wallet = getBalanceUseCase.getByMobile(tenantId, mobile);
        return ResponseEntity.ok(toResponse(wallet));
    }

    @SecuredEndpoint(obj = "wallets", act = "manage")
    @PostMapping("/{walletId}/top-up")
    @Operation(summary = "Top up wallet balance")
    public ResponseEntity<TopUpResponse> topUp(
            @PathVariable UUID walletId,
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Processing top-up for wallet: {} amount: {} method: {}", walletId, request.amount(), request.method());

        TopUpTransaction transaction = topUpUseCase.topUp(new TopUpUseCase.TopUpCommand(
                tenantId,
                walletId,
                request.amount(),
                request.method(),
                request.sourceIban(),
                request.idempotencyKey()
        ));

        return ResponseEntity.ok(toTopUpResponse(transaction));
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/{walletId}/transactions")
    @Operation(summary = "Get unified transaction history (Fineract txs + transfer counterparty info)")
    public PageResponse<TransactionItem> getTransactions(
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        return getTransactionHistoryUseCase.getHistory(walletId, page, size);
    }

    @SecuredEndpoint(obj = "wallets", act = "read")
    @GetMapping("/me/transactions/recent")
    @Operation(summary = "Get authenticated customer's last 10 transactions (resolved from JWT)")
    public List<TransactionItem> getMyRecentTransactions(
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        UUID keycloakUserId = extractCustomerId(jwt);
        UUID customerId = resolveCustomerId(keycloakUserId);

        Wallet wallet = getBalanceUseCase.getByCustomerId(tenantId, customerId);
        log.info("/me/transactions/recent keycloakUserId={} customerId={} walletId={} tenant={}",
                keycloakUserId, customerId, wallet.getId(), tenantId);

        return getTransactionHistoryUseCase.getHistory(wallet.getId(), 0, 10).content();
    }

    @SecuredEndpoint(obj = "wallets", act = "create")
    @PostMapping("/create")
    @Operation(summary = "Create wallet for customer (internal endpoint)")
    public ResponseEntity<WalletResponse> createWallet(
            @RequestBody CreateWalletRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tenantId = extractTenantId(jwt);
        log.info("Creating wallet for customer: {} tenant: {}", request.customerId(), tenantId);

        Wallet wallet = createWalletUseCase.create(new CreateWalletUseCase.CreateWalletCommand(
                tenantId,
                request.customerId(),
                request.currency(),
                request.iban(),
                null
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(wallet));
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private UUID extractCustomerId(Jwt jwt) {
        String customer = jwt.getClaimAsString("customer_id");
        if (customer == null) customer = jwt.getSubject();
        if (customer == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT missing customer_id and subject");
        }
        try {
            return UUID.fromString(customer);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT customer_id/sub is not a valid UUID");
        }
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getWalletNumber(),
                wallet.getCustomerId(),
                wallet.getAvailableBalance(),
                wallet.getReservedBalance(),
                wallet.getTotalBalance(),
                wallet.getCurrency(),
                wallet.getStatus() != null ? wallet.getStatus().name() : null,
                wallet.isAutoDebitEnabled(),
                wallet.getFineractSavingsAccountId(),
                wallet.isLedgerSynced(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private TopUpResponse toTopUpResponse(TopUpTransaction txn) {
        return new TopUpResponse(
                txn.getId(),
                txn.getTransactionNumber(),
                txn.getMethod() != null ? txn.getMethod().name() : null,
                txn.getAmount(),
                txn.getFeeAmount(),
                txn.getNetAmount(),
                txn.getStatus() != null ? txn.getStatus().name() : null,
                txn.getInitiatedAt(),
                txn.getCompletedAt()
        );
    }

    public record CreateWalletRequest(
            UUID customerId,
            String currency,
            String iban
    ) {}
}
