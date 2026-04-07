# Chess Backend — Full API Documentation

## Base URL
```
Development: http://localhost:8080
Production:  https://your-domain.com
```

## Authentication
All authenticated endpoints require:
```
Authorization: Bearer <JWT_TOKEN>
```

Get a token via `/api/auth/login` or `/api/auth/register`.

---

## 1. Auth Endpoints

### POST `/api/auth/register`
Create a new account.

**Request:**
```json
{
  "login": "player1",
  "password": "secret123",
  "displayName": "Player One"
}
```

**Response (200):**
```json
{
  "message": "User registered successfully",
  "token": "eyJhbGciOi...",
  "user": {
    "id": 1,
    "login": "player1",
    "displayName": "Player One",
    "blitzRating": 1200,
    "rapidRating": 1200,
    "bulletRating": 1200,
    "isOnline": false,
    "stats": {
      "blitz": { "wins": 0, "losses": 0, "draws": 0 },
      "rapid": { "wins": 0, "losses": 0, "draws": 0 },
      "bullet": { "wins": 0, "losses": 0, "draws": 0 }
    }
  }
}
```

**Errors:**
- `409` — Username already exists
- `400` — Validation error

---

### POST `/api/auth/login`
Authenticate and get JWT.

**Request:**
```json
{
  "login": "player1",
  "password": "secret123"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOi...",
  "user": { /* same structure as register */ }
}
```

**Errors:**
- `400` — Invalid credentials

---

## 2. User Endpoints

### GET `/api/users/profile`
Get my profile. **Auth required.**

**Response (200):**
```json
{
  "id": 1,
  "login": "player1",
  "displayName": "Player One",
  "blitzRating": 1250,
  "rapidRating": 1200,
  "bulletRating": 1180,
  "isOnline": true
}
```

### PUT `/api/users/profile`
Update display name. **Auth required.**

**Request:**
```json
{ "displayName": "New Name" }
```

### GET `/api/users/online`
List online users. **Auth required.**

---

## 3. Friends Endpoints

### GET `/api/friends`
List my accepted friends. **Auth required.**

**Response (200):**
```json
[
  {
    "friendshipId": 5,
    "login": "player2",
    "displayName": "Player Two",
    "blitzRating": 1400,
    "rapidRating": 1300,
    "bulletRating": 1200,
    "isOnline": true,
    "status": "ACCEPTED"
  }
]
```

### GET `/api/friends/pending`
List incoming pending requests. **Auth required.**

### POST `/api/friends/request`
Send a friend request. **Auth required.**

**Request:**
```json
{ "targetLogin": "player2" }
```

**Response (200):**
```json
{ "message": "Friend request sent", "friendshipId": 5 }
```

**Errors:**
- `400` — `"Cannot send friend request to yourself"`
- `400` — `"Already friends"`
- `400` — `"Friend request already pending"`

### POST `/api/friends/accept/{id}`
Accept a pending friend request. **Auth required.**

### POST `/api/friends/decline/{id}`
Decline a pending friend request. **Auth required.**

### DELETE `/api/friends/{id}`
Remove a friend. **Auth required.**

### POST `/api/friends/block`
Block a user. **Auth required.**

**Request:**
```json
{ "targetLogin": "trolluser" }
```

---

## 4. Game History Endpoints

### GET `/api/games/history?page=0&size=20`
My completed game history (paginated). **Auth required.**

**Response (200):**
```json
{
  "games": [
    {
      "gameId": 42,
      "opponentLogin": "player2",
      "opponentDisplayName": "Player Two",
      "opponentRating": 1350,
      "myColor": "WHITE",
      "result": "WIN",
      "ratingChange": 12,
      "timeControl": "BLITZ",
      "movesCount": 47,
      "playedAt": "2026-04-07T15:30:00"
    }
  ],
  "totalPages": 3,
  "totalElements": 52,
  "currentPage": 0
}
```

### GET `/api/games/{id}`
Full game detail with board state and moves. **Auth required.**

**Response (200):** Returns `GameDataDto` with all 36 fields.

### GET `/api/games/{id}/pgn`
Export game as PGN string. **Auth required.**

**Response (200):**
```json
{
  "pgn": "[Event \"Online Game\"]\n[White \"Player One\"]\n[Black \"Player Two\"]\n[TimeControl \"BLITZ\"]\n[Result \"1-0\"]\n\n1. e4 e5 2. Nf3 Nc6 ... 1-0"
}
```

---

## 5. Leaderboard Endpoints

### GET `/api/leaderboard?timeControl=BLITZ&page=0&size=50`
Top players by rating. **No auth required** (public leaderboard).

**Response (200):**
```json
{
  "players": [
    {
      "rank": 1,
      "login": "grandmaster",
      "displayName": "GM Magnus",
      "rating": 2850,
      "wins": 150,
      "losses": 20,
      "draws": 30,
      "isOnline": true
    }
  ],
  "totalPages": 2,
  "totalElements": 100,
  "currentPage": 0,
  "timeControl": "BLITZ"
}
```

**Query params:**
- `timeControl` — `BLITZ`, `RAPID`, or `BULLET` (default: BLITZ)
- `page` — 0-indexed (default: 0)
- `size` — page size (default: 50)

### GET `/api/leaderboard/rank?timeControl=BLITZ`
My rank. **Auth required.**

**Response (200):**
```json
{
  "rank": 42,
  "rating": 1250,
  "timeControl": "BLITZ"
}
```

---

## 6. Search Endpoints

### GET `/api/search/players?q=user&page=0&size=20`
Search players by username or display name. **No auth required.**

**Response (200):**
```json
{
  "players": [
    {
      "id": 5,
      "login": "username123",
      "displayName": "User Name",
      "blitzRating": 1200,
      "rapidRating": 1200,
      "bulletRating": 1200,
      "isOnline": false
    }
  ],
  "totalPages": 1,
  "totalElements": 3,
  "currentPage": 0
}
```

---

## 7. WebSocket Protocol

### Connection
```
ws://localhost:8080/ws?token=<JWT_TOKEN>
```

### Message Format
All messages are JSON with `type` field:

**Client → Server:**
```json
{ "type": "move", "gameId": 42, "move": "e2e4" }
```

**Server → Client:**
```json
{ "type": "gameUpdate", "data": { ... } }
```

### Message Types

| Type | Direction | Payload | Description |
|------|-----------|---------|-------------|
| `searchGame` | C→S | `{ timeControl: "BLITZ" }` | Join matchmaking |
| `cancelSearch` | C→S | `{}` | Leave matchmaking |
| `challenge` | C→S | `{ targetLogin, timeControl }` | Direct challenge |
| `acceptChallenge` | C→S | `{ challengeId }` | Accept challenge |
| `declineChallenge` | C→S | `{ challengeId }` | Decline challenge |
| `move` | C→S | `{ gameId, move }` | Make a move (SAN or UCI) |
| `resign` | C→S | `{ gameId }` | Resign game |
| `offerDraw` | C→S | `{ gameId }` | Offer draw |
| `acceptDraw` | C→S | `{ gameId }` | Accept draw |
| `declineDraw` | C→S | `{ gameId }` | Decline draw |
| `chat` | C→S | `{ gameId, message }` | In-game chat |
| `ping` | C→S | `{}` | Heartbeat |
| `connected` | S→C | `{ message }` | Connection confirmed |
| `gameStarted` | S→C | `GameDataDto` | Game created |
| `gameUpdate` | S→C | `GameDataDto` | Board state after move |
| `gameEnded` | S→C | `{ gameId, winner, result, whiteRating, blackRating }` | Game over |
| `drawOffered` | S→C | `{ gameId, offeredBy }` | Draw offered |
| `drawOfferSent` | S→C | `{ gameId }` | Your draw offer sent |
| `drawDeclined` | S→C | `{ gameId, declinedBy }` | Draw declined |
| `friendOnline` | S→C | `{ login, displayName }` | Friend came online |
| `friendOffline` | S→C | `{ login, displayName }` | Friend went offline |
| `error` | S→C | `{ message }` | Error occurred |

---

## 8. Health & Monitoring

### GET `/actuator/health`
Server health check.

**Response:**
```json
{ "status": "UP" }
```

---

## 9. Swagger UI

Interactive API docs available at:
```
http://localhost:8080/swagger-ui.html
```

---

## Mobile Integration Guide

### Flutter / Dart
```dart
// REST: Use http or dio package
final response = await http.post(
  Uri.parse('$baseUrl/api/auth/login'),
  body: jsonEncode({'login': 'user', 'password': 'pass'}),
  headers: {'Content-Type': 'application/json'},
);
String token = jsonDecode(response.body)['token'];

// WebSocket: Use web_socket_channel package
final channel = WebSocketChannel.connect(
  Uri.parse('ws://$host:8080/ws?token=$token'),
);
channel.stream.listen((message) {
  final data = jsonDecode(message);
  switch (data['type']) {
    case 'gameStarted': // handle game start
    case 'gameUpdate':  // handle board update
  }
});
```

### Kotlin / Android
```kotlin
// REST: Use Retrofit
@POST("/api/auth/login")
suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

// WebSocket: Use OkHttp
val client = OkHttpClient()
val request = Request.Builder().url("ws://host:8080/ws?token=$token").build()
client.newWebSocket(request, object : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        val msg = JSONObject(text)
        when (msg.getString("type")) {
            "gameStarted" -> { /* handle */ }
        }
    }
})
```
