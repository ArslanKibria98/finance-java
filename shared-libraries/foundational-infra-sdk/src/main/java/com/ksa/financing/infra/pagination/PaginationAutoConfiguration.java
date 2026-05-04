package com.ksa.financing.infra.pagination;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Auto-registers {@link PageQueryArgumentResolver} so any controller method
 * declaring a {@link PageQuery} parameter receives a fully-resolved instance
 * built from the request query string.
 *
 * <p>Activated automatically for any servlet web app that pulls in this SDK.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(PaginationProperties.class)
public class PaginationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PageQueryArgumentResolver pageQueryArgumentResolver(PaginationProperties props) {
        return new PageQueryArgumentResolver(props);
    }

    @Bean
    public WebMvcConfigurer paginationWebMvcConfigurer(PageQueryArgumentResolver resolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(resolver);
            }
        };
    }
}
