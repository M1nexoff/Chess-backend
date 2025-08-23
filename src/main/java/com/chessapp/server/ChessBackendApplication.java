package com.chessapp.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ChessBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChessBackendApplication.class, args);
    }
}

//Your structure is good as a generic event-driven WebSocket communication format. Here's how we can refine it:
//
//---
//
//### ✅ Suggested Data Structure
//
//```json
//{
//  "type": "string", // Event type
//  "data": {
//    // Event-specific payload
//  }
//}
//```
//
//---
//
//### ✅ Event Types and Example Payloads
//
//1. **connected**
//
//   ```json
//   {
//     "type": "connected",
//     "data": {
//       "userId": "12345",
//       "username": "PlayerOne"
//     }
//   }
//   ```
//
//2. **gameStarted**
//
//   ```json
//   {
//     "type": "gameStarted",
//     "data": {
//       "gameId": "abc123",
//       "whitePlayer": { "id": "12345", "name": "PlayerOne" },
//       "blackPlayer": { "id": "67890", "name": "PlayerTwo" },
//       "timeControl": { "initial": 300, "increment": 2 } // seconds
//     }
//   }
//   ```
//
//3. **moveResult** (when a move is validated and applied)
//
//   ```json
//   {
//     "type": "moveResult",
//     "data": {
//       "from": "e2",
//       "to": "e4",
//       "san": "e4",
//       "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1",
//       "isCheck": false,
//       "isCheckmate": false
//     }
//   }
//   ```
//
//4. **gameUpdate** (clock updates, captures, draw offers)
//
//   ```json
//   {
//     "type": "gameUpdate",
//     "data": {
//       "fen": "...",
//       "whiteTime": 290,
//       "blackTime": 300,
//       "moveNumber": 2
//     }
//   }
//   ```
//
//5. **gameEnded**
//
//   ```json
//   {
//     "type": "gameEnded",
//     "data": {
//       "reason": "checkmate",
//       "winner": "white",
//       "pgn": "[Event ...]"
//     }
//   }
//   ```
//
//6. **searchStarted**
//
//   ```json
//   {
//     "type": "searchStarted",
//     "data": {
//       "mode": "blitz",
//       "timeControl": "3+2"
//     }
//   }
//   ```
//
//7. **searchCancelled**
//
//   ```json
//   {
//     "type": "searchCancelled",
//     "data": {}
//   }
//   ```
//
//8. **incomingChallenge**
//
//   ```json
//   {
//     "type": "incomingChallenge",
//     "data": {
//       "challengeId": "xyz789",
//       "fromUser": { "id": "67890", "name": "PlayerTwo" },
//       "timeControl": "5+0"
//     }
//   }
//   ```
//
//9. **challengeAccepted**
//
//   ```json
//   {
//     "type": "challengeAccepted",
//     "data": {
//       "challengeId": "xyz789",
//       "gameId": "abc123"
//     }
//   }
//   ```
//
//10. **challengeDeclined**
//
//    ```json
//    {
//      "type": "challengeDeclined",
//      "data": {
//        "challengeId": "xyz789"
//      }
//    }
//    ```
//
//11. **challengeSent**
//
//    ```json
//    {
//      "type": "challengeSent",
//      "data": {
//        "challengeId": "xyz789",
//        "toUser": { "id": "67890", "name": "PlayerTwo" }
//      }
//    }
//    ```
//
//12. **chatMessage**
//
//    ```json
//    {
//      "type": "chatMessage",
//      "data": {
//        "from": "PlayerOne",
//        "message": "Good luck!",
//        "timestamp": 1698000000
//      }
//    }
//    ```
//
//13. **error**
//
//    ```json
//    {
//      "type": "error",
//      "data": {
//        "code": 400,
//        "message": "Invalid move"
//      }
//    }
//    ```
//
//---
//
//### ✅ Additions Recommended
//
//* **ping/pong** → For connection health.
//* **typingIndicator** (optional for chat).
//* **drawOffer**, **drawAccepted**, **drawDeclined** → Important for chess rules.
//* **resign** → Player resigns.
//* **takebackRequest**, **takebackAccepted**, **takebackDeclined** → Optional but common in chess apps.
//
//---
//
//✅ Would you like me to **create full Dart models** for these (using Freezed for immutability and sealed classes for `type`)?
//Or should I make **a single generic model** with dynamic `data`?