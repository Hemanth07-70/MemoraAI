package com.memoraai.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chat.dto.OllamaResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OllamaServiceTest {

    private MockWebServer mockWebServer;
    private OllamaService ollamaService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();
        ollamaService = new OllamaService(webClientBuilder, baseUrl, "llama3", Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnAnswerSuccessfully() throws Exception {
        OllamaResponse response = new OllamaResponse();
        response.setResponse("This is the LLM answer.");

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(response))
                .addHeader("Content-Type", "application/json"));

        String answer = ollamaService.generateAnswer("Test prompt").block();
        assertThat(answer).isEqualTo("This is the LLM answer.");
    }

    @Test
    void shouldHandleTimeoutAndRetry() {
        // Enqueue 2 timeouts, meaning it fails both initially and on the single retry
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ollamaService.generateAnswer("Test prompt").block();
        });

        assertThat(exception.getMessage()).contains("Ollama service unavailable or failed");
    }
}
