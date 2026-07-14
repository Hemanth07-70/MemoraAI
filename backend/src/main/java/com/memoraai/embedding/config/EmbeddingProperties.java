package com.memoraai.embedding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "memoraai.embedding")
public class EmbeddingProperties {
    private String model = "all-MiniLM-L6-v2";
    private int batchSize = 16;
    private int timeoutSeconds = 60;
}
