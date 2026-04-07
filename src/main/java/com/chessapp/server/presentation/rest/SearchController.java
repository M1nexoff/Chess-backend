package com.chessapp.server.presentation.rest;

import com.chessapp.server.application.dto.PlayerSearchDto;
import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.persistence.UserRepository;
import com.chessapp.server.infrastructure.security.JwtUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@CrossOrigin
public class SearchController {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public SearchController(UserRepository userRepository, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    /**
     * GET /api/search/players?q=username&page=0&size=20 — Search players.
     */
    @GetMapping("/players")
    public ResponseEntity<?> searchPlayers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Search query must be at least 2 characters"));
        }

        Page<User> results = userRepository.searchByLoginOrDisplayName(q, PageRequest.of(page, size));

        Page<PlayerSearchDto> dtos = results.map(user -> new PlayerSearchDto(
                user.getId(),
                user.getLogin(),
                user.getDisplayName(),
                user.getBlitzRating(),
                user.getRapidRating(),
                user.getBulletRating(),
                Boolean.TRUE.equals(user.getIsOnline())
        ));

        return ResponseEntity.ok(Map.of(
                "players", dtos.getContent(),
                "totalPages", dtos.getTotalPages(),
                "totalElements", dtos.getTotalElements(),
                "currentPage", page
        ));
    }
}
