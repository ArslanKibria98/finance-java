package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.wallet.application.dto.ExternalTransferResponse;
import com.ksa.financing.wallet.application.dto.InitiateExternalTransferRequest;
import com.ksa.financing.wallet.domain.model.ExternalFundTransfer;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import com.ksa.financing.wallet.domain.port.in.InitiateExternalTransferUseCase;
import com.ksa.financing.wallet.domain.port.out.ExternalFundTransferRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * External fund transfers (to/from Canadian bank accounts via Scotia RTP).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wallets/transfers/external")
@Tag(name = "External Fund Transfers", description = "Outbound/inbound bank transfers via Scotia RTP")
public class ExternalTransferController {

    private final InitiateExternalTransferUseCase initiateExternalTransferUseCase;
    private final ExternalFundTransferRepository transferRepository;
    private final GetBalanceUseCase getBalanceUseCase;
    private final RestTemplate restTemplate;

    @Value("${app.services.customer-service-url:http://localhost:8084}")
    private String customerServiceUrl;

    public ExternalTransferController(InitiateExternalTransferUseCase initiateExternalTransferUseCase,
                                      ExternalFundTransferRepository transferRepository,
                                      GetBalanceUseCase getBalanceUseCase,
                                      RestTemplate restTemplate) {
        this.initiateExternalTransferUseCase = initiateExternalTransferUseCase;
        this.transferRepository = transferRepository;
        this.getBalanceUseCase = getBalanceUseCase;
        this.restTemplate = restTemplate;
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "create")
    @PostMapping
    @Operation(summary = "Send money from a wallet to an external Canadian bank account (Scotia RTP)")
    public ResponseEntity<ExternalTransferResponse> initiate(
            @Valid @RequestBody InitiateExternalTransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        UUID tenantId = extractTenantId(jwt);
        UUID userId = extractUserId(jwt);
        String idempotencyKey = idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()
                ? idempotencyKeyHeader
                : UUID.randomUUID().toString();

        // Debtor (sender) is ALWAYS the authenticated user — derived from the JWT mobile_number
        // claim — never taken as a request parameter. An explicit sourceWalletId (e.g. admin/ops
        // tooling) overrides. Fall back to keycloak-sub→customer resolution if mobile is absent.
        UUID sourceWalletId = request.sourceWalletId();
        UUID customerId = null;
        if (sourceWalletId == null) {
            String mobile = jwt.getClaimAsString("mobile_number");
            if (mobile != null && !mobile.isBlank()) {
                try {
                    Wallet senderWallet = getBalanceUseCase.getByMobile(tenantId, mobile);
                    sourceWalletId = senderWallet.getId();
                } catch (Exception e) {
                    log.warn("Sender wallet lookup by JWT mobile failed: {}", e.getMessage());
                }
            }
            if (sourceWalletId == null) {
                customerId = resolveCustomerId(userId);
            }
        }

        ExternalFundTransfer transfer = initiateExternalTransferUseCase.initiate(
                new InitiateExternalTransferUseCase.InitiateExternalTransferCommand(
                        tenantId,
                        sourceWalletId,
                        customerId,
                        request.counterpartyName(),
                        request.counterpartyAccount(),
                        request.counterpartyEmail(),
                        request.counterpartyBankCode(),
                        request.amount(),
                        request.currency(),
                        request.purposeNote(),
                        idempotencyKey,
                        userId,
                        httpRequest.getRemoteAddr(),
                        httpRequest.getHeader("X-Device-Id")));

        return ResponseEntity
                .status(transfer.getStatus() != null && "COMPLETED".equals(transfer.getStatus().name())
                        ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(toResponse(transfer));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "read")
    @GetMapping("/{transferId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @Operation(summary = "Get external transfer by ID")
    public ResponseEntity<ExternalTransferResponse> getById(
            @PathVariable UUID transferId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        ExternalFundTransfer transfer = transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("ExternalTransfer", transferId.toString()));
        return ResponseEntity.ok(toResponse(transfer));
    }

    @SecuredEndpoint(obj = "wallet.transfers", act = "list")
    @GetMapping("/by-wallet/{walletId:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @Operation(summary = "List external transfers for a wallet (paginated, searchable)")
    public PageResponse<ExternalTransferResponse> listByWallet(
            @PathVariable UUID walletId,
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        extractTenantId(jwt);
        return transferRepository.findAllByWallet(walletId, query).map(this::toResponse);
    }

    private UUID resolveCustomerId(UUID keycloakUserId) {
        if (keycloakUserId == null) return null;
        try {
            String url = customerServiceUrl + "/internal/customers/by-keycloak/" + keycloakUserId;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return keycloakUserId;
            }
            Map<?, ?> body = resp.getBody();
            Object dataObj = body.get("data");
            Map<?, ?> payload = (dataObj instanceof Map) ? (Map<?, ?>) dataObj : body;
            Object cid = payload.get("customerId");
            return cid != null ? UUID.fromString(cid.toString()) : keycloakUserId;
        } catch (Exception e) {
            log.warn("customer-service lookup failed for keycloakUserId={}: {}", keycloakUserId, e.getMessage());
            return keycloakUserId;
        }
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
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

    private ExternalTransferResponse toResponse(ExternalFundTransfer t) {
        return new ExternalTransferResponse(
                t.getId(),
                t.getTransferNumber(),
                t.getDirection() != null ? t.getDirection().name() : null,
                t.getWalletId(),
                t.getAccountNumber(),
                t.getCounterpartyName(),
                t.getCounterpartyAccount(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getPurposeNote(),
                t.getMovementId(),
                t.getLedgerEntryId(),
                t.getScotiaPaymentId(),
                t.getScotiaClearingRef(),
                t.getScotiaStatus(),
                t.isCounterpartyInternal(),
                t.getCounterpartyWalletId(),
                t.getCounterpartyMovementId(),
                t.getErrorCode(),
                t.getErrorMessage(),
                t.getInitiatedAt(),
                t.getCompletedAt());
    }
}
