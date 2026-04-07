---
name: modules
description: Step-by-step instructions on how to structure and add new modules.
user-invocable: false
---

# Modular Structure

Whenever requested to add a new generalized workflow or feature (module), follow these ordered steps:

1. **Domain Creation:** Evaluate and define the `domain` Entities and Enums first. Establish what business terms are being added.
2. **Service Definitions:** Inside `application`, define the DTOs and the Service interfaces. Keep DTO records grouped contextually.
3. **Persistence Layer:** Move to `infrastructure` and create any necessary Spring Data JPA Interfaces (`Repository`).
4. **Connectivity:** Finally, create the `presentation` layer (`RestController` or `WebSocketHandler`) exposing the feature endpoints using the DTOs and delegating to the application interface.

## Notes for WebSocket Handlers
Do not dump business logic in `handleMessage()`. The `ChessWebSocketHandler` should read the payload, determine the intention, map it to a request DTO, and forward it to a business service.
