# Messaging & Event SDK

## Overview

The Messaging & Event SDK provides comprehensive support for event-driven architecture using Apache Kafka, gRPC communication, and schema management with Confluent Schema Registry. This SDK implements the event streaming patterns required for the Islamic financing platform.

## Features

### Kafka Integration
- **Idempotent Producer**: Ensures exactly-once semantics for event publishing
- **Manual Acknowledgment Consumer**: Provides fine-grained control over message processing
- **Dead Letter Queue (DLQ)**: Automatic routing of failed messages after configurable retries
- **Transactional Outbox Pattern**: Guaranteed event delivery with database-backed persistence

### Schema Management
- **Avro Schemas**: Strongly-typed event contracts with schema evolution support
- **Schema Registry Integration**: Centralized schema validation and compatibility checking
- **Event Envelope Pattern**: Consistent metadata wrapper for all domain events

### gRPC Support
- **Service Definitions**: Protobuf-based service contracts for internal communication
- **Interceptors**: Authentication, tenant context, and logging interceptors
- **Client/Server Configuration**: Load balancing and resilience patterns

## Architecture

```
messaging-event-sdk/
├── kafka/
│   ├── config/          # Kafka configuration classes
│   ├── producer/        # Domain event and outbox producers
│   └── consumer/        # Base consumers with retry logic
├── grpc/
│   ├── config/          # gRPC server and client configuration
│   └── interceptor/     # Authentication and context interceptors
├── avro/                # Generated Avro classes
├── proto/               # Generated Protobuf classes
├── contract/            # Event contracts and envelopes
└── exception/           # Custom exceptions
```

## Configuration

### Application Properties

```yaml
# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: islamic-financing-consumer
      enable-auto-commit: false
      auto-offset-reset: earliest
    producer:
      acks: all
      retries: 2147483647
      enable-idempotence: true

# Schema Registry
kafka:
  schema-registry:
    url: http://localhost:8081
    cache-size: 1000
    auto-register-schemas: true
    compatibility-level: BACKWARD

# Dead Letter Queue
kafka:
  dlq:
    topic-prefix: dlq.
    max-retries: 3
    retry-interval-ms: 5000

# gRPC Configuration
grpc:
  server:
    port: 9090
  client:
    lending-service:
      address: static://localhost:9090
      enable-keep-alive: true
```

## Usage Examples

### Publishing Domain Events

```java
@Service
public class LoanService {

    @Autowired
    private DomainEventProducer eventProducer;

    public void approveLoan(Loan loan) {
        // Business logic
        loan.approve();

        // Publish event
        LoanApprovedEvent event = new LoanApprovedEvent(
            loan.getId(),
            loan.getAmount(),
            loan.getCustomerId()
        );

        eventProducer.publishEvent(
            "domain.lending.loan.approved",
            event,
            loan.getTenantId(),
            MDC.get("correlationId")
        );
    }
}
```

### Consuming Domain Events

```java
@Component
public class LoanEventConsumer extends DomainEventConsumer<LoanEvent> {

    public LoanEventConsumer(MeterRegistry meterRegistry) {
        super(LoanEvent.class, meterRegistry);
    }

    @Override
    @KafkaListener(
        topics = "domain.lending.loan.*",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    protected void processEvent(LoanEvent event, EventEnvelope envelope) {
        // Process the event
        switch (event.getEventType()) {
            case APPLICATION_SUBMITTED:
                handleApplicationSubmitted(event);
                break;
            case LOAN_APPROVED:
                handleLoanApproved(event);
                break;
            // ... other cases
        }
    }
}
```

### Using Transactional Outbox Pattern

```java
@Service
@Transactional
public class PaymentService {

    @Autowired
    private TransactionalOutboxProducer outboxProducer;

    public void processPayment(Payment payment) {
        // Save payment to database
        paymentRepository.save(payment);

        // Publish event via outbox pattern (guaranteed delivery)
        PaymentProcessedEvent event = new PaymentProcessedEvent(payment);

        outboxProducer.executeOutboxPattern(
            event,
            "domain.payment.payment.processed",
            payment.getId(),
            payment.getTenantId()
        );
    }
}
```

### gRPC Service Implementation

```java
@GrpcService
public class LendingServiceImpl extends LendingServiceGrpc.LendingServiceImplBase {

    @Override
    public void applyForLoan(
            LoanApplicationRequest request,
            StreamObserver<LoanApplicationResponse> responseObserver) {

        // Process loan application
        String applicationId = loanService.submitApplication(request);

        // Send response
        LoanApplicationResponse response = LoanApplicationResponse.newBuilder()
            .setApplicationId(applicationId)
            .setStatus("SUBMITTED")
            .setMessage("Application submitted successfully")
            .setSubmittedAt(Timestamp.now())
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

## Event Flow Patterns

### 1. Fire-and-Forget Notifications
```
Service → Kafka → Consumer → Process
```

### 2. Transactional Outbox
```
Service → DB Transaction → Outbox Table → Kafka → Consumer
```

### 3. Dead Letter Queue
```
Consumer → Retry (3x) → DLQ Topic → DLQ Handler → Manual Investigation
```

## Monitoring

The SDK provides comprehensive metrics via Micrometer:

- **Producer Metrics**:
  - `domain.events.published.success` - Successful publishes
  - `domain.events.published.failure` - Failed publishes
  - `domain.events.publish.duration` - Publishing duration

- **Consumer Metrics**:
  - `domain.events.consumed.success` - Successful consumptions
  - `domain.events.consumed.error` - Failed consumptions
  - `domain.events.processing.duration` - Processing duration

- **DLQ Metrics**:
  - `dlq.messages.received` - Messages received in DLQ
  - `dlq.messages.processed` - Successfully processed DLQ messages
  - `dlq.messages.requeued` - Messages requeued from DLQ

## Testing

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class DomainEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldPublishEventSuccessfully() {
        // Test implementation
    }
}
```

### Integration Testing

```java
@SpringBootTest
@EmbeddedKafka(
    topics = {"test-topic"},
    partitions = 1
)
class KafkaIntegrationTest {

    @Test
    void shouldProduceAndConsumeEvents() {
        // Test implementation
    }
}
```

## Best Practices

1. **Event Naming**: Use domain-driven naming: `domain.<context>.<aggregate>.<event>`
2. **Idempotency**: Always include idempotency keys in events
3. **Schema Evolution**: Follow backward compatibility for schema changes
4. **Error Handling**: Distinguish between recoverable and non-recoverable errors
5. **Monitoring**: Use correlation IDs for distributed tracing
6. **Testing**: Test schema evolution and DLQ scenarios

## Dependencies

- Apache Kafka 3.9.1
- Confluent Schema Registry 7.8.0
- Apache Avro 1.12.0
- gRPC 1.69.0
- Protocol Buffers 4.29.3
- Spring Kafka 3.3.0

## Build

```bash
# Build the SDK
mvn clean install -pl shared-libraries/messaging-event-sdk

# Run tests
mvn test -pl shared-libraries/messaging-event-sdk

# Generate Avro and Protobuf classes
mvn generate-sources -pl shared-libraries/messaging-event-sdk
```

## Next Steps

After implementing this SDK, proceed to:
- **Prompt 04**: Workflow Orchestration SDK implementation
- Integrate with domain services for event publishing
- Set up Kafka cluster and Schema Registry