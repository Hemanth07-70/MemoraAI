package com.memoraai.chat.service;

import reactor.core.publisher.Mono;

public interface LLMService {
    
    /**
     * Generates an answer from the underlying LLM provider based on the provided prompt.
     * @param prompt The formatted prompt containing context and the user question.
     * @return A Mono emitting the generated string response.
     */
    Mono<String> generateAnswer(String prompt);

    /**
     * @return The name of the LLM provider (e.g., "nemotron", "ollama").
     */
    String getProviderName();

    /**
     * @return The specific model being used (e.g., "nvidia/nemotron-3-super", "llama3").
     */
    String getModelName();
}
