package com.memoraai.processing.service;

import com.memoraai.common.exception.ResourceNotFoundException;
import com.memoraai.document.entity.Document;
import com.memoraai.processing.entity.JobStatus;
import com.memoraai.processing.entity.JobType;
import com.memoraai.processing.entity.ProcessingJob;
import com.memoraai.processing.repository.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingJobService {

    private final ProcessingJobRepository processingJobRepository;

    @Transactional
    public ProcessingJob createJob(Document document, JobType jobType) {
        ProcessingJob job = ProcessingJob.builder()
                .document(document)
                .jobType(jobType)
                .status(JobStatus.PENDING)
                .progress(0)
                .build();
        return processingJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public ProcessingJob getJobById(UUID id) {
        return processingJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processing job not found"));
    }

    @Transactional(readOnly = true)
    public List<ProcessingJob> getJobsForDocument(Document document) {
        return processingJobRepository.findByDocument(document);
    }

    @Transactional
    public ProcessingJob updateJobStatus(UUID id, JobStatus status, String errorMessage) {
        ProcessingJob job = getJobById(id);
        job.setStatus(status);
        if (errorMessage != null) {
            job.setErrorMessage(errorMessage);
        }
        if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
            job.setCompletedAt(LocalDateTime.now());
            if (status == JobStatus.COMPLETED) {
                job.setProgress(100);
            }
        }
        return processingJobRepository.save(job);
    }

    @Transactional
    public ProcessingJob updateJobProgress(UUID id, Integer progress) {
        ProcessingJob job = getJobById(id);
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100");
        }
        job.setProgress(progress);
        return processingJobRepository.save(job);
    }
}
