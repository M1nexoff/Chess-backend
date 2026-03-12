package com.chessapp.server.presentation.rest;

import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setLogin("testuser");
        testUser.setDisplayName("Test User");
        testUser.setBlitzRating(1200);
        testUser.setRapidRating(1200);
        testUser.setBulletRating(1200);
        testUser.setIsOnline(true);
    }

    @Test
    void testRegister_Success() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setLogin("testuser");
        req.setPassword("testpass");
        req.setDisplayName("Test User");

        when(userService.registerUser("testuser", "testpass", "Test User")).thenReturn(testUser);
        when(jwtUtils.generateToken("testuser")).thenReturn("mock-jwt-token");

        ResponseEntity<?> response = authController.register(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("User registered successfully", body.get("message"));
        assertEquals("mock-jwt-token", body.get("token"));
    }

    @Test
    void testRegister_UsernameExists() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setLogin("testuser");
        req.setPassword("testpass");
        req.setDisplayName("Test User");

        when(userService.registerUser("testuser", "testpass", "Test User"))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        ResponseEntity<?> response = authController.register(req);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testLogin_Success() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setLogin("testuser");
        req.setPassword("testpass");

        when(userService.authenticateUser("testuser", "testpass")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken("testuser")).thenReturn("mock-jwt-token");

        ResponseEntity<?> response = authController.login(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("mock-jwt-token", body.get("token"));
    }

    @Test
    void testLogin_InvalidCredentials() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setLogin("testuser");
        req.setPassword("wrongpass");

        when(userService.authenticateUser("testuser", "wrongpass")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
