package com.memoraai.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
public class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOpenApiSecurityConfig() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                // Verify global security requirement is 'bearerAuth'
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                // Verify security scheme 'bearerAuth' is defined correctly
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }
}
