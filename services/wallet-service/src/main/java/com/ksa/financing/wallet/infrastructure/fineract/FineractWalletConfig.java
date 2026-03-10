package com.ksa.financing.wallet.infrastructure.fineract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "fineract")
public class FineractWalletConfig {

    private static final Logger log = LoggerFactory.getLogger(FineractWalletConfig.class);

    private boolean enabled = false;
    private String baseUrl = "https://localhost:8443/fineract-provider/api/v1";
    private String username = "mifos";
    private String password = "password";
    private String tenantId = "default";
    private int savingsProductId = 1;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;

    @Bean("fineractWalletRestTemplate")
    public RestTemplate fineractWalletRestTemplate() throws Exception {
        // Trust all certificates for Fineract self-signed HTTPS (dev environment)
        TrustManager[] trustAll = { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] certs, String type) { }
            public void checkServerTrusted(X509Certificate[] certs, String type) { }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new java.security.SecureRandom());

        var factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection httpsConn) {
                    httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                    httpsConn.setHostnameVerifier((hostname, session) -> true);
                }
                super.prepareConnection(connection, httpMethod);
            }
        };
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        var restTemplate = new RestTemplate(factory);

        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().set("Authorization", "Basic " + credentials);
            request.getHeaders().set("Fineract-Platform-TenantId", tenantId);
            request.getHeaders().set("Content-Type", "application/json");
            request.getHeaders().set("Accept", "application/json");
            return execution.execute(request, body);
        }));

        log.info("Fineract RestTemplate configured with SSL trust-all for base-url={}", baseUrl);
        return restTemplate;
    }

    // --- Getters and Setters ---

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public int getSavingsProductId() { return savingsProductId; }
    public void setSavingsProductId(int savingsProductId) { this.savingsProductId = savingsProductId; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
