package com.chessapp.server.presentation.rest;

import com.chessapp.server.application.dto.UserResponseDto;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserController userController;

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
    void testGetProfile_Success() {
        String token = "Bearer my-jwt-token";
        when(jwtUtils.getUserNameFromJwtToken("my-jwt-token")).thenReturn("testuser");
        when(userService.findByLogin("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> response = userController.getProfile(token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserResponseDto dto = (UserResponseDto) response.getBody();
        assertEquals("testuser", dto.getLogin());
        assertEquals("Test User", dto.getDisplayName());
    }

    @Test
    void testGetProfile_UserNotFound() {
        String token = "Bearer my-jwt-token";
        when(jwtUtils.getUserNameFromJwtToken("my-jwt-token")).thenReturn("unknown_user");
        when(userService.findByLogin("unknown_user")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.getProfile(token);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
