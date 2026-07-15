package com.memoraai.anme.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
public class MappingCheckTest {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    public void printMappings() {
        System.out.println("====== START MAPPINGS ======");
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            if (entry.getValue().getBeanType().getName().contains("KnowledgeGraphController")) {
                System.out.println("FOUND: " + entry.getKey() + " -> " + entry.getValue().getMethod().getName());
            }
        }
        System.out.println("====== END MAPPINGS ======");
    }
}
