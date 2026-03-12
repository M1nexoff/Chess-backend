# AI_STANDARDS.md

Welcome to the AI Collaboration Standards for the Chess-backend project. Any future AI agents modifying this project must adhere strictly to these guidelines.

## 1. Project Context
- **Application Type:** Heavy WebSocket-based online Chess backend built with Spring Boot.
- **REST APIs:** Used minimally for Authentication (Login/Register) and User Profiles.
- **WebSockets:** Used for almost everything else (Matchmaking, Chat, Game Moves, Challenges).

## 2. Architectural Guidelines (Clean Architecture)
This project enforces Clean Architecture to prevent the typical "spaghetti code" monolithic structure.
- **domain:** Contains the core models (`User`, `Game`, `Challenge`), Enums (`GameState`, `TimeControl`), and Domain exceptions. No Spring dependencies here if possible.
- **application:** Contains business logic interfaces (`UserService`, `GameService`) and their `Impl` classes. DTOs belong here.
- **infrastructure:** Contains Spring Data JPA Repositories, Security Configurations (JWT), and WebSocket configurations.
- **presentation:** Contains REST Controllers and WebSocket Handlers. These components should ONLY delegate requests to `application` layer services using DTOs.

## 3. SOLID Principles & Clean Code rules
- **Interfaces over implementations:** Services must have an interface and an implementation class.
- **Use DTOs:** Never return raw entities (like `User` or `Game`) from Controllers or WebSocket handlers. The JPA structure and lazy-loading references must not leak into presentation.
- **Single Responsibility Principle:** Do not put unrelated logic together. (e.g., Matchmaking logic is separate from Game logic).

## 4. When adding a new feature
1. Add the relevant code to the `domain` (if it introduces new business terms).
2. Create DTOs and update/create Application Services (`application` package).
3. If persistence is needed, add Repositories in `infrastructure`.
4. Finally, expose the feature via REST (`presentation.rest`) or WS (`presentation.websocket`).

## 5. Important existing specifics
- The entry point for WebSocket is `ChessWebSocketHandler`. Do not dump business rules directly in `handleMessage`; delegate to the appropriate Application service!
- Swagger is already configured. Any REST controllers (except Swagger auto-configuration) should have neat `@Operation` and `@ApiResponses` tags.

Thank you for contributing properly!
