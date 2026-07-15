package com.memoraai.anme.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SwaggerCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void dumpSwagger() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
                
        String content = result.getResponse().getContentAsString();
        System.out.println("====== SWAGGER OUTPUT ======");
        if (content.contains("knowledge-graph")) {
            System.out.println("FOUND KNOWLEDGE GRAPH IN SWAGGER!");
        } else {
            System.out.println("NOT FOUND IN SWAGGER!");
        }
        System.out.println("====== SWAGGER OUTPUT END ======");
    }
}
