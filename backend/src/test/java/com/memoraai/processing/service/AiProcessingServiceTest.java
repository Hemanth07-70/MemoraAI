package com.memoraai.processing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memoraai.document.entity.Document;
import com.memoraai.document.entity.DocumentStatus;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.extracteddocument.service.ExtractedDocumentService;
import com.memoraai.processing.dto.AiProcessResponse;
import com.memoraai.processing.entity.JobStatus;
import com.memoraai.processing.entity.JobType;
import com.memoraai.processing.entity.ProcessingJob;
import com.memoraai.processing.repository.ProcessingJobRepository;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.embedding.service.EmbeddingService;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.extracteddocument.entity.ExtractedDocument;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class AiProcessingServiceTest {

    private MockWebServer mockWebServer;
    private ProcessingJobRepository jobRepository;
    private DocumentRepository documentRepository;
    private ExtractedDocumentService extractedDocumentService;
    private ProcessingJobService processingJobService;
    private EmbeddingService embeddingService;
    private DocumentChunkRepository documentChunkRepository;
    private com.memoraai.documentintelligence.service.DocumentIntelligenceService documentIntelligenceService;
    private AiProcessingService aiProcessingService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        jobRepository = mock(ProcessingJobRepository.class);
        documentRepository = mock(DocumentRepository.class);
        extractedDocumentService = mock(ExtractedDocumentService.class);
        processingJobService = mock(ProcessingJobService.class);
        embeddingService = mock(EmbeddingService.class);
        documentChunkRepository = mock(DocumentChunkRepository.class);
        documentIntelligenceService = mock(com.memoraai.documentintelligence.service.DocumentIntelligenceService.class);
        com.memoraai.chunking.service.DocumentChunkingService chunkingService = mock(com.memoraai.chunking.service.DocumentChunkingService.class);
        com.memoraai.concept.service.ConceptExtractionService conceptExtractionService = mock(com.memoraai.concept.service.ConceptExtractionService.class);
        com.memoraai.anme.service.KnowledgeGraphService knowledgeGraphService = mock(com.memoraai.anme.service.KnowledgeGraphService.class);
        com.memoraai.anme.service.ANMEMemoryService anmeMemoryService = mock(com.memoraai.anme.service.ANMEMemoryService.class);
        WebClient.Builder webClientBuilder = WebClient.builder();
        
        aiProcessingService = new AiProcessingService(
                jobRepository, 
                documentRepository,
                extractedDocumentService,
                chunkingService,
                processingJobService,
                embeddingService,
                documentChunkRepository,
                webClientBuilder,
                documentIntelligenceService,
                conceptExtractionService,
                knowledgeGraphService,
                anmeMemoryService,
                mockWebServer.url("/").toString()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testProcessPendingJobs_Success_OCR() throws Exception {
        // Arrange
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .storagePath("/path/to/file.pdf")
                .status(DocumentStatus.UPLOADED)
                .build();

        UUID jobId = UUID.randomUUID();
        ProcessingJob job = ProcessingJob.builder()
                .id(jobId)
                .jobType(JobType.OCR)
                .status(JobStatus.PENDING)
                .document(document)
                .build();

        when(jobRepository.findByStatus(JobStatus.PENDING))
                .thenReturn(Collections.singletonList(job));

        AiProcessResponse response = AiProcessResponse.builder()
                .success(true)
                .status("COMPLETED")
                .message("Text extracted successfully")
                .pageCount(5)
                .wordCount(100)
                .characterCount(500)
                .text("extracted text")
                .build();
                
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(response))
                .addHeader("Content-Type", "application/json"));

        // Act
        aiProcessingService.processPendingJobs();

        // Assert
        verify(extractedDocumentService).saveExtraction(eq(document), any(AiProcessResponse.class));
        
        ArgumentCaptor<ProcessingJob> jobCaptor = ArgumentCaptor.forClass(ProcessingJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        
        ProcessingJob savedJob = jobCaptor.getValue();
        assertEquals(JobStatus.COMPLETED, savedJob.getStatus());
        assertEquals(100, savedJob.getProgress());
        assertNotNull(savedJob.getCompletedAt());
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        Document savedDoc = docCaptor.getValue();
        assertEquals(DocumentStatus.READY, savedDoc.getStatus());
        
        verify(processingJobService).createJob(document, JobType.EMBEDDING);
    }
    
    @Test
    void testProcessPendingJobs_Success_Embedding() throws Exception {
        // Arrange
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .storagePath("/path/to/file.pdf")
                .build();

        UUID jobId = UUID.randomUUID();
        ProcessingJob job = ProcessingJob.builder()
                .id(jobId)
                .jobType(JobType.EMBEDDING)
                .status(JobStatus.PENDING)
                .document(document)
                .build();

        when(jobRepository.findByStatus(JobStatus.PENDING))
                .thenReturn(Collections.singletonList(job));

        ExtractedDocument ed = ExtractedDocument.builder().document(document).build();
        DocumentChunk chunk1 = DocumentChunk.builder().id(UUID.randomUUID()).chunkIndex(0).extractedDocument(ed).build();
        DocumentChunk chunk2 = DocumentChunk.builder().id(UUID.randomUUID()).chunkIndex(1).extractedDocument(ed).build();

        when(documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(docId))
                .thenReturn(List.of(chunk1, chunk2));

        // Act
        aiProcessingService.processPendingJobs();

        // Assert
        verify(embeddingService).generateAndPersistEmbedding(chunk1);
        verify(embeddingService).generateAndPersistEmbedding(chunk2);
        
        ArgumentCaptor<ProcessingJob> jobCaptor = ArgumentCaptor.forClass(ProcessingJob.class);
        verify(jobRepository, atLeastOnce()).save(jobCaptor.capture());
        
        ProcessingJob savedJob = jobCaptor.getValue();
        assertEquals(JobStatus.COMPLETED, savedJob.getStatus());
        assertEquals(100, savedJob.getProgress());
        assertNotNull(savedJob.getCompletedAt());
        
        verify(processingJobService).createJob(document, JobType.CONCEPT_EXTRACTION);
        verify(processingJobService).createJob(document, JobType.INTELLIGENCE);
    }

    @Test
    void testProcessPendingJobs_Failure_CommunicationError() {
        // Arrange
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .storagePath("/path/to/file.pdf")
                .build();

        UUID jobId = UUID.randomUUID();
        ProcessingJob job = ProcessingJob.builder()
                .id(jobId)
                .jobType(JobType.OCR)
                .status(JobStatus.PENDING)
                .document(document)
                .build();

        when(jobRepository.findByStatus(JobStatus.PENDING))
                .thenReturn(Collections.singletonList(job));

        // Enqueue a 500 error response
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // Act
        aiProcessingService.processPendingJobs();

        // Assert
        verifyNoInteractions(extractedDocumentService);
        
        ArgumentCaptor<ProcessingJob> jobCaptor = ArgumentCaptor.forClass(ProcessingJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        
        ProcessingJob savedJob = jobCaptor.getValue();
        assertEquals(JobStatus.FAILED, savedJob.getStatus());
        assertNotNull(savedJob.getErrorMessage());
    }
}
