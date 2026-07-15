package com.memoraai.processing.repository;

import com.memoraai.document.entity.Document;
import com.memoraai.processing.entity.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {
    List<ProcessingJob> findByDocument(Document document);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"document", "document.owner"})
    List<ProcessingJob> findByStatus(com.memoraai.processing.entity.JobStatus status);
}
