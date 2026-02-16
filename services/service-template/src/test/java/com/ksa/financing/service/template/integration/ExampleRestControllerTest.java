package com.ksa.financing.service.template.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.service.template.adapter.rest.request.CreateExampleRequest;
import com.ksa.financing.service.template.adapter.rest.request.AddEntityRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * REST Controller integration test template.
 * Tests REST endpoints with mocked security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Example REST Controller Tests")
class ExampleRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @DisplayName("Should create example via REST API")
    @Transactional
    void shouldCreateExampleViaRestApi() throws Exception {
        // Given
        var request = new CreateExampleRequest();
        request.setName("Test Example");
        request.setDescription("Test Description");

        // When & Then
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Correlation-ID", "test-correlation-id"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Example"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(header().string("X-Correlation-ID", "test-correlation-id"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @DisplayName("Should get example by ID")
    @Transactional
    void shouldGetExampleById() throws Exception {
        // First create an example
        var createRequest = new CreateExampleRequest();
        createRequest.setName("Test Example");
        createRequest.setDescription("Description");

        var response = mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        var createdExample = objectMapper.readTree(response.getResponse().getContentAsString());
        var exampleId = createdExample.get("id").asText();

        // Then retrieve it
        mockMvc.perform(get("/api/v1/examples/{id}", exampleId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exampleId))
                .andExpect(jsonPath("$.name").value("Test Example"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @DisplayName("Should list examples")
    @Transactional
    void shouldListExamples() throws Exception {
        // Create multiple examples
        for (int i = 1; i <= 3; i++) {
            var request = new CreateExampleRequest();
            request.setName("Example " + i);
            request.setDescription("Description " + i);

            mockMvc.perform(post("/api/v1/examples")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // List all examples
        mockMvc.perform(get("/api/v1/examples"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/examples"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @DisplayName("Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - Invalid request with empty name
        var request = new CreateExampleRequest();
        request.setName("");  // Invalid - empty name
        request.setDescription("Description");

        // When & Then
        mockMvc.perform(post("/api/v1/examples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should allow access to health endpoint without authentication")
    void shouldAllowAccessToHealthEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Should provide OpenAPI documentation")
    void shouldProvideOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").exists());
    }
}