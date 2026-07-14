package com.memoraai.chunking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "memoraai.chunking")
public class ChunkingProperties {
    private int chunkSize = 1000;
    private int overlap = 200;
}
