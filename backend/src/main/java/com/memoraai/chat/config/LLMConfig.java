package com.memoraai.chat.config;

import com.memoraai.chat.service.LLMService;
import com.memoraai.chat.service.NemotronCloudService;
import com.memoraai.chat.service.OllamaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Configuration
public class LLMConfig {

    @Bean
    @ConditionalOnProperty(name = "memoraai.ai.provider", havingValue = "nemotron", matchIfMissing = true)
    public LLMService nemotronCloudService(
            WebClient.Builder webClientBuilder,
            @Value("${memoraai.ai.nemotron.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${memoraai.ai.nemotron.api-key:}") String apiKey,
            @Value("${memoraai.ai.nemotron.model:nvidia/nemotron-3-super-120b-a12b}") String model,
            @Value("${memoraai.ai.nemotron.timeout:60s}") Duration timeout) {
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("NEMOTRON_API_KEY is required when LLM_PROVIDER=nemotron");
        }
        
        log.info("Configured AI provider: nemotron");
        log.info("Using LLM implementation: NemotronCloudService");
        return new NemotronCloudService(webClientBuilder, baseUrl, apiKey, model, timeout);
    }

    @Bean
    @ConditionalOnProperty(name = "memoraai.ai.provider", havingValue = "ollama")
    public LLMService ollamaService(
            WebClient.Builder webClientBuilder,
            @Value("${memoraai.ai.ollama.base-url:http://localhost:11434}") String ollamaUrl,
            @Value("${memoraai.ai.ollama.model:llama3}") String model,
            @Value("${memoraai.ai.ollama.timeout:60s}") Duration timeout) {
        
        log.info("Configured AI provider: ollama");
        log.info("Using LLM implementation: OllamaService");
        return new OllamaService(webClientBuilder, ollamaUrl, model, timeout);
    }
}
