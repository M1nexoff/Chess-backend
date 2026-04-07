---
name: clean-arch
description: Enforces Clean Architecture principles for this repository. Loaded whenever architectural decisions are requested.
user-invocable: false
---

# Clean Architecture Rules

When working on this application, you must strictly adhere to the Clean Architecture layout as defined in `AI_STANDARDS.md`.

## Core Layers
1. **domain:** Contains the core models (e.g., `User`, `Game`, `Challenge`), Enums (`GameState`, `TimeControl`), and Domain exceptions. 
   - **NO** Spring frameworks or external dependencies allowed here.
2. **application:** Contains business logic interfaces (e.g., `UserService`, `GameService`) and their `Impl` classes alongside DTOs.
3. **infrastructure:** Contains Spring Data JPA Repositories, Security Configurations (JWT), and WebSocket configurations. This layer deals with the database or external APIs.
4. **presentation:** Contains REST Controllers and WebSocket Handlers. 
   - These components should **ONLY** delegate requests to `application` layer services using DTOs.

Do not allow "spaghetti code". Ensure dependencies point inward toward the `domain`.
