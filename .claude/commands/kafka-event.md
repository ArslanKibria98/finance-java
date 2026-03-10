# Create Kafka Event Pipeline

You are the **Event Pipeline Engineer**. Create Kafka event topics, producers, and consumers following platform patterns.

## Input
- Event to create: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (messaging SDK section)
2. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md` (Kafka naming rules)
3. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (Kafka env vars)
4. `/var/www/islamic-financing-platform/docs/standards/RESILIENCE_STANDARDS.md` (retry policy)
5. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/15_DATABASE_STRATEGY.md` (transactional outbox)

## Naming Convention (from NAMING_CONVENTIONS.md)
```
Topic:          financing.{aggregate}.{event-past-tense}
Dead Letter:    {topic}.DLT
Consumer Group: ${spring.application.name}
Avro Schema:    {AggregateEvent}.avsc
```

## Configuration (ZERO HARDCODING from ENVIRONMENT_CONFIG.md)
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
    properties:
      schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8082}
```

## Transactional Outbox Pattern (from Blueprint 15)
Save entity + event in SAME @Transactional. Poller publishes to Kafka.

## Checklist
- [ ] Topic name follows NAMING_CONVENTIONS.md
- [ ] Dead Letter Topic configured: {topic}.DLT
- [ ] Kafka config from env vars (ENVIRONMENT_CONFIG.md)
- [ ] Topic name NOT hardcoded in @KafkaListener (use ${kafka.topics.xxx})
- [ ] Transactional outbox pattern (Blueprint 15)
- [ ] Avro schema in `src/main/avro/`
- [ ] Consumer group = `${spring.application.name}`
- [ ] Idempotency check in consumer
- [ ] Retry policy per RESILIENCE_STANDARDS.md
- [ ] Correlation ID propagated via event envelope
