package com.ksa.financing.storage.autoconfigure;

import com.ksa.financing.storage.adapter.MinioFileStorageAdapter;
import com.ksa.financing.storage.config.FileStorageProperties;
import com.ksa.financing.storage.port.FileStoragePort;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for file storage.
 * Auto-activates when minio.enabled=true in application.yml.
 *
 * Services just need to add the dependency — no manual @Configuration required.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class FileStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(FileStorageProperties props) {
        log.info("Configuring MinIO client: endpoint={} bucket={}", props.getEndpoint(), props.getBucket());
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(FileStoragePort.class)
    public FileStoragePort fileStoragePort(MinioClient minioClient, FileStorageProperties props) {
        return new MinioFileStorageAdapter(minioClient, props);
    }
}
