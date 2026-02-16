package com.ksa.islamic.orchestration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ksa.islamic.domain.core.model.Money;
import io.temporal.common.converter.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Data Converter Configuration for Temporal
 *
 * Configures custom serialization for domain objects like Money, Date/Time objects,
 * and other complex domain entities.
 */
@Slf4j
@Configuration
public class DataConverterConfig {

    /**
     * Creates a custom ObjectMapper for Temporal serialization
     */
    @Bean
    @ConditionalOnMissingBean(name = "temporalObjectMapper")
    public ObjectMapper temporalObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register modules for Java 8 time and Optional support
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());

        // Configure serialization features
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // Register custom serializers/deserializers for domain objects
        mapper.registerModule(new DomainObjectModule());

        log.info("Configured Temporal ObjectMapper with domain object support");
        return mapper;
    }

    /**
     * Creates the default data converter with custom Jackson configuration
     */
    @Bean
    @ConditionalOnMissingBean
    public DataConverter dataConverter(ObjectMapper temporalObjectMapper) {
        log.info("Creating DataConverter with custom ObjectMapper");

        // Create Jackson JSON payload converter with our custom mapper
        JacksonJsonPayloadConverter jacksonConverter = new JacksonJsonPayloadConverter(temporalObjectMapper);

        // Build the data converter with our custom converter first
        DataConverter dataConverter = DefaultDataConverter.newDefaultInstance()
                .withPayloadConverterOverrides(jacksonConverter);

        return dataConverter;
    }

    /**
     * Jackson module for domain object serialization
     */
    private static class DomainObjectModule extends com.fasterxml.jackson.databind.module.SimpleModule {
        public DomainObjectModule() {
            super("DomainObjectModule");

            // Add custom serializers for domain objects
            addSerializer(Money.class, new MoneySerializer());
            addDeserializer(Money.class, new MoneyDeserializer());
        }
    }

    /**
     * Custom serializer for Money domain object
     */
    private static class MoneySerializer extends com.fasterxml.jackson.databind.JsonSerializer<Money> {
        @Override
        public void serialize(Money value,
                            com.fasterxml.jackson.core.JsonGenerator gen,
                            com.fasterxml.jackson.databind.SerializerProvider serializers)
                            throws java.io.IOException {
            gen.writeStartObject();
            gen.writeNumberField("amount", value.getAmount());
            gen.writeStringField("currency", value.getCurrency().getCurrencyCode());
            gen.writeEndObject();
        }
    }

    /**
     * Custom deserializer for Money domain object
     */
    private static class MoneyDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Money> {
        @Override
        public Money deserialize(com.fasterxml.jackson.core.JsonParser p,
                                com.fasterxml.jackson.databind.DeserializationContext ctxt)
                                throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
            java.math.BigDecimal amount = node.get("amount").decimalValue();
            String currencyCode = node.get("currency").asText();
            return Money.of(amount, java.util.Currency.getInstance(currencyCode));
        }
    }
}