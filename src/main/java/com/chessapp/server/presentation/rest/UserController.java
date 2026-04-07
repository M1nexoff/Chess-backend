package com.chessapp.server.presentation.rest;

import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import com.chessapp.server.application.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chessapp.server.application.dto.UserResponseDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String username = getUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        Optional<User> userOpt = userService.findByLogin(username);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(createUserResponse(userOpt.get()));
        } else {
            logger.warn("User not found for profile request: {} auth: {}", username, authHeader);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String token,
            @RequestBody UpdateProfileRequest request) {
        String username = getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
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
    public ResponseEntity<?> getOnlineUsers(@RequestHeader("Authorization") String authHeader) {
        String username = getUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
        Optional<User> userOpt = userService.findByLogin(username);

        if (userOpt.isPresent()) {
            List<User> onlineUsers = userService.getOnlineUsersExcept(userOpt.get().getId());
            return ResponseEntity.ok(onlineUsers.stream().map(this::createUserResponse).toArray());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String getUsernameFromToken(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                return jwtUtils.getUserNameFromJwtToken(token.substring(7)); // Remove "Bearer " prefix
            }
            return jwtUtils.getUserNameFromJwtToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private UserResponseDto createUserResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getLogin(),
                user.getDisplayName(),
                user.getBlitzRating(),
                user.getRapidRating(),
                user.getBulletRating(),
                user.getIsOnline());
    }

    public static class UpdateProfileRequest {
        private String displayName;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

}