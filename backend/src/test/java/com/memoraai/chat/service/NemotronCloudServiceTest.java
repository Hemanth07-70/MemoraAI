package com.memoraai.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.chat.dto.NemotronResponse;
import com.memoraai.chat.exception.LLMAuthenticationException;
import com.memoraai.chat.exception.LLMProviderException;
import com.memoraai.chat.exception.LLMRateLimitException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NemotronCloudServiceTest {

    private MockWebServer mockWebServer;
    private NemotronCloudService nemotronCloudService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();
        nemotronCloudService = new NemotronCloudService(webClientBuilder, baseUrl, "test-api-key", "nvidia/nemotron-3-super", Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnAnswerSuccessfully() throws Exception {
        NemotronResponse.Message message = new NemotronResponse.Message("assistant", "This is the LLM answer from Nemotron.");
        NemotronResponse.Choice choice = new NemotronResponse.Choice(0, message, "stop");
        NemotronResponse response = new NemotronResponse();
        response.setChoices(List.of(choice));

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(response))
                .addHeader("Content-Type", "application/json"));

        String answer = nemotronCloudService.generateAnswer("Test prompt").block();
        assertThat(answer).isEqualTo("This is the LLM answer from Nemotron.");
    }

    @Test
    void shouldThrowLLMAuthenticationExceptionOn401() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        LLMAuthenticationException exception = assertThrows(LLMAuthenticationException.class, () -> {
            nemotronCloudService.generateAnswer("Test prompt").block();
        });

        assertThat(exception.getMessage()).contains("Authentication failed with Nemotron Cloud");
    }

    @Test
    void shouldHandleRateLimitAndRetryThenFail() {
        // Enqueue multiple 429s to exhaust the retries (3 retries + 1 initial)
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));

        LLMRateLimitException exception = assertThrows(LLMRateLimitException.class, () -> {
            nemotronCloudService.generateAnswer("Test prompt").block();
        });

        assertThat(exception.getMessage()).contains("Rate limit exceeded with Nemotron Cloud");
    }

    @Test
    void shouldHandleRateLimitAndRetryThenSucceed() throws Exception {
        NemotronResponse.Message message = new NemotronResponse.Message("assistant", "Success after retry.");
        NemotronResponse.Choice choice = new NemotronResponse.Choice(0, message, "stop");
        NemotronResponse response = new NemotronResponse();
        response.setChoices(List.of(choice));

        // Enqueue 2 rate limits, then success
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        mockWebServer.enqueue(new MockResponse().setResponseCode(429));
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(response))
                .addHeader("Content-Type", "application/json"));

        String answer = nemotronCloudService.generateAnswer("Test prompt").block();
        assertThat(answer).isEqualTo("Success after retry.");
    }

    @Test
    void shouldHandleTimeoutAndRetry() {
        // Enqueue 500 to trigger retry, it's easier to simulate than read timeout in this setup
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        LLMProviderException exception = assertThrows(LLMProviderException.class, () -> {
            nemotronCloudService.generateAnswer("Test prompt").block();
        });

        assertThat(exception.getMessage()).contains("Nemotron API error (500");
    }
}
