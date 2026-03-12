package com.chessapp.server.presentation.rest;

import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import com.chessapp.server.application.service.UserService;
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

        Optional<User> userOpt = userService.findByLogin(username);

        if (userOpt.isPresent()) {
            return ResponseEntity.ok(createUserResponse(userOpt.get()));
        } else {
            System.out.println(username + " " + authHeader);
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
    public ResponseEntity<?> getOnlineUsers(@RequestHeader("Authorization") String authHeader) {
        String username = getUsernameFromToken(authHeader);
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