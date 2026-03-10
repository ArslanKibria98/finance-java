package com.ksa.financing.identity.infrastructure.casbin;

import lombok.extern.slf4j.Slf4j;
import org.casbin.adapter.JDBCAdapter;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class CasbinConfig {

    @Value("${casbin.model-path:casbin/model.conf}")
    private String modelPath;

    @Bean
    public Enforcer casbinEnforcer(DataSource dataSource) throws Exception {
        var modelResource = new ClassPathResource(modelPath);
        String modelText;
        try (var reader = new BufferedReader(new InputStreamReader(modelResource.getInputStream(), StandardCharsets.UTF_8))) {
            modelText = reader.lines().collect(Collectors.joining("\n"));
        }

        var model = new Model();
        model.loadModelFromText(modelText);

        var adapter = new JDBCAdapter(dataSource);
        var enforcer = new Enforcer(model, adapter);

        enforcer.addFunction("pathMatch", new PathMatchFunction());

        enforcer.loadPolicy();
        log.info("Casbin enforcer initialized with {} policies from database", enforcer.getPolicy().size());

        return enforcer;
    }
}
