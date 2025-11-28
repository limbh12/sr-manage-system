# Copilot Instructions for SR Management System

## Project Overview

This is an SR (Service Request) Management System designed to manage and track service requests efficiently. The system consists of:

- **Backend**: Spring Boot 3.x application with Java 17+
- **Frontend**: React 18.x application with TypeScript and Vite

The application supports JWT-based authentication, CRUD operations for service requests, and role-based access control (ADMIN/USER).

## Project Structure

```
sr-manage-system/
├── docs/               # API and database documentation
├── backend/            # Spring Boot backend
│   └── src/main/java/com/srmanagement/
│       ├── config/     # Configuration classes
│       ├── controller/ # REST controllers
│       ├── service/    # Business logic
│       ├── repository/ # Data access layer
│       ├── entity/     # JPA entities
│       ├── dto/        # Data transfer objects
│       ├── security/   # Security classes (JWT)
│       └── exception/  # Exception handling
└── frontend/           # React frontend
    └── src/
        ├── components/ # Reusable components
        ├── pages/      # Page components
        ├── hooks/      # Custom hooks
        ├── services/   # API services
        ├── store/      # Redux store
        ├── utils/      # Utility functions
        └── types/      # TypeScript type definitions
```

## Build and Test Commands

### Backend (Spring Boot)

```bash
cd backend

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the application
./gradlew bootRun
```

### Frontend (React/TypeScript)

```bash
cd frontend

# Install dependencies
npm install

# Run linting
npm run lint

# Build the project
npm run build

# Run development server
npm run dev
```

## Coding Standards

### Java/Spring Boot (Backend)

- Use Java 17+ features
- Follow standard Java naming conventions (camelCase for variables/methods, PascalCase for classes)
- Use Lombok annotations (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) for entity classes
- Add Javadoc comments to classes and public methods
- Use `@RestController` for REST API endpoints
- Use `@Service` for business logic
- Use `@Repository` for data access
- Follow the existing package structure under `com.srmanagement`

### TypeScript/React (Frontend)

- Use TypeScript with strict mode enabled
- Use functional components with React hooks
- Export types and interfaces from the `types/` directory
- Use path aliases (`@/`) for imports
- Add JSDoc comments to components and functions
- Follow existing ESLint rules
- Use Redux Toolkit for state management
- Use Axios for HTTP requests

## Domain Concepts

### SR (Service Request)

- **Status**: OPEN → IN_PROGRESS → RESOLVED → CLOSED
- **Priority**: LOW, MEDIUM, HIGH, CRITICAL
- **Requester**: User who created the SR
- **Assignee**: User responsible for handling the SR

### User Roles

- **ADMIN**: Can manage all users and SRs
- **USER**: Can create and manage their own SRs

## API Guidelines

- Base URL: `/api`
- Use JWT Bearer tokens for authentication
- Follow RESTful conventions
- Return JSON responses
- Use standard HTTP status codes (200, 201, 400, 401, 403, 404, 500)
- Refer to `docs/API.md` for detailed API specifications

## Testing Requirements

- Write unit tests for new service methods in the backend
- Use JUnit 5 and Spring Test for backend testing
- Ensure tests pass before committing changes
- Test authentication and authorization for secured endpoints

## Security Considerations

- Never commit sensitive data (passwords, API keys, JWT secrets)
- Use environment variables for configuration
- Validate all user inputs
- Follow JWT best practices for token handling
- Use Spring Security for authentication and authorization
