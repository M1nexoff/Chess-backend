# Chess WebSocket API Documentation

Swagger (OpenAPI 3.0) is designed primarily for RESTful HTTP APIs and does not natively support WebSockets. While there are specs like AsyncAPI for event-driven architectures, WebSocket APIs are often best documented using Markdown.

Below is the complete documentation for the WebSocket endpoints, events, and message formats used in this chess application based on `ChessWebSocketHandler.java`.

## Connection Endpoint

**URI:** `ws://<host>:<port>/chess?token=<jwt_token>`

- WebSocket connections require an active JWT token passed as a query parameter (`?token=...`).
- When successfully connected, the server will respond with a `connected` message and also send any pending game challenges to the user.

---

## Message Format

All interactions with the WebSocket server use JSON.

**Server to Client messages** generally take the following format:
```json
{
  "type": "eventName",
  "data": {
    // Event-specific data payload
  }
}
```

**Client to Server messages** must have at minimum a `type` field to route the message:
```json
{
  "type": "eventName",
  // Other event-specific parameters at the root level
}
```

---

## Client -> Server Messages (Outgoing events)

Send these messages to the server over the open WebSocket connection.

### 1. Matchmaking

#### `searchGame`
Start searching for a random opponent with a specified time control.
```json
{
  "type": "searchGame",
  "timeControl": "BLITZ" // BLITZ, RAPID, or BULLET
}
```

#### `cancelSearch`
Cancel an active matchmaking search.
```json
{
  "type": "cancelSearch"
}
```

### 2. Direct Challenges

#### `challenge`
Send a direct game challenge to another online user.
```json
{
  "type": "challenge",
  "targetLogin": "opponent_username",
  "timeControl": "BLITZ"
}
```

#### `acceptChallenge`
Accept an incoming challenge by ID.
```json
{
  "type": "acceptChallenge",
  "challengeId": 123
}
```

#### `declineChallenge`
Decline an incoming challenge by ID.
```json
{
  "type": "declineChallenge",
  "challengeId": 123
}
```

### 3. Gameplay & Chat

#### `move`
Make a chess move in an active game.
```json
{
  "type": "move",
  "gameId": 123,                        
  "move": "e2e4" // Standard algebraic notation or UCI (e.g., e2e4, e7e8Q)
}
```

#### `resign`
Resign from an active game.
```json
{
  "type": "resign",
  "gameId": 123
}
```

#### `chat`
Send an in-game chat message to your opponent.
```json
{
  "type": "chat",
  "gameId": 123,
  "message": "Good luck!"
}
```

---

## Server -> Client Messages (Incoming events)

Listen for these messages from the Server in your client-side code.

### 1. Connection & Errors

#### `connected`
Sent immediately after a successful authentication and connection.
```json
{
  "type": "connected",
  "data": { "message": "Connected successfully" }
}
```

#### `error`
Sent when something goes wrong (e.g. invalid time control, invalid move).
```json
{
  "type": "error",
  "data": { "message": "Error description here" }
}
```
*Note: Invalid moves usually send an event type corresponding to the move result (e.g., `INVALID_MOVE`, `NOT_YOUR_TURN`) instead of a generic error string.*

### 2. Matchmaking & Challenges

#### `searchStarted` / `searchCancelled`
Confirmations of matchmaking actions.
```json
{
  "type": "searchStarted", // or searchCancelled
  "data": { "timeControl": "BLITZ" } 
}
```

#### `challengeSent`
Confirmation that your direct challenge was successfully sent.
```json
{
  "type": "challengeSent",
  "data": {
    "challengeId": 123,
    "targetUser": "opponent_username",
    "timeControl": "BLITZ"
  }
}
```

#### `incomingChallenge`
Sent when someone challenges you, or immediately upon connecting if you have pending challenges.
```json
{
  "type": "incomingChallenge",
  "data": {
    "challengeId": 123,
    "challenger": "challenger_username",
    "challengerDisplayName": "Challenger Display Name",
    "timeControl": "BLITZ"
  }
}
```

#### `challengeDeclined`
Sent when a user declines your challenge.
```json
{
  "type": "challengeDeclined",
  "data": {
    "challengeId": 123,
    "message": "User declined your challenge"
  }
}
```

### 3. Gameplay Updates

#### `gameStarted` / `gameUpdate`
Sent when a game begins or after a valid move is made. Both contain full game state data.
```json
{
  "type": "gameStarted", // or gameUpdate
  "data": {
    "gameId": 123,
    "whitePlayer": "user1",
    "blackPlayer": "user2",
    "boardState": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // FEN string
    "moves": ["e2e4", "e7e5"],
    "isWhiteTurn": true,
    "timeControl": "BLITZ",
    "whiteTimeLeft": 180000, // milliseconds
    "blackTimeLeft": 180000,
    "state": "IN_PROGRESS",
    // Rating deltas and display names are also included
  }
}
```

#### `gameEnded`
Sent when a game finishes (Checkmate, Draw, Resignation, Timeout).
```json
{
  "type": "gameEnded",
  "data": {
    "gameId": 123,
    "winner": "winner_username", // or "draw"
    "result": "WHITE_WIN",       // Reason for end (e.g. BLACK_WIN_TIMEOUT, DRAW, etc.)
    "whiteRating": 1215,
    "blackRating": 1185
  }
}
```

#### `chatMessage`
Sent when your opponent sends a chat message.
```json
{
  "type": "chatMessage",
  "data": {
    "gameId": 123,
    "sender": "Opponent Display Name",
    "message": "Good luck!",
    "timestamp": 1700000000000 
  }
}
```
