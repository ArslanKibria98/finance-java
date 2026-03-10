package com.ksa.financing.infra.autoconfigure;

import com.ksa.financing.infra.exception.GlobalExceptionHandler;
import com.ksa.financing.infra.response.ApiResponseAdvice;
import com.ksa.financing.infra.security.JsonAccessDeniedHandler;
import com.ksa.financing.infra.security.JsonAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@Import({ApiResponseAdvice.class})
@EnableConfigurationProperties(ErrorMessageProperties.class)
public class FoundationalInfraAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "errorMessageSource")
    public MessageSource errorMessageSource(ErrorMessageProperties props) {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();

        List<String> basenames = new ArrayList<>();
        if (props.getAdditionalBasenames() != null) {
            basenames.addAll(props.getAdditionalBasenames());
        }
        basenames.add("errors/errors");

        source.setBasenames(basenames.toArray(String[]::new));
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(false);
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler(
            @Qualifier("errorMessageSource") MessageSource errorMessageSource) {
        return new GlobalExceptionHandler(errorMessageSource);
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint(
            @Qualifier("errorMessageSource") MessageSource errorMessageSource) {
        return new JsonAuthenticationEntryPoint(errorMessageSource);
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public JsonAccessDeniedHandler jsonAccessDeniedHandler(
            @Qualifier("errorMessageSource") MessageSource errorMessageSource) {
        return new JsonAccessDeniedHandler(errorMessageSource);
    }
}
