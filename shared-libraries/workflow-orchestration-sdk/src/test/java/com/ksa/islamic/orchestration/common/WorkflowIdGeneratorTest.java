package com.ksa.islamic.orchestration.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Workflow ID Generator Tests")
class WorkflowIdGeneratorTest {

    @Test
    @DisplayName("Should generate valid UUID-based workflow ID")
    void testGenerateUUID() {
        // Act
        String id = WorkflowIdGenerator.generateUUID();

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }

    @Test
    @DisplayName("Should generate workflow ID with prefix")
    void testGenerateWithPrefix() {
        // Act
        String id = WorkflowIdGenerator.generateWithPrefix("LOAN");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("LOAN-");
        assertThat(id.split("-")).hasSize(6); // prefix + 5 UUID parts
    }

    @Test
    @DisplayName("Should generate workflow ID with timestamp")
    void testGenerateWithTimestamp() {
        // Act
        String id = WorkflowIdGenerator.generateWithTimestamp("ORDER");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("ORDER-");
        assertThat(id).matches("ORDER-\\d{8}-\\d{6}-[a-f0-9]{8}");
    }

    @Test
    @DisplayName("Should generate workflow ID for entity")
    void testGenerateForEntity() {
        // Act
        String id = WorkflowIdGenerator.generateForEntity("customer", "CUST123");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("CUSTOMER-CUST123-");
        assertThat(id).matches("CUSTOMER-CUST123-[a-f0-9]{8}");
    }

    @Test
    @DisplayName("Should generate workflow ID for operation")
    void testGenerateForOperation() {
        // Act
        String id = WorkflowIdGenerator.generateForOperation("loan", "L123", "disbursement");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("LOAN-L123-DISBURSEMENT-");
        assertThat(id).matches("LOAN-L123-DISBURSEMENT-[a-f0-9]{8}");
    }

    @Test
    @DisplayName("Should generate workflow ID for batch")
    void testGenerateForBatch() {
        // Act
        String id = WorkflowIdGenerator.generateForBatch("disbursement", "001");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("BATCH-DISBURSEMENT-");
        assertThat(id).matches("BATCH-DISBURSEMENT-\\d{8}-\\d{6}-001");
    }

    @Test
    @DisplayName("Should generate workflow ID for scheduled job")
    void testGenerateForScheduled() {
        // Act
        String id = WorkflowIdGenerator.generateForScheduled("daily-accrual");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("SCHED-DAILY-ACCRUAL-");
        assertThat(id).matches("SCHED-DAILY-ACCRUAL-\\d{8}-\\d{6}");
    }

    @Test
    @DisplayName("Should generate workflow ID for tenant")
    void testGenerateForTenant() {
        // Act
        String id = WorkflowIdGenerator.generateForTenant("tenant1", "origination");

        // Assert
        assertThat(id).isNotNull();
        assertThat(id).startsWith("T-TENANT1-ORIGINATION-");
        assertThat(id).matches("T-TENANT1-ORIGINATION-[a-f0-9]{12}");
    }

    @Test
    @DisplayName("Should generate idempotent workflow ID")
    void testGenerateIdempotent() {
        // Act
        String id1 = WorkflowIdGenerator.generateIdempotent("payment", "PAY-123");
        String id2 = WorkflowIdGenerator.generateIdempotent("payment", "PAY-123");

        // Assert
        assertThat(id1).isEqualTo("IDMP-PAYMENT-PAY-123");
        assertThat(id2).isEqualTo("IDMP-PAYMENT-PAY-123");
        assertThat(id1).isEqualTo(id2); // Same input produces same output
    }

    @Test
    @DisplayName("Should validate workflow IDs correctly")
    void testIsValidWorkflowId() {
        // Assert valid IDs
        assertThat(WorkflowIdGenerator.isValidWorkflowId("LOAN-123-ABC")).isTrue();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("simple123")).isTrue();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("ID_WITH_UNDERSCORE")).isTrue();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("id-with-hyphens")).isTrue();

        // Assert invalid IDs
        assertThat(WorkflowIdGenerator.isValidWorkflowId(null)).isFalse();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("")).isFalse();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("  ")).isFalse();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("id with spaces")).isFalse();
        assertThat(WorkflowIdGenerator.isValidWorkflowId("id@with#special$chars")).isFalse();
    }

    @Test
    @DisplayName("Should extract prefix from workflow ID")
    void testExtractPrefix() {
        // Assert
        assertThat(WorkflowIdGenerator.extractPrefix("LOAN-123-ABC")).isEqualTo("LOAN");
        assertThat(WorkflowIdGenerator.extractPrefix("ORDER-XYZ")).isEqualTo("ORDER");
        assertThat(WorkflowIdGenerator.extractPrefix("simple")).isNull();
        assertThat(WorkflowIdGenerator.extractPrefix(null)).isNull();
    }
}