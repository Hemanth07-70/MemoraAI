package com.memoraai.processing.service;

import com.memoraai.document.entity.Document;
import com.memoraai.processing.entity.JobStatus;
import com.memoraai.processing.entity.JobType;
import com.memoraai.processing.entity.ProcessingJob;
import com.memoraai.processing.repository.ProcessingJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingJobServiceTest {

    @Mock
    private ProcessingJobRepository processingJobRepository;

    @InjectMocks
    private ProcessingJobService processingJobService;

    private Document testDocument;
    private ProcessingJob testJob;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        testDocument = Document.builder().id(UUID.randomUUID()).build();
        jobId = UUID.randomUUID();
        testJob = ProcessingJob.builder()
                .id(jobId)
                .document(testDocument)
                .jobType(JobType.OCR)
                .status(JobStatus.PENDING)
                .progress(0)
                .build();
    }

    @Test
    void createJob_success() {
        when(processingJobRepository.save(any(ProcessingJob.class))).thenReturn(testJob);

        ProcessingJob result = processingJobService.createJob(testDocument, JobType.OCR);

        assertNotNull(result);
        assertEquals(JobStatus.PENDING, result.getStatus());
        verify(processingJobRepository).save(any(ProcessingJob.class));
    }

    @Test
    void updateJobStatus_completed() {
        when(processingJobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(processingJobRepository.save(any(ProcessingJob.class))).thenReturn(testJob);

        ProcessingJob result = processingJobService.updateJobStatus(jobId, JobStatus.COMPLETED, null);

        assertEquals(JobStatus.COMPLETED, result.getStatus());
        assertEquals(100, result.getProgress());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void updateJobProgress_success() {
        when(processingJobRepository.findById(jobId)).thenReturn(Optional.of(testJob));
        when(processingJobRepository.save(any(ProcessingJob.class))).thenReturn(testJob);

        ProcessingJob result = processingJobService.updateJobProgress(jobId, 50);

        assertEquals(50, result.getProgress());
    }
}
