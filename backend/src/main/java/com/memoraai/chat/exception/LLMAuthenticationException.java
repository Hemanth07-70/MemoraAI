package com.memoraai.chat.exception;

public class LLMAuthenticationException extends LLMProviderException {
    public LLMAuthenticationException(String message) {
        super(message);
    }

    public LLMAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
