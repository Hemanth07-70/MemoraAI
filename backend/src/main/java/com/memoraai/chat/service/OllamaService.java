package com.memoraai.chat.service;

import com.memoraai.chat.dto.OllamaRequest;
import com.memoraai.chat.dto.OllamaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
public class OllamaService implements LLMService {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public OllamaService(
            WebClient.Builder webClientBuilder,
            @Value("${memoraai.ai.ollama-url:http://localhost:11434}") String ollamaUrl,
            @Value("${memoraai.ai.model:llama3}") String model,
            @Value("${memoraai.ai.timeout:60s}") Duration timeout) {
        this.webClient = webClientBuilder.baseUrl(ollamaUrl).build();
        this.model = model;
        this.timeout = timeout;
    }

    public Mono<String> generateAnswer(String prompt) {
        OllamaRequest request = OllamaRequest.builder()
                .model(this.model)
                .prompt(prompt)
                .stream(false)
                .build();

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable ||
                                throwable instanceof WebClientResponseException.GatewayTimeout ||
                                throwable instanceof java.util.concurrent.TimeoutException))
                .map(OllamaResponse::getResponse)
                .onErrorResume(e -> {
                    log.error("Failed to generate answer from Ollama: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Ollama service unavailable or failed: " + e.getMessage(), e));
                });
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public String getModelName() {
        return this.model;
    }
}
