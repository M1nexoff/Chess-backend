---
name: clean-code
description: Enforces Clean Code and SOLID principles for Java Spring Boot.
user-invocable: false
---

# Clean Code Guidelines

You must abide by the following programming conventions whenever writing or modifying code in this project:

1. **Interfaces Over Implementations:** Application logic should rely on interfaces (e.g., `UserService`) not concrete classes (`UserServiceImpl`).
2. **Use DTOs (Data Transfer Objects):** Never return or accept raw entity models (like `User` or `Game`) directly in Controllers or WebSocket handlers. Use explicit `RequestDTO` and `ResponseDTO` records.
3. **Single Responsibility Principle (SRP):** Classes should have only one reason to change. Separate distinctly different lifecycles (e.g. `MatchmakingService` is separate from `GameService`).
4. **Explicit Logging:** Ensure error pathways have appropriate `logger.error()` logging but keep success path logging clean and debug-level based.
5. **Swagger / OpenAPI Documentation:** Decorate REST APIs with `@Operation` and `@ApiResponses` tags. No undocumented API routes are permitted.
