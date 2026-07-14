package com.memoraai.chat.exception;

public class LLMRateLimitException extends LLMProviderException {
    public LLMRateLimitException(String message) {
        super(message);
    }

    public LLMRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
