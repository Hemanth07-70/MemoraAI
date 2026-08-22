package com.memoraai.processing.service;

import com.memoraai.document.entity.Document;
import com.memoraai.document.entity.DocumentStatus;
import com.memoraai.document.repository.DocumentRepository;
import com.memoraai.extracteddocument.service.ExtractedDocumentService;
import com.memoraai.processing.dto.AiProcessRequest;
import com.memoraai.processing.dto.AiProcessResponse;
import com.memoraai.processing.entity.JobStatus;
import com.memoraai.processing.entity.JobType;
import com.memoraai.processing.entity.ProcessingJob;
import com.memoraai.processing.repository.ProcessingJobRepository;
import com.memoraai.chunking.entity.DocumentChunk;
import com.memoraai.chunking.repository.DocumentChunkRepository;
import com.memoraai.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class AiProcessingService {

    private final ProcessingJobRepository jobRepository;
    private final DocumentRepository documentRepository;
    private final ExtractedDocumentService extractedDocumentService;
    private final com.memoraai.chunking.service.DocumentChunkingService documentChunkingService;
    private final ProcessingJobService processingJobService;
    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;
    private final WebClient webClient;
    private final com.memoraai.documentintelligence.service.DocumentIntelligenceService documentIntelligenceService;
    private final com.memoraai.concept.service.ConceptExtractionService conceptExtractionService;
    private final com.memoraai.anme.service.KnowledgeGraphService knowledgeGraphService;
    private final com.memoraai.anme.service.ANMEMemoryService anmeMemoryService;

    public AiProcessingService(
            ProcessingJobRepository jobRepository,
            DocumentRepository documentRepository,
            ExtractedDocumentService extractedDocumentService,
            com.memoraai.chunking.service.DocumentChunkingService documentChunkingService,
            ProcessingJobService processingJobService,
            EmbeddingService embeddingService,
            DocumentChunkRepository documentChunkRepository,
            WebClient.Builder webClientBuilder,
            com.memoraai.documentintelligence.service.DocumentIntelligenceService documentIntelligenceService,
            com.memoraai.concept.service.ConceptExtractionService conceptExtractionService,
            com.memoraai.anme.service.KnowledgeGraphService knowledgeGraphService,
            com.memoraai.anme.service.ANMEMemoryService anmeMemoryService,
            @Value("${memoraai.ai.base-url}") String aiBaseUrl) {
        this.jobRepository = jobRepository;
        this.documentRepository = documentRepository;
        this.extractedDocumentService = extractedDocumentService;
        this.documentChunkingService = documentChunkingService;
        this.processingJobService = processingJobService;
        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
        this.webClient = webClientBuilder.baseUrl(aiBaseUrl).build();
        this.documentIntelligenceService = documentIntelligenceService;
        this.conceptExtractionService = conceptExtractionService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.anmeMemoryService = anmeMemoryService;
    }

    // On startup, reset any jobs left in PROCESSING state from a previous run (e.g. Render restart mid-job)
    @EventListener(ApplicationReadyEvent.class)
    public void resetStuckJobsOnStartup() {
        List<ProcessingJob> stuckJobs = jobRepository.findByStatus(JobStatus.PROCESSING);
        if (!stuckJobs.isEmpty()) {
            log.warn("Found {} stuck PROCESSING jobs on startup — resetting to PENDING", stuckJobs.size());
            stuckJobs.forEach(job -> {
                job.setStatus(JobStatus.PENDING);
                job.setProgress(0);
                jobRepository.save(job);
            });
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processPendingJobs() {
        List<ProcessingJob> pendingJobs = jobRepository.findByStatus(JobStatus.PENDING);
        if (pendingJobs.isEmpty()) {
            return; // No jobs to process
        }

        log.info("Found {} pending processing jobs.", pendingJobs.size());

        for (ProcessingJob job : pendingJobs) {
            try {
                processJob(job);
            } catch (Exception e) {
                log.error("Failed to process job {}: {}", job.getId(), e.getMessage());
                handleJobFailure(job, e.getMessage());
            }
        }
    }

    private void processJob(ProcessingJob job) {
        log.info("Starting processing for job {} of type {}", job.getId(), job.getJobType());

        // Reload to avoid LazyInitializationException — scheduler has no @Transactional
        Document document = documentRepository.findById(job.getDocument().getId())
                .orElseThrow(() -> new IllegalStateException("Document not found: " + job.getDocument().getId()));

        if (JobType.EMBEDDING.equals(job.getJobType())) {
            processEmbeddingJob(job);
            return;
        }
        if (JobType.INTELLIGENCE.equals(job.getJobType())) {
            processIntelligenceJob(job);
            return;
        }

        if (JobType.CONCEPT_EXTRACTION.equals(job.getJobType())) {
            processConceptExtractionJob(job);
            return;
        }

        if (JobType.KNOWLEDGE_GRAPH.equals(job.getJobType())) {
            processKnowledgeGraphJob(job);
            return;
        }

        String fileContent = null;
        String storagePath = document.getStoragePath();
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(storagePath));
            fileContent = Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            log.warn("Could not read file at {}, sending path only: {}", storagePath, e.getMessage());
        }

        AiProcessRequest request = AiProcessRequest.builder()
                .jobId(job.getId())
                .documentId(document.getId())
                .jobType(job.getJobType().name())
                .filePath(storagePath)
                .fileContent(fileContent)
                .build();

        AiProcessResponse response = webClient.post()
                .uri("/api/v1/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiProcessResponse.class)
                .timeout(Duration.ofSeconds(60))
                .block();

        if (response != null && response.isSuccess()) {
            if ("COMPLETED".equals(response.getStatus()) && "OCR".equals(job.getJobType().name())) {
                var extractedDocument = extractedDocumentService.saveExtraction(document, response);
                documentChunkingService.chunkDocument(extractedDocument);
                
                // Create EMBEDDING job after OCR completes
                processingJobService.createJob(document, JobType.EMBEDDING);
                
                job.setStatus(JobStatus.COMPLETED);
                job.setProgress(100);
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                checkAndUpdateDocumentStatus(document);
                
                log.info("Job {} successfully completed extraction and chunking. Status: {}", job.getId(), response.getStatus());
            } else {
                job.setStatus(JobStatus.PROCESSING);
                jobRepository.save(job);
                log.info("Job {} successfully dispatched to AI service. Status: {}", job.getId(), response.getStatus());
            }
        } else {
            String errorMsg = response != null ? response.getMessage() : "Null response from AI service";
            handleJobFailure(job, "AI Service rejected job: " + errorMsg);
        }
    }
    
    private void processKnowledgeGraphJob(ProcessingJob job) {
        log.info("Starting knowledge graph generation for job {}", job.getId());
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);

        try {
            // Reload document with owner eagerly to avoid LazyInitializationException
            // outside a Hibernate session (scheduler has no @Transactional wrapper)
            Document document = documentRepository.findById(job.getDocument().getId())
                    .orElseThrow(() -> new IllegalStateException("Document not found: " + job.getDocument().getId()));

            knowledgeGraphService.generateKnowledgeGraph(document.getId());

            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Job {} successfully completed knowledge graph generation.", job.getId());
            checkAndUpdateDocumentStatus(document);

            // Initialize UserMemoryState for every extracted concept (idempotent)
            try {
                anmeMemoryService.initializeMemoryStates(
                        document.getId(),
                        document.getOwner().getId(),
                        document.getOwner());
                log.info("Memory states initialized for document {}", document.getId());
            } catch (Exception memEx) {
                log.warn("Memory state initialization failed for document {}: {}", document.getId(), memEx.getMessage());
            }
        } catch (Exception e) {
            handleJobFailure(job, "Knowledge graph generation failed: " + e.getMessage());
        }
    }

    private void handleJobFailure(ProcessingJob job, String errorMessage) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        jobRepository.save(job);

        // Use findById to avoid proxy lazy-load outside session
        documentRepository.findById(job.getDocument().getId()).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            documentRepository.save(doc);
        });
        
        log.error("Job {} marked as FAILED. Reason: {}", job.getId(), errorMessage);
    }

    private void checkAndUpdateDocumentStatus(Document document) {
        if (document == null) return;
        List<ProcessingJob> allJobs = jobRepository.findByDocument(document);
        boolean allCompleted = !allJobs.isEmpty() && allJobs.stream().allMatch(j -> j.getStatus() == JobStatus.COMPLETED);
        if (allCompleted) {
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);
            log.info("Document {} is now fully READY.", document.getId());
        }
    }

    private void processEmbeddingJob(ProcessingJob job) {
        log.info("Starting embedding generation for job {}", job.getId());
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);
        
        try {
            List<DocumentChunk> chunks = documentChunkRepository.findByExtractedDocumentDocumentIdOrderByChunkIndexAsc(job.getDocument().getId());
            int total = chunks.size();

            if (total == 0) {
                log.warn("No chunks found for document {}", job.getDocument().getId());
            } else {
                log.info("Batch-embedding {} chunks in one call", total);
                embeddingService.generateAndPersistEmbeddingsBatch(chunks);
                job.setProgress(100);
                jobRepository.save(job);
            }
            
            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Branch off CONCEPT_EXTRACTION and INTELLIGENCE in parallel
            processingJobService.createJob(job.getDocument(), JobType.CONCEPT_EXTRACTION);
            processingJobService.createJob(job.getDocument(), JobType.INTELLIGENCE);
            
            log.info("Job {} successfully completed embedding generation.", job.getId());
            checkAndUpdateDocumentStatus(job.getDocument());
        } catch (Exception e) {
            handleJobFailure(job, "Embedding generation failed: " + e.getMessage());
        }
    }

    private void processIntelligenceJob(ProcessingJob job) {
        log.info("Starting intelligence generation for job {}", job.getId());
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);
        
        try {
            documentIntelligenceService.generateIntelligence(job.getDocument().getId());
            
            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            log.info("Job {} successfully completed intelligence generation.", job.getId());
            checkAndUpdateDocumentStatus(job.getDocument());
        } catch (Exception e) {
            handleJobFailure(job, "Intelligence generation failed: " + e.getMessage());
        }
    }

    private void processConceptExtractionJob(ProcessingJob job) {
        log.info("Starting concept extraction for job {}", job.getId());
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);
        
        try {
            conceptExtractionService.extractConcepts(job.getDocument().getId());
            
            job.setStatus(JobStatus.COMPLETED);
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Generate Knowledge Graph Job after Concept Extraction completes
            processingJobService.createJob(job.getDocument(), JobType.KNOWLEDGE_GRAPH);
            
            log.info("Job {} successfully completed concept extraction.", job.getId());
            checkAndUpdateDocumentStatus(job.getDocument());
        } catch (Exception e) {
            handleJobFailure(job, "Concept extraction failed: " + e.getMessage());
        }
    }
}
