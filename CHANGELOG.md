# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v0.3.0] - 2026-07-12

### Added
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
