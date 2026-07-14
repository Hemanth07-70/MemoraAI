package com.memoraai.chat.service;

import com.memoraai.search.dto.SearchResultItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromptBuilderService {

    private static final String SYSTEM_PROMPT = """
            You are MemoraAI, an AI study assistant.
            Answer ONLY using the supplied context.
            Do NOT use outside knowledge.
            If the answer cannot be found inside the provided context, reply exactly:
            "I couldn't find this information in the uploaded documents."
            """;

    public String buildPrompt(String question, List<SearchResultItem> retrievedChunks, List<com.memoraai.conversation.dto.ChatMessageDto> history) {
        String contextStr = retrievedChunks.stream()
                .map(chunk -> String.format("Chunk [%d]: %s", chunk.getChunkIndex(), chunk.getText()))
                .collect(Collectors.joining("\n\n"));

        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT).append("\n\n");
        prompt.append("Context\n").append(contextStr).append("\n\n");

        if (history != null && !history.isEmpty()) {
            prompt.append("Conversation History\n");
            for (com.memoraai.conversation.dto.ChatMessageDto msg : history) {
                prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("Question\n").append(question).append("\n\nAnswer\n");
        return prompt.toString();
    }
}
