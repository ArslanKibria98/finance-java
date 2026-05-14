package com.ksa.financing.risk.adapter.rest.controller;

import com.ksa.financing.infra.security.blacklist.BlacklistCacheService;
import com.ksa.financing.infra.security.blacklist.BlacklistEntry;
import com.ksa.financing.infra.security.blacklist.BlacklistType;
import com.ksa.financing.risk.infrastructure.blacklist.BlacklistRedisSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal blacklist check endpoint for service-to-service synchronous lookups.
 * Used as a fallback when a service cannot reach Redis directly, or for diagnostics.
 *
 * <p>NOT exposed via Kong public gateway. Authentication uses internal mTLS / shared secret.</p>
 */
@RestController
@RequestMapping("/internal/blacklist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Blacklist Internal", description = "Service-to-service blacklist lookup")
public class BlacklistInternalController {

    private final BlacklistCacheService cacheService;
    private final BlacklistRedisSyncService redisSync;

    @Operation(summary = "Check whether a value is blacklisted")
    @PostMapping("/check")
    public ResponseEntity<BlacklistCheckResponse> check(@RequestBody BlacklistCheckRequest request) {
        BlacklistEntry entry = cacheService.lookup(request.type(), request.value());
        boolean blocked = entry != null;
        return ResponseEntity.ok(new BlacklistCheckResponse(
                blocked,
                blocked ? entry.type() : null,
                blocked ? entry.reason() : null
        ));
    }

    @Operation(summary = "Force re-hydration of all blacklist types from Postgres → Redis")
    @PostMapping("/sync")
    public ResponseEntity<Void> resync() {
        log.warn("Manual blacklist Redis re-sync triggered");
        redisSync.syncAll();
        return ResponseEntity.accepted().build();
    }

    public record BlacklistCheckRequest(BlacklistType type, String value) {}

    public record BlacklistCheckResponse(boolean blocked, BlacklistType type, String reason) {}
}
