package com.chessapp.server.presentation.rest;

import com.chessapp.server.application.dto.FriendResponseDto;
import com.chessapp.server.application.service.FriendService;
import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.model.Friendship;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
@CrossOrigin
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    private final FriendService friendService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public FriendController(FriendService friendService, UserService userService, JwtUtils jwtUtils) {
        this.friendService = friendService;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * GET /api/friends — List my accepted friends.
     */
    @GetMapping
    public ResponseEntity<?> getFriends(@RequestHeader("Authorization") String authHeader) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        List<FriendResponseDto> friends = friendService.getFriends(user);
        return ResponseEntity.ok(friends);
    }

    /**
     * GET /api/friends/pending — List incoming pending friend requests.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(@RequestHeader("Authorization") String authHeader) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        List<FriendResponseDto> pending = friendService.getPendingRequests(user);
        return ResponseEntity.ok(pending);
    }

    /**
     * POST /api/friends/request — Send a friend request.
     * Body: { "targetLogin": "someUser" }
     */
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        String targetLogin = body.get("targetLogin");
        if (targetLogin == null || targetLogin.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetLogin is required"));
        }

        try {
            Friendship friendship = friendService.sendRequest(user, targetLogin);
            return ResponseEntity.ok(Map.of(
                    "message", "Friend request sent",
                    "friendshipId", friendship.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/friends/accept/{id} — Accept a pending friend request.
     */
    @PostMapping("/accept/{id}")
    public ResponseEntity<?> acceptRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        try {
            friendService.acceptRequest(user, id);
            return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/friends/decline/{id} — Decline a pending friend request.
     */
    @PostMapping("/decline/{id}")
    public ResponseEntity<?> declineRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        try {
            friendService.declineRequest(user, id);
            return ResponseEntity.ok(Map.of("message", "Friend request declined"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/friends/{id} — Remove an existing friend.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeFriend(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        try {
            friendService.removeFriend(user, id);
            return ResponseEntity.ok(Map.of("message", "Friend removed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/friends/block — Block a user.
     * Body: { "targetLogin": "someUser" }
     */
    @PostMapping("/block")
    public ResponseEntity<?> blockUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        String targetLogin = body.get("targetLogin");
        if (targetLogin == null || targetLogin.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetLogin is required"));
        }

        try {
            friendService.blockUser(user, targetLogin);
            return ResponseEntity.ok(Map.of("message", "User blocked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Helpers ---

    private User resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String username = jwtUtils.getUserNameFromJwtToken(authHeader.substring(7));
            return userService.findByLogin(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
}
