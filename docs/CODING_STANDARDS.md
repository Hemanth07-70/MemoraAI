# MemoraAI Coding Standards & Guidelines

This document outlines the coding standards, architectural principles, and best practices for the MemoraAI platform. Adhering to these guidelines ensures consistency, maintainability, and scalability across the project.

---

## 1. Architecture

### Feature-First Architecture
- Code should be organized by feature or domain, rather than strictly by technical layer.
- Components related to a specific domain (e.g., `user`, `quiz`, `document`) should be kept together.
- This promotes high cohesion and loose coupling between different parts of the application.

### Microservices
- Services must remain independent and communicate via well-defined REST APIs or message brokers.
- Each service is responsible for its own data store. No shared databases between services.

---

## 2. Backend (Spring Boot / Java)

### Dependency Injection
- **Constructor Injection ONLY**. Do not use `@Autowired` on fields.
- Use Lombok's `@RequiredArgsConstructor` to generate constructors for final fields automatically.

### DTO Usage & Entity Encapsulation
- **No Entity Exposure**: JPA Entities must never be exposed directly through REST controllers.
- Always map Entities to Data Transfer Objects (DTOs) before returning them to the client.
- Use explicit mapper classes (e.g., MapStruct) to handle Entity <-> DTO conversions.

### Package Naming Conventions
- Packages should follow the standard structure: `com.memoraai.<module_name>.<layer>`
- Valid layers include: `config`, `controller`, `dto`, `entity`, `exception`, `repository`, `security`, `service`, `util`.

### Global Exception Handling
- Use `@ControllerAdvice` and `@ExceptionHandler` for centralized error handling.
- Always return a standardized error response payload (e.g., `ApiErrorResponse` containing a timestamp, status, error code, and generic message).
- Do not expose internal stack traces or database errors in API responses.

### API Versioning
- APIs should be versioned at the URI level (e.g., `/api/v1/users`).
- Maintain backward compatibility when making changes to existing endpoints.

### Validation Rules
- Use `spring-boot-starter-validation` (Jakarta Validation) for all incoming requests.
- Validate inputs at the controller boundary using `@Valid`.
- Define explicit validation constraints on DTO fields.

---

## 3. Frontend (React / TypeScript)

### Component Structure
- Keep components small and focused on a single responsibility.
- Separate presentational components from container (smart) components.

### State Management
- Prefer React Context or custom hooks for localized state.
- Use a global state manager only when state needs to be shared across many disconnected components.

### API Communication
- Use custom hooks for data fetching to abstract API calls away from components.
- Implement proper error handling and display user-friendly error messages (e.g., `Network Error`, `Backend Offline`) instead of generic failures.

---

## 4. General Guidelines

### Logging
- Log meaningful messages. Include context (e.g., user ID, document ID) when applicable.
- Use appropriate log levels (`ERROR` for failures requiring attention, `WARN` for recoverable issues, `INFO` for significant state changes, `DEBUG` for troubleshooting).
- Avoid logging sensitive information (PII, passwords, tokens).

### Testing Strategy
- **Unit Tests**: Write comprehensive unit tests for all business logic, particularly in the service layer.
- **Integration Tests**: Test database interactions and controller endpoints using `@SpringBootTest` or `@WebMvcTest`.
- Target a minimum of 80% test coverage for critical paths.

### Git Branching Strategy
- Use a standard Git Flow or trunk-based development strategy.
- Main branches: `main` (production-ready code) and `develop` (integration branch).
- Feature branches: `feature/<issue-number>-<short-description>`.
- Bugfix branches: `bugfix/<issue-number>-<short-description>`.
- All code must be reviewed via Pull Requests before merging.
