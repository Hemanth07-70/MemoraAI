# Milestone 2: Authentication & User Foundation

## Overview
This milestone establishes the foundational architecture for the backend of MemoraAI, focusing on user registration, authentication, and secure routing. The backend has transitioned to a **feature-first** package structure to improve maintainability and cohesion.

## Architecture & Structure
The backend is organized by feature rather than layer. 
- `auth`: Contains logic for Registration, Login, and JWT generation/validation.
- `user`: Contains User entity, repository, and operations specific to user data.
- `profile`: Contains UserProfile entity and related operations.
- `common`: Contains global cross-cutting concerns like Exception Handling, base API Responses, and Security configurations.

## Authentication Flow
MemoraAI uses **Stateless JWT Authentication**:
1. **Registration**: The user submits an email, password, first name, and last name.
   - The password is encrypted using `BCrypt`.
   - A `User` record and an empty `UserProfile` record are created.
   - A signed JWT is returned immediately in the response.
2. **Login**: The user submits email and password.
   - Spring Security's `AuthenticationManager` verifies the credentials.
   - A signed JWT is returned.
3. **Protected Endpoints**: The client must include the JWT in the `Authorization` header (`Bearer <token>`) for all protected endpoints.
   - The `JwtAuthenticationFilter` intercepts requests, parses the token, verifies the signature/expiration, and extracts the user.
   - If valid, it populates the `SecurityContext`, allowing the request to proceed.

## Database Schema
- **User**: Stores core identity data (`id`, `email`, `password`, `role`, `enabled`, `createdAt`, `updatedAt`).
- **UserProfile**: Stores extended profile data (`userId`, `profilePicture`, `bio`, `preferredLanguage`, `timezone`, `theme`). Has a strict One-to-One relationship with the User entity.

## Next Steps
Future milestones will build upon this foundation by adding OAuth, password resets, email verification, and specific learning modules (documents, quizzes, etc.).
