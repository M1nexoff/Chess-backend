# Chess Backend — Project Plan

## Architecture
- **Framework:** Spring Boot 3.2.3, JPA, WebSocket, Spring Security (JWT)
- **Chess Engine:** chesslib 1.3.4
- **Dev DB:** H2 (file-based) | **Prod DB:** PostgreSQL
- **API Docs:** Swagger UI at `/swagger-ui.html`

## Feature Status

### ✅ Phase 1: Core Engine & Gameplay
| Feature | Status |
|---------|--------|
| Board state (FEN) | ✅ Done |
| Move validation (chesslib) | ✅ Done |
| Promotion / En Passant / Castling | ✅ Done |
| Checkmate / Stalemate / Draw detection | ✅ Done |
| Clock management (Bullet/Blitz/Rapid) | ✅ Done (bug fixed) |
| Timer timeout scheduling | ✅ Done |
| Resign | ✅ Done |
| Draw offer / accept / decline | ✅ Done |
| ELO rating system (K=32) | ✅ Done |
| SLF4J logging (zero println) | ✅ Done |
| Constructor injection | ✅ Done |
| GameDataDto (type-safe) | ✅ Done |

### ✅ Phase 2: Matchmaking & Challenges
| Feature | Status |
|---------|--------|
| Rating-based matchmaking (±200) | ✅ Done |
| Direct challenges with expiry | ✅ Done |
| In-game chat | ✅ Done |

### ✅ Phase 3: Social & Friends
| Feature | Status |
|---------|--------|
| Friend request / accept / decline | ✅ Done |
| Remove friend | ✅ Done |
| Block user | ✅ Done |
| Friend online/offline notifications | ✅ Done |
| Presence broadcasting (WebSocket) | ✅ Done |

### ✅ Phase 4: Game History & Leaderboard
| Feature | Status |
|---------|--------|
| Game history (paginated) | ✅ Done |
| Game detail with moves | ✅ Done |
| PGN export | ✅ Done |
| Leaderboard by time control | ✅ Done |
| Player rank | ✅ Done |
| Player search | ✅ Done |

### ✅ Phase 5: API & Infrastructure
| Feature | Status |
|---------|--------|
| OpenAPI/Swagger config | ✅ Done |
| Full API documentation | ✅ Done |
| Production profile (PostgreSQL) | ✅ Done |
| Async thread pool | ✅ Done |
| Actuator health endpoint | ✅ Done |
| Claude Skills (5 total) | ✅ Done |

### 🔲 Phase 6: Future (Not Started)
| Feature | Status |
|---------|--------|
| Time increment support (3+2) | 🔲 Planned |
| Stockfish puzzle integration | 🔲 Planned |
| FEN/PGN analysis engine | 🔲 Planned |
| Spectator mode | 🔲 Planned |
| Tournament system | 🔲 Planned |
| Anti-cheat heuristics | 🔲 Planned |
| Redis matchmaking queue | 🔲 Planned |
| Pre-move support | 🔲 Planned |

## REST API Endpoints (18)

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/auth/register` | ❌ |
| POST | `/api/auth/login` | ❌ |
| GET | `/api/users/profile` | ✅ |
| PUT | `/api/users/profile` | ✅ |
| GET | `/api/users/online` | ✅ |
| GET | `/api/friends` | ✅ |
| GET | `/api/friends/pending` | ✅ |
| POST | `/api/friends/request` | ✅ |
| POST | `/api/friends/accept/{id}` | ✅ |
| POST | `/api/friends/decline/{id}` | ✅ |
| DELETE | `/api/friends/{id}` | ✅ |
| POST | `/api/friends/block` | ✅ |
| GET | `/api/games/history` | ✅ |
| GET | `/api/games/{id}` | ✅ |
| GET | `/api/games/{id}/pgn` | ✅ |
| GET | `/api/leaderboard` | ❌ |
| GET | `/api/leaderboard/rank` | ✅ |
| GET | `/api/search/players` | ❌ |

## WebSocket Messages (20+)
See `API_DOCUMENTATION.md` for full protocol reference.

## File Structure
```
src/main/java/com/chessapp/server/
├── domain/model/       → User, Game, Challenge, Friendship
├── domain/enums/       → 6 enums
├── application/dto/    → 7 DTOs (all records)
├── application/service → 9 service interfaces + impls
├── infrastructure/     → 4 repos, security, config
└── presentation/       → 6 REST controllers, 1 WS handler
```
