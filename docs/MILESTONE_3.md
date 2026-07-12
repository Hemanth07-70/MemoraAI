# Milestone 3: Document Management System

## Overview
This milestone establishes the foundational Document Management System for MemoraAI, allowing users to securely upload, manage, and retrieve their educational materials. It handles file storage, metadata tracking, and initiates the processing pipeline (stubs for future AI integration).

## Objectives Completed
- **File Upload & Validation**: Secure API to upload documents (PDF, DOCX, PPTX, TXT, PNG, JPG) up to 50MB.
- **Storage Abstraction**: `StorageService` interface implemented with a local file system provider, ready to be swapped for S3 or MinIO.
- **Metadata Management**: `Document` entity tracking ownership, file metadata, and status.
- **Asynchronous Job Foundation**: `ProcessingJob` entity tracking background AI processing pipelines (OCR, Embeddings).
- **Security**: Soft delete and strict ownership checks so users can only access their own documents.

## Modules Implemented

### `document` Module
- `Document` entity and `DocumentStatus` enum (UPLOADED, PROCESSING, READY, FAILED).
- `StorageService` interface and `LocalFileSystemStorageService`.
- `DocumentValidator` ensuring secure extensions and MIME types.
- REST endpoints for upload, listing, retrieval, download, and soft delete.

### `processing` Module
- `ProcessingJob` entity tracking status (PENDING, PROCESSING, COMPLETED, FAILED) and progress (0-100%).
- `JobType` enum (OCR, EMBEDDING, SUMMARY, QUIZ, MEMORY_ANALYSIS).
- REST endpoint to fetch processing job status for polling by the frontend.

## API Endpoints Created
All endpoints are secured with JWT under `/api/v1/`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/documents/upload` | Upload a new document (multipart form data). |
| GET | `/documents` | List all documents owned by the user. |
| GET | `/documents/{id}` | Get metadata of a specific document. |
| GET | `/documents/{id}/download` | Download the physical document file. |
| DELETE | `/documents/{id}` | Soft delete a document. |
| GET | `/processing/jobs/{id}` | Get the status and progress of a processing job. |

## Next Steps (Future Milestones)
- Implement asynchronous workers to pick up `PENDING` processing jobs from the database or a message queue.
- Implement the actual OCR and Embedding pipelines.
- Expand test coverage to the asynchronous job processing logic.
