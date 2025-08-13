package com.chessapp.server.controller;

import com.chessapp.server.data.model.User;
import com.chessapp.server.security.JwtUtils;
import com.chessapp.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request.getLogin(), request.getPassword(), request.getDisplayName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("user", createUserResponse(user));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.authenticateUser(request.getLogin(), request.getPassword());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtUtils.generateToken(user.getLogin());

            // Set user online
            userService.setUserOnline(user, true);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", createUserResponse(user));

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
        }
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

        Map<String, Object> stats = new HashMap<>();
        stats.put("blitz", Map.of("wins", user.getBlitzWins(), "losses", user.getBlitzLosses(), "draws", user.getBlitzDraws()));
        stats.put("rapid", Map.of("wins", user.getRapidWins(), "losses", user.getRapidLosses(), "draws", user.getRapidDraws()));
        stats.put("bullet", Map.of("wins", user.getBulletWins(), "losses", user.getBulletLosses(), "draws", user.getBulletDraws()));
        userResponse.put("stats", stats);

        return userResponse;
    }

    public static class RegisterRequest {
        private String login;
        private String password;
        private String displayName;

        // Getters and setters
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public static class LoginRequest {
        private String login;
        private String password;

        // Getters and setters
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
