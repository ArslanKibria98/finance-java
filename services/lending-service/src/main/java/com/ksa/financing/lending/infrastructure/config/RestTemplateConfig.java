package com.ksa.financing.lending.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Trust all certs for dev (Fineract uses self-signed HTTPS)
        var requestFactory = new SslTrustAllRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        return new RestTemplate(requestFactory);
    }

    /**
     * Request factory that disables SSL verification for HTTPS connections.
     * Required for dev/test environments where Fineract uses a self-signed certificate.
     */
    static class SslTrustAllRequestFactory extends SimpleClientHttpRequestFactory {

        private static final TrustManager[] TRUST_ALL = {
                new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };

        private static final SSLSocketFactory SSL_FACTORY;

        static {
            try {
                var sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, TRUST_ALL, new java.security.SecureRandom());
                SSL_FACTORY = sslContext.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new IllegalStateException("Failed to create trust-all SSLContext", e);
            }
        }

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            if (connection instanceof HttpsURLConnection https) {
                https.setSSLSocketFactory(SSL_FACTORY);
                https.setHostnameVerifier((hostname, session) -> true);
            }
            super.prepareConnection(connection, httpMethod);
        }
    }
}
