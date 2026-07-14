package com.memoraai.chat.service;

import com.memoraai.search.dto.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderServiceTest {

    private PromptBuilderService promptBuilderService;

    @BeforeEach
    void setUp() {
        promptBuilderService = new PromptBuilderService();
    }

    @Test
    void shouldBuildPromptCorrectly() {
        SearchResultItem item1 = SearchResultItem.builder()
                .documentId(UUID.randomUUID())
                .chunkId(UUID.randomUUID())
                .chunkIndex(0)
                .text("This is the first piece of context.")
                .score(0.95)
                .build();

        SearchResultItem item2 = SearchResultItem.builder()
                .documentId(UUID.randomUUID())
                .chunkId(UUID.randomUUID())
                .chunkIndex(1)
                .text("This is the second piece of context.")
                .score(0.85)
                .build();

        String question = "What is the context?";
        List<SearchResultItem> chunks = List.of(item1, item2);
        String prompt = promptBuilderService.buildPrompt(question, chunks, null);

        assertThat(prompt).contains("You are MemoraAI, an AI study assistant.");
        assertThat(prompt).contains("Chunk [0]: This is the first piece of context.");
        assertThat(prompt).contains("Chunk [1]: This is the second piece of context.");
        assertThat(prompt).contains(question);
    }
}
