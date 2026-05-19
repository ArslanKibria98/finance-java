package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.application.dto.CheckRecipientsRequest;
import com.ksa.financing.wallet.application.dto.CheckRecipientsResponse;
import com.ksa.financing.wallet.application.dto.CheckRecipientsResponse.RecipientCheckItem;
import com.ksa.financing.wallet.application.dto.InitiateTransferRequest;
import com.ksa.financing.wallet.application.dto.RecipientLookupResponse;
import com.ksa.financing.wallet.application.dto.TransferByMobileRequest;
import com.ksa.financing.wallet.application.dto.TransferResponse;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.in.GetRecentRecipientsUseCase;
import com.ksa.financing.wallet.domain.port.in.GetTransferUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferByMobileUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateTransferUseCase;
import com.ksa.financing.wallet.domain.port.in.LookupRecipientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets/transfers")
@Tag(name = "Wallet Transfers", description = "Wallet-to-wallet P2P transfer operations")
public class WalletTransferController {

    private final InitiateTransferUseCase initiateTransferUseCase;
    private final InitiateTransferByMobileUseCase initiateTransferByMobileUseCase;
    private final LookupRecipientUseCase lookupRecipientUseCase;
    private final GetTransferUseCase getTransferUseCase;
    private final GetRecentRecipientsUseCase getRecentRecipientsUseCase;
    private final RestTemplate restTemplate;

    @Value("${app.services.customer-service-url:http://localhost:8084}")
    private String customerServiceUrl;

    public WalletTransferController(
            InitiateTransferUseCase initiateTransferUseCase,
            InitiateTransferByMobileUseCase initiateTransferByMobileUseCase,
            LookupRecipientUseCase lookupRecipientUseCase,
            GetTransferUseCase getTransferUseCase,
            GetRecentRecipientsUseCase getRecentRecipientsUseCase,
            RestTemplate restTemplate) {
        this.initiateTransferUseCase = initiateTransferUseCase;
        this.initiateTransferByMobileUseCase = initiateTransferByMobileUseCase;
        this.lookupRecipientUseCase = lookupRecipientUseCase;
        this.getTransferUseCase = getTransferUseCase;
        this.getRecentRecipientsUseCase = getRecentRecipientsUseCase;
        this.restTemplate = restTemplate;
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "read")
    @GetMapping("/recent-recipients")
    @Operation(summary = "Get last 5 unique recipients the authenticated user has sent money to")
    public ResponseEntity<List<GetRecentRecipientsUseCase.RecentRecipient>> getRecentRecipients(
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID keycloakUserId = extractKeycloakUserId(jwt);

        // Resolve customerId from keycloakUserId (similar to WalletController)
        UUID customerId = resolveCustomerId(keycloakUserId);

        log.info("Fetching recent recipients for keycloakUserId={} resolvedCustomerId={}", keycloakUserId, customerId);
        var recipients = getRecentRecipientsUseCase.getRecentRecipients(tenantId, customerId);
        return ResponseEntity.ok(recipients);
    }

    private UUID resolveCustomerId(UUID keycloakUserId) {
        try {
            String url = customerServiceUrl + "/internal/customers/by-keycloak/" + keycloakUserId;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return keycloakUserId; // Fallback
            }
            Map<?, ?> body = resp.getBody();
            Object dataObj = body.get("data");
            Map<?, ?> payload = (dataObj instanceof Map) ? (Map<?, ?>) dataObj : body;
            Object cid = payload.get("customerId");
            return cid != null ? UUID.fromString(cid.toString()) : keycloakUserId;
        } catch (Exception e) {
            log.warn("customer-service lookup failed for keycloakUserId={}: {} — falling back", keycloakUserId, e.getMessage());
            return keycloakUserId;
        }
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "create")
    @PostMapping
    @Operation(summary = "Initiate wallet-to-wallet transfer")
    public ResponseEntity<TransferResponse> initiate(
            @Valid @RequestBody InitiateTransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = extractUserId(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        log.info("Initiate transfer src={} dst={} amount={} key={} user={}",
                request.sourceWalletId(), request.destinationWalletId(),
                request.amount(), idempotencyKey, userId);

        WalletTransfer transfer = initiateTransferUseCase.initiate(
                new InitiateTransferUseCase.InitiateTransferCommand(
                        tenantId,
                        request.sourceWalletId(),
                        request.destinationWalletId(),
                        request.destinationWalletNumber(),
                        request.amount(),
                        request.currency(),
                        request.purposeNote(),
                        request.channel(),
                        idempotencyKey,
                        userId,
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("X-Device-Id")));

        return ResponseEntity
                .status("COMPLETED".equals(transfer.getStatus().name()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(toResponse(transfer));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "read")
    @GetMapping("/{transferId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @Operation(summary = "Get transfer by ID")
    public ResponseEntity<TransferResponse> getById(
            @PathVariable UUID transferId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        WalletTransfer transfer = getTransferUseCase.getById(tenantId, transferId);
        return ResponseEntity.ok(toResponse(transfer));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "list")
    @GetMapping("/by-wallet/{walletId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @Operation(summary = "List transfers for a wallet (sent + received). Supports query params: page, size, search (transferNumber, status, channel, purposeNote, errorCode, errorMessage).")
    public PageResponse<TransferResponse> listByWallet(
            @PathVariable UUID walletId,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        var page = getTransferUseCase.listByWallet(walletId, query);
        return page.map(this::toResponse);
    }

    private static boolean contains(String f, String term) {
        return f != null && f.toLowerCase().contains(term);
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "lookup")
    @GetMapping("/recipient")
    @Operation(summary = "Verify recipient by mobile number or IBAN before initiating a transfer")
    public ResponseEntity<RecipientLookupResponse> lookupRecipient(
            @RequestParam(name = "accountNumber") String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var view = lookupRecipientUseCase.lookup(tenantId, accountNumber);
        if (view.isEmpty()) {
            return ResponseEntity.ok(new RecipientLookupResponse(
                    false, null, null, null, null, null, null, null, null, false, "RECIPIENT_NOT_FOUND"));
        }
        var v = view.get();
        return ResponseEntity.ok(new RecipientLookupResponse(
                true,
                v.walletId(),
                v.walletNumber(),
                v.iban(),
                v.maskedName(),
                v.maskedMobile(),
                v.currency(),
                v.walletStatus() != null ? v.walletStatus().name() : null,
                v.userStatus(),
                v.canReceive(),
                v.reason()));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "lookup")
    @PostMapping("/recipients/check")
    @Operation(summary = "Bulk check: for each mobile number, return whether it has an active wallet")
    public ResponseEntity<CheckRecipientsResponse> checkRecipients(
            @Valid @RequestBody CheckRecipientsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        var checks = lookupRecipientUseCase.checkRecipientsByMobile(request.mobileNumbers());
        var items = checks.stream()
                .map(c -> new RecipientCheckItem(
                        c.found(),
                        c.walletId(),
                        c.walletNumber(),
                        c.iban(),
                        c.maskedName(),
                        c.maskedMobile(),
                        c.currency(),
                        c.walletStatus() != null ? c.walletStatus().name() : null,
                        c.userStatus(),
                        c.canReceive(),
                        c.reason()))
                .toList();
        return ResponseEntity.ok(new CheckRecipientsResponse(items));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "create")
    @PostMapping("/by-mobile")
    @Operation(summary = "Initiate transfer using only the receiver's mobile number (sender from JWT)")
    public ResponseEntity<TransferResponse> initiateByMobile(
            @Valid @RequestBody TransferByMobileRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        UUID senderKeycloakId = extractKeycloakUserId(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        log.info("Initiate transfer-by-mobile sender={} receiverMobile=****{} amount={}",
                senderKeycloakId,
                request.receiverMobile() != null && request.receiverMobile().length() >= 4
                        ? request.receiverMobile().substring(request.receiverMobile().length() - 4)
                        : "?",
                request.amount());

        WalletTransfer transfer = initiateTransferByMobileUseCase.initiateByMobile(
                new InitiateTransferByMobileUseCase.InitiateByMobileCommand(
                        senderKeycloakId,
                        request.receiverMobile(),
                        request.amount(),
                        request.currency(),
                        request.purposeNote(),
                        idempotencyKey,
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("X-Device-Id")));

        return ResponseEntity
                .status("COMPLETED".equals(transfer.getStatus().name()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(toResponse(transfer));
    }

    private UUID extractKeycloakUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT missing subject (sub) claim");
        }
        try {
            return UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT subject is not a valid UUID");
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private UUID extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        try {
            return sub != null ? UUID.fromString(sub) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TransferResponse toResponse(WalletTransfer t) {
        return new TransferResponse(
                t.getId(),
                t.getTransferNumber(),
                t.getSourceWalletId(),
                t.getDestinationWalletId(),
                t.getAmount(),
                t.getFeeAmount(),
                t.getTotalDebit(),
                t.getCurrency(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getChannel() != null ? t.getChannel().name() : null,
                t.getPurposeNote(),
                t.getDebitMovementId(),
                t.getCreditMovementId(),
                t.getErrorCode(),
                t.getErrorMessage(),
                t.getInitiatedAt(),
                t.getCompletedAt());
    }
}
