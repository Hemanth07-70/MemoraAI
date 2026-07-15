package com.memoraai;

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
public class ActuatorCheckTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void dumpActuator() throws Exception {
        MvcResult result = mockMvc.perform(get("/actuator/mappings"))
                .andReturn();
                
        String content = result.getResponse().getContentAsString();
        System.out.println("====== ACTUATOR STATUS: " + result.getResponse().getStatus() + " ======");
        System.out.println(content.substring(0, Math.min(content.length(), 200)));
    }
}
