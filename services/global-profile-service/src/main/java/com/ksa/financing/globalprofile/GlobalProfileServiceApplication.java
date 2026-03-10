package com.ksa.financing.globalprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
        "com.ksa.financing.globalprofile",
        "com.ksa.financing.infra.response"
})
public class GlobalProfileServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlobalProfileServiceApplication.class, args);
    }
}
