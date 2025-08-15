package com.chessapp.server.controller;

import com.chessapp.server.data.model.User;
import com.chessapp.server.security.JwtUtils;
import com.chessapp.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        String username = getUsernameFromToken(token);

        Optional<User> userOpt = userService.findByLogin(username);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(createUserResponse(userOpt.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String token,
                                           @RequestBody UpdateProfileRequest request) {
        String username = getUsernameFromToken(token);
        Optional<User> userOpt = userService.findByLogin(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (request.getDisplayName() != null) {
                user = userService.updateDisplayName(user, request.getDisplayName());
            }
            return ResponseEntity.ok(createUserResponse(user));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers(@RequestHeader("Authorization") String token) {
        String username = getUsernameFromToken(token);
        Optional<User> userOpt = userService.findByLogin(username);

        if (userOpt.isPresent()) {
            List<User> onlineUsers = userService.getOnlineUsersExcept(userOpt.get().getId());
            return ResponseEntity.ok(onlineUsers.stream().map(this::createUserResponse).toArray());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String getUsernameFromToken(String token) {
        return jwtUtils.getUserNameFromJwtToken(token.substring(7)); // Remove "Bearer " prefix
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("login", user.getLogin());
        userResponse.put("displayName", user.getDisplayName());
        userResponse.put("blitzRating", user.getBlitzRating());
        userResponse.put("rapidRating", user.getRapidRating());
        userResponse.put("bulletRating", user.getBulletRating());
        userResponse.put("isOnline", user.getIsOnline());
        return userResponse;
    }

    public static class UpdateProfileRequest {
        private String displayName;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
}