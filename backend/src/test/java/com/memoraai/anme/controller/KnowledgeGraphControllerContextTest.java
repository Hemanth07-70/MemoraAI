package com.memoraai.anme.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class KnowledgeGraphControllerContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testControllerIsRegistered() {
        KnowledgeGraphController controller = applicationContext.getBean(KnowledgeGraphController.class);
        assertNotNull(controller);
        System.out.println("====== SUCCESS: KnowledgeGraphController IS IN CONTEXT ======");
    }
}
