package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.fraud.FraudEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FraudEventRepository {

    FraudEvent save(FraudEvent event);

    Optional<FraudEvent> findByTenantAndEventId(UUID tenantId, String eventId);

    List<FraudEvent> findByCustomerId(UUID tenantId, String customerId, LocalDateTime since);

    List<FraudEvent> findByDeviceId(String deviceId, LocalDateTime since);

    List<FraudEvent> findByDisbursementIban(String iban, LocalDateTime since);

    long countByCustomerAndType(UUID tenantId, String customerId, String eventType, LocalDateTime since);

    long countDistinctCustomersByDeviceId(String deviceId, LocalDateTime since);

    long countByCustomerAndTypeBetween(UUID tenantId, String customerId, String eventType,
                                       LocalDateTime from, LocalDateTime to);

    List<FraudEvent> findDuplicateApplications(UUID tenantId, String customerId,
                                               String productType, java.math.BigDecimal amount,
                                               LocalDateTime since);

    long countDistinctCustomersByIban(String iban, LocalDateTime since);

    List<FraudEvent> findByIpAddress(String ipAddress, LocalDateTime since);

    long countDistinctCardsByCustomer(UUID tenantId, String customerId, LocalDateTime since);

    List<FraudEvent> findRecentByType(UUID tenantId, String customerId,
                                      String eventType, int limit);

    List<FraudEvent> findLastNByCustomer(UUID tenantId, String customerId, int limit);

    long countReversalsForCustomer(UUID tenantId, String customerId);

    long countReversalsForLoan(UUID tenantId, String customerId, String loanApplicationId);
}
