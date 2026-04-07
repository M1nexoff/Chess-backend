---
name: api-conventions
description: REST API design patterns and conventions for this chess backend
---

# API Conventions Skill

## Response Format
All REST endpoints MUST return consistent JSON:

**Success:**
```json
{ "data": { ... } }
// or for paginated:
{ "players": [...], "totalPages": 5, "totalElements": 100, "currentPage": 0 }
```

**Error:**
```json
{ "error": "Human-readable message" }
```

## Authentication Pattern
- Extract JWT from `Authorization: Bearer <token>` header
- Resolve user via `JwtUtils.getUserNameFromJwtToken()` → `UserService.findByLogin()`
- Return 401 if user not found
- Every authenticated controller MUST have a private `resolveUser(authHeader)` helper

## DTO Rules
- Use Java `record` types for all DTOs (`GameDataDto`, `FriendResponseDto`, etc.)
- NEVER return JPA entities directly from controllers
- Map entities to DTOs in the service layer or controller helper methods
- DTO names: `{Entity}ResponseDto` for responses, `{Entity}RequestDto` for requests

## Pagination
- Use Spring `Pageable` / `Page<T>` for all list endpoints
- Default page size: 20
- Query params: `?page=0&size=20`
- Response includes: `totalPages`, `totalElements`, `currentPage`

## Endpoint Naming
- Use plural nouns: `/api/games`, `/api/friends`, `/api/users`
- Use path params for IDs: `/api/games/{id}`
- Use query params for filters: `?timeControl=BLITZ&page=0`
- Actions as sub-resources: `/api/friends/request`, `/api/friends/accept/{id}`

## Security Route Config
New endpoints MUST be registered in `WebSecurityConfig.java`:
- Public: `/api/auth/**`
- Authenticated: Everything else requires JWT
- WebSocket: `/ws/**` (JWT validated in handshake interceptor)

## Controller Structure
```java
@RestController
@RequestMapping("/api/{resource}")
@CrossOrigin
public class ResourceController {
    // Constructor injection (NO @Autowired fields)
    // Helper: resolveUser(authHeader)
    // Helper: unauthorized()
}
```
