package com.memoraai.chunking.service;

import com.memoraai.chunking.config.ChunkingProperties;
import com.memoraai.chunking.dto.ChunkStatisticsResponse;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkingService {

    private final DocumentChunkRepository documentChunkRepository;
    private final ChunkingProperties chunkingProperties;

    @Transactional
    public ChunkStatisticsResponse chunkDocument(ExtractedDocument extractedDocument) {
        log.info("Chunking started for document {}", extractedDocument.getDocument().getId());
        
        String text = extractedDocument.getExtractedText();
        if (text == null || text.trim().isEmpty()) {
            log.warn("Extracted text is empty for document {}", extractedDocument.getDocument().getId());
            return new ChunkStatisticsResponse(0, 0, 0, 0);
        }

        int chunkSize = chunkingProperties.getChunkSize();
        int overlap = chunkingProperties.getOverlap();
        
        if (overlap >= chunkSize) {
            overlap = chunkSize / 2; // Safety fallback
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int currentIndex = 0;
        int chunkIndex = 0;

        while (currentIndex < text.length()) {
            int endIndex = Math.min(currentIndex + chunkSize, text.length());
            int splitPoint = endIndex;

            if (endIndex < text.length()) {
                splitPoint = findBestSplitPoint(text, currentIndex, endIndex, overlap);
            }

            // Safety: Ensure splitPoint is strictly > currentIndex
            if (splitPoint <= currentIndex) {
                splitPoint = currentIndex + 1;
            }

            String chunkText = text.substring(currentIndex, splitPoint).trim();
            if (!chunkText.isEmpty()) {
                DocumentChunk chunk = DocumentChunk.builder()
                        .extractedDocument(extractedDocument)
                        .chunkIndex(chunkIndex++)
                        .chunkText(chunkText)
                        .startOffset(currentIndex)
                        .endOffset(splitPoint)
                        .characterCount(chunkText.length())
                        .wordCount(chunkText.split("\\s+").length)
                        .build();
                chunks.add(chunk);
            }

            if (splitPoint >= text.length()) {
                break; // We have reached the end of the document
            }

            // CORE REQUIREMENT: Guarantee strict forward progress.
            // Minimum meaningful progress ensures we don't slide by 1 character
            int minimumMeaningfulProgress = Math.max(1, (chunkSize - overlap) / 2);
            int absoluteMinNextIndex = currentIndex + minimumMeaningfulProgress;

            // Calculate next start index based on overlap
            int nextIndex = splitPoint - overlap;

            // Adjust to avoid splitting a word at the start of the next chunk
            nextIndex = adjustToWordStartSafely(text, nextIndex, absoluteMinNextIndex, splitPoint);

            // Final safety guarantees
            if (nextIndex < absoluteMinNextIndex) {
                nextIndex = absoluteMinNextIndex;
            }
            if (nextIndex >= splitPoint) {
                nextIndex = splitPoint; // Zero overlap, but safe progress
            }

            currentIndex = nextIndex;
        }

        log.info("Chunks created: {}. Proceeding to persistence.", chunks.size());
        documentChunkRepository.saveAll(chunks);
        log.info("Persistence completed for {} chunks.", chunks.size());

        int totalSize = 0;
        int minSize = Integer.MAX_VALUE;
        int maxSize = 0;

        for (DocumentChunk chunk : chunks) {
            int len = chunk.getCharacterCount();
            totalSize += len;
            if (len < minSize) minSize = len;
            if (len > maxSize) maxSize = len;
        }

        ChunkStatisticsResponse stats = ChunkStatisticsResponse.builder()
                .chunkCount(chunks.size())
                .averageChunkSize(chunks.isEmpty() ? 0 : totalSize / chunks.size())
                .largestChunk(chunks.isEmpty() ? 0 : maxSize)
                .smallestChunk(chunks.isEmpty() ? 0 : minSize)
                .build();

        log.info("Chunk statistics: {}", stats);
        log.info("Processing finished for document {}", extractedDocument.getDocument().getId());
        
        return stats;
    }

    private int findBestSplitPoint(String text, int startIndex, int maxEndIndex, int overlap) {
        // "If a semantic split would produce a chunk smaller than the overlap, 
        // ignore that semantic split and perform a larger split instead."
        int absoluteMin = startIndex + overlap + 1;
        if (absoluteMin >= maxEndIndex) {
            return maxEndIndex;
        }

        int minAcceptableSplit = Math.max(absoluteMin, maxEndIndex - overlap);

        // 1. Paragraph boundary (\n\n)
        int p = text.lastIndexOf("\n\n", maxEndIndex);
        if (p >= minAcceptableSplit) return p + 2;
        
        // single newline
        p = text.lastIndexOf("\n", maxEndIndex);
        if (p >= minAcceptableSplit) return p + 1;

        // 2. Sentence boundary (. ! ?)
        p = text.lastIndexOf(". ", maxEndIndex);
        if (p >= minAcceptableSplit) return p + 2;
        p = text.lastIndexOf("! ", maxEndIndex);
        if (p >= minAcceptableSplit) return p + 2;
        p = text.lastIndexOf("? ", maxEndIndex);
        if (p >= minAcceptableSplit) return p + 2;

        // 3. Whitespace
        // Search backwards, but MUST NOT go before absoluteMin to ensure chunk > overlap.
        for (int i = maxEndIndex; i > absoluteMin; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }

        // 4. Force split
        return maxEndIndex;
    }

    private int adjustToWordStartSafely(String text, int targetIndex, int minIndex, int maxIndex) {
        if (targetIndex <= 0) return 0;
        if (targetIndex >= text.length()) return text.length();

        // If it's already exactly at the start of a word
        if (Character.isWhitespace(text.charAt(targetIndex - 1))) {
            return targetIndex;
        }

        // Search backward up to minIndex
        for (int i = targetIndex - 1; i >= minIndex; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1; // start of the next word
            }
        }

        // Search forward up to maxIndex
        for (int i = targetIndex; i < maxIndex; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }

        return targetIndex;
    }

    @Transactional(readOnly = true)
    public List<com.memoraai.chunking.dto.ChunkResponse> getChunksForDocument(java.util.UUID documentId) {
        return documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(documentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChunkStatisticsResponse getChunkStatistics(java.util.UUID documentId) {
        List<DocumentChunk> chunks = documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(documentId);
        
        int totalSize = 0;
        int minSize = Integer.MAX_VALUE;
        int maxSize = 0;
        for (DocumentChunk chunk : chunks) {
            int len = chunk.getCharacterCount();
            totalSize += len;
            if (len < minSize) minSize = len;
            if (len > maxSize) maxSize = len;
        }

        return ChunkStatisticsResponse.builder()
                .chunkCount(chunks.size())
                .averageChunkSize(chunks.isEmpty() ? 0 : totalSize / chunks.size())
                .largestChunk(chunks.isEmpty() ? 0 : maxSize)
                .smallestChunk(chunks.isEmpty() ? 0 : minSize)
                .build();
    }

    private com.memoraai.chunking.dto.ChunkResponse mapToResponse(DocumentChunk chunk) {
        return com.memoraai.chunking.dto.ChunkResponse.builder()
                .id(chunk.getId())
                .chunkIndex(chunk.getChunkIndex())
                .chunkText(chunk.getChunkText())
                .startOffset(chunk.getStartOffset())
                .endOffset(chunk.getEndOffset())
                .characterCount(chunk.getCharacterCount())
                .wordCount(chunk.getWordCount())
                .build();
    }
}
