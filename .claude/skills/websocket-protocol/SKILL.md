---
name: websocket-protocol
description: WebSocket message structure and handler patterns for real-time chess gameplay
---

# WebSocket Protocol Skill

## Connection
- Endpoint: `ws://host:8080/ws?token=<JWT_TOKEN>`
- JWT is validated in `JwtHandshakeInterceptor` before connection is established
- On connect: user is set online, friends are notified, pending challenges are sent

## Message Envelope
ALL WebSocket messages use this JSON envelope:

**Client → Server:**
```json
{ "type": "move", "gameId": 42, "move": "e2e4" }
```

**Server → Client:**
```json
{ "type": "gameUpdate", "data": { ... } }
```

## Message Types Reference

### Matchmaking
| Client sends | Server responds | Description |
|---|---|---|
| `searchGame` (+ timeControl) | `searchStarted` | Enter matchmaking queue |
| `cancelSearch` | `searchCancelled` | Leave queue |
| — | `gameStarted` | Match found, game created |

### Challenges
| Client sends | Server responds | Description |
|---|---|---|
| `challenge` (+ targetLogin, timeControl) | `challengeSent` / `incomingChallenge` | Direct challenge |
| `acceptChallenge` (+ challengeId) | `gameStarted` | Challenge accepted |
| `declineChallenge` (+ challengeId) | `challengeDeclined` | Challenge declined |

### Gameplay
| Client sends | Server responds | Description |
|---|---|---|
| `move` (+ gameId, move) | `gameUpdate` (full GameDataDto) | Make a move |
| `resign` (+ gameId) | `gameEnded` | Resign the game |
| `offerDraw` (+ gameId) | `drawOffered` / `drawOfferSent` | Offer draw |
| `acceptDraw` (+ gameId) | `gameEnded` | Accept draw |
| `declineDraw` (+ gameId) | `drawDeclined` | Decline draw |

### Social
| Client sends | Server responds | Description |
|---|---|---|
| `chat` (+ gameId, message) | `chatMessage` | In-game chat |
| `ping` | `pong` | Heartbeat |
| — | `friendOnline` | Friend came online |
| — | `friendOffline` | Friend went offline |

## Handler Pattern
Every new message type MUST:
1. Have a `case` in the switch block in `handleMessage()`
2. Delegate to a private `handleXxx(User, Map)` method
3. That method delegates to a service — NO business logic in the handler
4. Wrap everything in try/catch, send `error` type on failure

```java
case "newMessageType":
    handleNewMessageType(user, messageData);
    break;

private void handleNewMessageType(User user, Map<String, Object> messageData) {
    try {
        // Extract params
        // Call service
        // Send response via sendToUser()
    } catch (Exception e) {
        logger.error("Error: ", e);
        sendToUser(user.getLogin(), "error", Map.of("message", "Failed"));
    }
}
```
