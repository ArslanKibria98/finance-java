package com.ksa.financing.piivault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ksa.financing.piivault",
        "com.ksa.financing.infra.response"
})
public class PiiVaultServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PiiVaultServiceApplication.class, args);
    }
}
