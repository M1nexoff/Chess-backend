package com.chessapp.server.presentation.rest;

import com.chessapp.server.application.dto.LeaderboardEntryDto;
import com.chessapp.server.application.service.LeaderboardService;
import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.enums.TimeControl;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.security.JwtUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public LeaderboardController(LeaderboardService leaderboardService,
                                 UserService userService, JwtUtils jwtUtils) {
        this.leaderboardService = leaderboardService;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * GET /api/leaderboard?timeControl=BLITZ&page=0&size=50 — Top players.
     */
    @GetMapping
    public ResponseEntity<?> getLeaderboard(
            @RequestParam(defaultValue = "BLITZ") String timeControl,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            TimeControl tc = TimeControl.valueOf(timeControl.toUpperCase());
            Page<LeaderboardEntryDto> leaderboard = leaderboardService.getTopPlayers(tc, page, size);
            return ResponseEntity.ok(Map.of(
                    "players", leaderboard.getContent(),
                    "totalPages", leaderboard.getTotalPages(),
                    "totalElements", leaderboard.getTotalElements(),
                    "currentPage", page,
                    "timeControl", tc.name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid timeControl. Use BLITZ, RAPID, or BULLET"));
        }
    }

    /**
     * GET /api/leaderboard/rank?timeControl=BLITZ — My rank.
     */
    @GetMapping("/rank")
    public ResponseEntity<?> getMyRank(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "BLITZ") String timeControl) {
        User user = resolveUser(authHeader);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        try {
            TimeControl tc = TimeControl.valueOf(timeControl.toUpperCase());
            int rank = leaderboardService.getPlayerRank(user, tc);
            return ResponseEntity.ok(Map.of(
                    "rank", rank,
                    "rating", user.getRatingForTimeControl(tc),
                    "timeControl", tc.name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid timeControl"));
        }
    }

    private User resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            String username = jwtUtils.getUserNameFromJwtToken(authHeader.substring(7));
            return userService.findByLogin(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
