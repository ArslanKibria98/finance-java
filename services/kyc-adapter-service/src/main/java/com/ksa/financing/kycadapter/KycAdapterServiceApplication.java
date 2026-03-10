package com.ksa.financing.kycadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ksa.financing.kycadapter",
        "com.ksa.islamic.orchestration.config",
        "com.ksa.financing.infra.response"
})
public class KycAdapterServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KycAdapterServiceApplication.class, args);
    }
}
