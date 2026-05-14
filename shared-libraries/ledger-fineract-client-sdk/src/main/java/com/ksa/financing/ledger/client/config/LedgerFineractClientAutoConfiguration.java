package com.ksa.financing.ledger.client.config;

import com.ksa.financing.ledger.client.savings.LedgerFineractSavingsClient;
import com.ksa.financing.ledger.client.savings.RestLedgerFineractSavingsClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configures {@link LedgerFineractSavingsClient} and a dedicated
 * {@code ledgerClientRestTemplate}.
 * <p>
 * Activated automatically when the SDK is on the classpath. Override individual beans
 * by declaring a {@code @Bean} of the same name in the consuming service.
 */
@AutoConfiguration
@EnableConfigurationProperties(LedgerClientProperties.class)
public class LedgerFineractClientAutoConfiguration {

    @Bean(name = "ledgerClientRestTemplate")
    @ConditionalOnMissingBean(name = "ledgerClientRestTemplate")
    public RestTemplate ledgerClientRestTemplate(LedgerClientProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    @Bean
    @ConditionalOnMissingBean
    public LedgerFineractSavingsClient ledgerFineractSavingsClient(
            @Qualifier("ledgerClientRestTemplate") RestTemplate restTemplate,
            LedgerClientProperties props) {
        return new RestLedgerFineractSavingsClient(restTemplate, props);
    }
}
