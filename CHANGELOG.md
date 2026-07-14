# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v0.4.3] - 2026-07-13

### Added
- **Document Chunking Engine**: Implemented native, deterministic text chunking algorithm for segmenting extracted document text.
- `DocumentChunk` entity to store chunked text sequentially, with properties like `chunk_index`, `start_offset`, and `end_offset`.
- `DocumentChunkingService` seamlessly integrated into the asynchronous AI processing pipeline to chunk texts immediately following successful OCR.
- Chunk retrieval APIs (`GET /api/v1/documents/{id}/chunks` and `statistics`), fully secured with JWT and document ownership validation.

## [v0.4.2] - 2026-07-13

### Added
- **PDF Text Extraction Engine**: FastAPI service now parses and extracts text from uploaded PDF files using `PyMuPDF`.
- `ExtractedDocument` entity and repository in Spring Boot for persisting AI-extracted text and statistics.
- Shared Docker volume (`document_storage`) enabling both the backend and AI service to read uploaded files securely.
- Real-time transition of `ProcessingJob` state from `PENDING` to `PROCESSING` to `COMPLETED` based on FastAPI worker response.
- Real-time transition of `Document` status from `UPLOADED` to `READY` upon successful OCR extraction.

### Changed
- Extended `ProcessResponse` API contract in both Spring Boot and FastAPI to include `pageCount`, `wordCount`, `characterCount`, and `text`.
- Refactored `AiProcessingService` to coordinate extraction persistence and task completion.

## [v0.3.0] - 2026-07-12
- Document Management Module (`DocumentController`, `DocumentService`, `DocumentRepository`).
- File Storage capability using the local filesystem (`LocalFileSystemStorageService`).
- Processing Job foundational entity and enums to support async pipelines.
- Polling endpoint for AI processing job statuses (`ProcessingJobController`).
- Multi-part file upload support with size constraints (50MB) and validation.
- Endpoints to retrieve, download, list, and soft-delete documents.

### Changed
- Refactored Global Exception Handler to capture file upload and storage-specific errors.
- Restructured `application.yml` to support document storage environment variables (`STORAGE_UPLOAD_DIR`).
- Adjusted Swagger OpenAPI configuration to accurately secure all endpoints except `auth` and `health`.

### Fixed
- Fixed bug with Swagger UI incorrectly ignoring standard JWT security rules.
- Fixed OpenAPI `@SecurityRequirement` namespace conflict by ensuring all endpoints adhere to the global `Bearer Authentication` standard.

### Known Issues
- Currently, Document processing is stubbed; AI components are not yet actively parsing uploaded files.
