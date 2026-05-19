package com.ksa.financing.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ksa.financing.onboarding",
        "com.ksa.islamic.orchestration.config",
        "com.ksa.financing.infra.response"
})
public class OnboardingWorkflowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnboardingWorkflowServiceApplication.class, args);
    }
}
