package com.ksa.islamic.messaging.kafka.producer;

import com.ksa.islamic.messaging.contract.EventEnvelope;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DomainEventProducer
 */
@ExtendWith(MockitoExtension.class)
class DomainEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @Mock
    private RecordMetadata recordMetadata;

    private MeterRegistry meterRegistry;
    private DomainEventProducer producer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        producer = new DomainEventProducer(kafkaTemplate, meterRegistry);
    }

    @Test
    void shouldPublishEventSuccessfully() {
        // Given
        String topicName = "domain.lending.loan.approved";
        TestEvent event = new TestEvent("loan-123", "APPROVED");
        String tenantId = "tenant-001";
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(100L);

        // When
        CompletableFuture<SendResult<String, Object>> result =
            producer.publishEvent(topicName, event, tenantId, correlationId);

        // Complete the future
        future.complete(sendResult);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isDone()).isTrue();

        // Verify the producer record was created correctly
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());

        ProducerRecord<String, Object> capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.topic()).isEqualTo(topicName);
        assertThat(capturedRecord.key()).isEqualTo(tenantId);

        EventEnvelope envelope = (EventEnvelope) capturedRecord.value();
        assertThat(envelope.getTenantId()).isEqualTo(tenantId);
        assertThat(envelope.getCorrelationId()).isEqualTo(correlationId);
        assertThat(envelope.getEventType()).isEqualTo("TestEvent");
        assertThat(envelope.getPayload()).isEqualTo(event);

        // Verify headers
        assertThat(capturedRecord.headers().lastHeader("tenant.id")).isNotNull();
        assertThat(capturedRecord.headers().lastHeader("event.id")).isNotNull();
    }

    @Test
    void shouldHandlePublishingFailure() {
        // Given
        String topicName = "domain.lending.loan.approved";
        TestEvent event = new TestEvent("loan-123", "APPROVED");
        String tenantId = "tenant-001";
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        RuntimeException exception = new RuntimeException("Kafka unavailable");
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        CompletableFuture<SendResult<String, Object>> result =
            producer.publishEvent(topicName, event, tenantId, correlationId);

        // Complete exceptionally
        future.completeExceptionally(exception);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCompletedExceptionally()).isTrue();

        // Verify error counter incremented
        assertThat(meterRegistry.counter("domain.events.published.failure").count())
            .isEqualTo(1.0);
    }

    @Test
    void shouldPublishEventWithCustomKey() {
        // Given
        String topicName = "domain.lending.loan.approved";
        String customKey = "custom-partition-key";
        TestEvent event = new TestEvent("loan-123", "APPROVED");
        String tenantId = "tenant-001";
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        producer.publishEventWithKey(topicName, customKey, event, tenantId, correlationId);

        // Then
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor =
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());

        ProducerRecord<String, Object> capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.key()).isEqualTo(customKey);
    }

    @Test
    void shouldPublishEventSynchronously() throws Exception {
        // Given
        String topicName = "domain.lending.loan.approved";
        TestEvent event = new TestEvent("loan-123", "APPROVED");
        String tenantId = "tenant-001";
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<SendResult<String, Object>> future =
            CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(100L);

        // When
        SendResult<String, Object> result =
            producer.publishEventSync(topicName, event, tenantId, correlationId, 5);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(sendResult);
    }

    @Test
    void shouldIncrementSuccessCounterOnSuccess() {
        // Given
        String topicName = "domain.lending.loan.approved";
        TestEvent event = new TestEvent("loan-123", "APPROVED");
        String tenantId = "tenant-001";
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<SendResult<String, Object>> future =
            CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);

        // When
        producer.publishEvent(topicName, event, tenantId, correlationId);

        // Then
        assertThat(meterRegistry.counter("domain.events.published.success").count())
            .isEqualTo(1.0);
    }

    /**
     * Test event class for testing
     */
    private static class TestEvent {
        private final String id;
        private final String status;

        public TestEvent(String id, String status) {
            this.id = id;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestEvent testEvent = (TestEvent) o;
            return id.equals(testEvent.id) && status.equals(testEvent.status);
        }

        @Override
        public int hashCode() {
            return id.hashCode() + status.hashCode();
        }
    }
}