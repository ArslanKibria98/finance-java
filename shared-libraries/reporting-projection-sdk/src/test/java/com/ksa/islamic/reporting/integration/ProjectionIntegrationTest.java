package com.ksa.islamic.reporting.integration;

import com.ksa.islamic.reporting.config.ProjectionConfig;
import com.ksa.islamic.reporting.event.LoanEvents;
import com.ksa.islamic.reporting.projector.LoanProjector;
import com.ksa.islamic.reporting.projector.ProjectionCheckpoint;
import com.ksa.islamic.reporting.projector.ProjectionCheckpointRepository;
import com.ksa.islamic.reporting.readmodel.LoanSummaryReadModel;
import com.ksa.islamic.reporting.repository.LoanReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the projection system using Testcontainers.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("projection_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // PostgreSQL properties
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Read model datasource
        registry.add("spring.datasource.read-model.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.read-model.username", postgres::getUsername);
        registry.add("spring.datasource.read-model.password", postgres::getPassword);

        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
    }

    @Autowired
    private LoanReadRepository loanReadRepository;

    @Autowired
    private ProjectionCheckpointRepository checkpointRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private LoanProjector loanProjector;

    @BeforeEach
    void setUp() {
        loanReadRepository.deleteAll();
        checkpointRepository.deleteAll();
    }

    @Test
    @Transactional
    void testEndToEndProjection() throws Exception {
        // Given - Create and send a loan approved event
        LoanEvents.LoanApproved approvedEvent = LoanEvents.LoanApproved.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-INT-001")
                .tenantId("tenant-test")
                .loanAccountNumber("LN-TEST-001")
                .customerId("CUST-TEST-001")
                .customerName("Test Customer")
                .productType("MURABAHA")
                .principalAmount(new BigDecimal("50000.00"))
                .interestRate(new BigDecimal("4.5"))
                .termMonths(36)
                .approvalDate(LocalDate.now())
                .approverUserId("USER-TEST")
                .build();

        // When - Send event to Kafka
        kafkaTemplate.send("domain.loan.approved", approvedEvent.getAggregateId(), approvedEvent)
                .get(5, TimeUnit.SECONDS);

        // Then - Wait for projection to be processed
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var loan = loanReadRepository.findById("LOAN-INT-001");
                    assertThat(loan).isPresent();
                    assertThat(loan.get().getLoanAccountNumber()).isEqualTo("LN-TEST-001");
                    assertThat(loan.get().getStatus()).isEqualTo("APPROVED");
                });

        // Verify checkpoint was created
        var checkpoints = checkpointRepository.findByProjectorNameAndEventId(
                "LoanProjector", approvedEvent.getEventId().toString());
        assertThat(checkpoints).isPresent();

        // Given - Send disbursement event
        LoanEvents.LoanDisbursed disbursedEvent = LoanEvents.LoanDisbursed.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-INT-001")
                .tenantId("tenant-test")
                .loanAccountNumber("LN-TEST-001")
                .customerId("CUST-TEST-001")
                .disbursementAmount(new BigDecimal("50000.00"))
                .disbursementDate(LocalDate.now())
                .disbursementChannel("BANK_TRANSFER")
                .bankAccountNumber("ACC-TEST")
                .build();

        // When - Send disbursement event
        kafkaTemplate.send("domain.loan.disbursed", disbursedEvent.getAggregateId(), disbursedEvent)
                .get(5, TimeUnit.SECONDS);

        // Then - Verify loan status updated
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var loan = loanReadRepository.findById("LOAN-INT-001");
                    assertThat(loan).isPresent();
                    assertThat(loan.get().getStatus()).isEqualTo("DISBURSED");
                    assertThat(loan.get().getDisbursementDate()).isEqualTo(LocalDate.now());
                });
    }

    @Test
    void testIdempotency() {
        // Given - Same event sent multiple times
        LoanEvents.LoanApproved event = LoanEvents.LoanApproved.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-IDEM-001")
                .tenantId("tenant-test")
                .loanAccountNumber("LN-IDEM-001")
                .customerId("CUST-IDEM-001")
                .customerName("Idempotent Customer")
                .productType("IJARA")
                .principalAmount(new BigDecimal("75000.00"))
                .interestRate(new BigDecimal("3.5"))
                .termMonths(48)
                .approvalDate(LocalDate.now())
                .approverUserId("USER-IDEM")
                .build();

        // When - Process event multiple times
        loanProjector.projectIncremental(event);
        loanProjector.projectIncremental(event);
        loanProjector.projectIncremental(event);

        // Then - Only one loan should be created
        var loans = loanReadRepository.findAll();
        assertThat(loans).hasSize(1);
        assertThat(loans.get(0).getLoanAccountNumber()).isEqualTo("LN-IDEM-001");

        // Only one checkpoint should exist
        var checkpoints = checkpointRepository.findAll();
        var eventCheckpoints = checkpoints.stream()
                .filter(c -> c.getEventId().equals(event.getEventId().toString()))
                .toList();
        assertThat(eventCheckpoints).hasSize(1);
    }

    @Test
    void testQueryPerformance() {
        // Given - Create multiple loans
        for (int i = 0; i < 100; i++) {
            LoanSummaryReadModel loan = LoanSummaryReadModel.builder()
                    .loanId("LOAN-PERF-" + i)
                    .tenantId("tenant-test")
                    .loanAccountNumber("LN-PERF-" + i)
                    .customerId("CUST-" + (i % 10)) // 10 different customers
                    .customerName("Customer " + (i % 10))
                    .productType(i % 2 == 0 ? "MURABAHA" : "IJARA")
                    .status(i % 3 == 0 ? "OVERDUE" : "ACTIVE")
                    .principalAmount(new BigDecimal("100000.00"))
                    .outstandingPrincipal(new BigDecimal(100000 - (i * 1000)))
                    .interestRate(new BigDecimal("5.0"))
                    .daysOverdue(i % 3 == 0 ? i : 0)
                    .overdueAmount(i % 3 == 0 ? new BigDecimal(i * 100) : BigDecimal.ZERO)
                    .build();
            loanReadRepository.save(loan);
        }

        // When - Execute various queries
        long startTime = System.currentTimeMillis();

        // Query by customer
        var customerLoans = loanReadRepository.findActiveLoansByCustomer("CUST-5");

        // Query overdue loans
        var overdueLoans = loanReadRepository.findByStatus("OVERDUE");

        // Query by product type
        var murabahaLoans = loanReadRepository.findByProductType("MURABAHA");

        long queryTime = System.currentTimeMillis() - startTime;

        // Then - Verify performance
        assertThat(queryTime).isLessThan(100); // All queries should complete in < 100ms
        assertThat(customerLoans).isNotEmpty();
        assertThat(overdueLoans).isNotEmpty();
        assertThat(murabahaLoans).hasSize(50);
    }

    @Test
    void testMaterializedViewRefresh() {
        // This test would verify materialized view refresh
        // Implementation depends on actual database setup

        // Given - Insert test data
        LoanSummaryReadModel loan = LoanSummaryReadModel.builder()
                .loanId("LOAN-MV-001")
                .tenantId("tenant-test")
                .loanAccountNumber("LN-MV-001")
                .customerId("CUST-MV-001")
                .customerName("MV Test Customer")
                .productType("MURABAHA")
                .status("ACTIVE")
                .principalAmount(new BigDecimal("200000.00"))
                .outstandingPrincipal(new BigDecimal("180000.00"))
                .interestRate(new BigDecimal("6.0"))
                .daysOverdue(0)
                .overdueAmount(BigDecimal.ZERO)
                .build();

        loanReadRepository.save(loan);

        // When - Query portfolio summary (would use materialized view)
        var portfolioSummary = loanReadRepository.getPortfolioSummary("tenant-test");

        // Then - Verify summary data
        assertThat(portfolioSummary).isNotNull();
        // Additional assertions based on your portfolio summary structure
    }
}