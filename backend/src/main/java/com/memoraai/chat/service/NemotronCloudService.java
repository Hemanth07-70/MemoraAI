package com.memoraai.chat.service;

import com.memoraai.chat.dto.NemotronRequest;
import com.memoraai.chat.dto.NemotronResponse;
import com.memoraai.chat.exception.LLMAuthenticationException;
import com.memoraai.chat.exception.LLMProviderException;
import com.memoraai.chat.exception.LLMRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
public class NemotronCloudService implements LLMService {

    private final WebClient webClient;
    private final String model;
    private final Duration timeout;

    public NemotronCloudService(
            WebClient.Builder webClientBuilder,
            @Value("${memoraai.ai.nemotron.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${memoraai.ai.nemotron.api-key}") String apiKey,
            @Value("${memoraai.ai.nemotron.model:nvidia/nemotron-3-super}") String model,
            @Value("${memoraai.ai.nemotron.timeout:60s}") Duration timeout) {
        
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
        this.timeout = timeout;
    }

    @Override
    public Mono<String> generateAnswer(String prompt) {
        NemotronRequest request = NemotronRequest.builder()
                .model(this.model)
                .messages(List.of(
                        NemotronRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .temperature(0.0)
                .build();

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NemotronResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isTransientError))
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    throw new LLMProviderException("Empty choices in Nemotron response");
                })
                .onErrorMap(this::mapException);
    }

    @Override
    public String getProviderName() {
        return "nemotron";
    }

    @Override
    public String getModelName() {
        return this.model;
    }

    private boolean isTransientError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wce) {
            return wce.getStatusCode().is5xxServerError() || 
                   wce.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
        }
        return throwable instanceof java.util.concurrent.TimeoutException;
    }

    private Throwable mapException(Throwable throwable) {
        Throwable cause = throwable;
        if (reactor.core.Exceptions.isRetryExhausted(throwable)) {
            cause = throwable.getCause();
        }

        if (cause instanceof LLMProviderException) {
            return cause;
        }
        
        if (cause instanceof WebClientResponseException wce) {
            if (wce.getStatusCode() == HttpStatus.UNAUTHORIZED || wce.getStatusCode() == HttpStatus.FORBIDDEN) {
                return new LLMAuthenticationException("Authentication failed with Nemotron Cloud: " + wce.getMessage(), wce);
            }
            if (wce.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return new LLMRateLimitException("Rate limit exceeded with Nemotron Cloud: " + wce.getMessage(), wce);
            }
            return new LLMProviderException("Nemotron API error (" + wce.getStatusCode() + "): " + wce.getResponseBodyAsString(), wce);
        }
        
        if (cause instanceof java.util.concurrent.TimeoutException) {
            return new LLMProviderException("Nemotron API request timed out", cause);
        }

        return new LLMProviderException("Unexpected error calling Nemotron Cloud", cause);
    }
}
