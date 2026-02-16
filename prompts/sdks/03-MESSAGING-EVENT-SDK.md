# 📬 Prompt 03: Messaging & Event SDK Implementation

**Objective**: Implement the `messaging-event-sdk` for Kafka event streaming, gRPC communication, and schema management.

**Prerequisites**:
- ✅ Prompt 00-02 complete

---

## 📚 Reference Documents

**IMPORTANT**: Read these documents thoroughly before proceeding.

### Master Blueprint
- `/var/www/docs/islamic-financing/master-blueprint/01_ARCHITECTURE_PRINCIPLES.md`
  - Section: Event-Driven Architecture
  - Section: API Design Principles

- `/var/www/docs/islamic-financing/master-blueprint/02_TECHNOLOGY_STACK.md`
  - Section: Message Broker - Apache Kafka 3.9.1
  - Section: gRPC configuration

- `/var/www/docs/islamic-financing/master-blueprint/06_ORCHESTRATION_PATTERNS.md`
  - Event flow patterns
  - Async communication patterns

---

## 🎯 Implementation Requirements

### Technologies
- **Kafka**: 3.9.1
- **Confluent Schema Registry**: 7.8.0
- **Avro**: 1.12.0
- **gRPC**: 1.69.0
- **Protobuf**: 4.29.3

### What to Implement

#### 1. Kafka Configuration (in `kafka/config/`)
- `KafkaProducerConfig` - Idempotent producer with Avro serialization
- `KafkaConsumerConfig` - Manual acknowledgment, Avro deserialization
- `SchemaRegistryConfig` - Confluent Schema Registry integration

#### 2. Kafka Producer (in `kafka/producer/`)
- `DomainEventProducer` - Publish domain events with event envelope
- `TransactionalOutboxProducer` - Transactional outbox pattern support
- Topic naming strategy: `domain.<context>.<aggregate>.<event>`

#### 3. Kafka Consumer (in `kafka/consumer/`)
- `DomainEventConsumer` - Base consumer with error handling
- `DeadLetterQueueHandler` - DLQ routing after max retries (3 attempts)
- `RetryableMessageListener` - Exponential backoff retry

#### 4. Avro Schemas (in `avro/`)
Create schemas for:
- `event_envelope.avsc` - Wrapper with tenantId, correlationId, causationId
- `loan_event.avsc` - Loan lifecycle events
- `customer_event.avsc` - Customer events
- `payment_event.avsc` - Payment events
- All schemas must include `version` field for evolution

#### 5. Protobuf Definitions (in `proto/`)
- `common.proto` - Shared messages (Money, ErrorResponse, Pagination)
- `lending_service.proto` - Lending service gRPC API
- `customer_service.proto` - Customer service gRPC API
- `sharia_service.proto` - Sharia compliance gRPC API

#### 6. gRPC Configuration (in `grpc/config/`)
- `GrpcServerConfig` - Server on port 9090 with interceptors
- `GrpcClientConfig` - Client factory with load balancing
- Interceptors:
  - `AuthenticationInterceptor` - JWT validation
  - `TenantContextInterceptor` - Extract tenant from metadata
  - `LoggingInterceptor` - Structured logging

#### 7. Event Contracts (in `contract/`)
- `EventEnvelope` - Standard wrapper for all events
- Contract interfaces for each event type
- Versioning support

---

## 🧪 Testing Requirements

- Kafka producer/consumer integration tests using `@EmbeddedKafka`
- Schema evolution compatibility tests
- gRPC client/server tests with in-process server
- Dead Letter Queue handling tests

---

## ✅ Success Criteria

- [ ] Kafka producer sends events with Avro serialization
- [ ] Schema Registry validates schemas on produce
- [ ] Consumer processes events with manual ack
- [ ] DLQ handles failed messages after 3 retries
- [ ] gRPC services expose APIs with interceptors
- [ ] All Avro schemas compile to Java classes
- [ ] All Protobuf definitions generate gRPC stubs
- [ ] Tests pass: `mvn test -pl shared-libraries/messaging-event-sdk`
- [ ] Build succeeds: `mvn clean install -pl shared-libraries/messaging-event-sdk`

---

## 🔄 Next Step

After completing this SDK, proceed to:
- **Prompt 04**: `workflow-orchestration-sdk` implementation
