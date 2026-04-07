package com.chessapp.server.presentation.rest;

import com.chessapp.server.application.dto.GameHistoryDto;
import com.chessapp.server.application.service.GameService;
import com.chessapp.server.application.service.UserService;
import com.chessapp.server.domain.enums.GameResult;
import com.chessapp.server.domain.model.Game;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.persistence.GameRepository;
import com.chessapp.server.infrastructure.security.JwtUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/games")
@CrossOrigin
public class GameHistoryController {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    public GameHistoryController(GameRepository gameRepository, GameService gameService,
                                 UserService userService, JwtUtils jwtUtils) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * GET /api/games/history?page=0&size=20 — My completed game history (paginated).
     */
    @GetMapping("/history")
    public ResponseEntity<?> getGameHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        Page<Game> games = gameRepository.findCompletedGamesByPlayer(user, PageRequest.of(page, size));

        Page<GameHistoryDto> history = games.map(game -> toHistoryDto(game, user));
        return ResponseEntity.ok(Map.of(
                "games", history.getContent(),
                "totalPages", history.getTotalPages(),
                "totalElements", history.getTotalElements(),
                "currentPage", page
        ));
    }

    /**
     * GET /api/games/{id} — Single game detail with moves.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGameDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        var gameData = gameService.createGameData(id);
        if (gameData == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(gameData);
    }

    /**
     * GET /api/games/{id}/pgn — Export game as PGN string.
     */
    @GetMapping("/{id}/pgn")
    public ResponseEntity<?> exportPgn(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = resolveUser(authHeader);
        if (user == null) return unauthorized();

        Optional<Game> gameOpt = gameService.findByIdWithMoves(id);
        if (gameOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Game game = gameOpt.get();
        String pgn = buildPgn(game);
        return ResponseEntity.ok(Map.of("pgn", pgn));
    }

    // --- Helpers ---

    private GameHistoryDto toHistoryDto(Game game, User perspective) {
        boolean isWhite = game.getWhitePlayer().getId().equals(perspective.getId());
        User opponent = isWhite ? game.getBlackPlayer() : game.getWhitePlayer();
        String myColor = isWhite ? "WHITE" : "BLACK";

        String resultStr = "UNKNOWN";
        int ratingChange = 0;
        if (game.getResult() != null) {
            resultStr = mapResult(game.getResult(), isWhite);
        }

        int movesCount = 0;
        try {
            movesCount = game.getMoves() != null ? game.getMoves().size() : 0;
        } catch (Exception ignored) {}

        return new GameHistoryDto(
                game.getId(),
                opponent.getLogin(),
                opponent.getDisplayName(),
                opponent.getRatingForTimeControl(game.getTimeControl()),
                myColor,
                resultStr,
                ratingChange,
                game.getTimeControl().name(),
                movesCount,
                game.getEndedAt() != null ? game.getEndedAt() : game.getCreatedAt()
        );
    }

    private String mapResult(GameResult result, boolean isWhite) {
        return switch (result) {
            case WHITE_WIN, WHITE_WIN_TIMEOUT, WHITE_WIN_RESIGNATION -> isWhite ? "WIN" : "LOSS";
            case BLACK_WIN, BLACK_WIN_TIMEOUT, BLACK_WIN_RESIGNATION -> isWhite ? "LOSS" : "WIN";
            case DRAW, DRAW_BY_AGREEMENT -> "DRAW";
        };
    }

    private String buildPgn(Game game) {
        StringBuilder pgn = new StringBuilder();
        pgn.append("[Event \"Online Game\"]\n");
        pgn.append("[White \"").append(game.getWhitePlayer().getDisplayName()).append("\"]\n");
        pgn.append("[Black \"").append(game.getBlackPlayer().getDisplayName()).append("\"]\n");
        pgn.append("[TimeControl \"").append(game.getTimeControl().name()).append("\"]\n");
        if (game.getResult() != null) {
            pgn.append("[Result \"").append(pgnResult(game.getResult())).append("\"]\n");
        }
        pgn.append("\n");

        List<String> moves = game.getMoves();
        if (moves != null) {
            for (int i = 0; i < moves.size(); i++) {
                if (i % 2 == 0) {
                    pgn.append((i / 2 + 1)).append(". ");
                }
                pgn.append(moves.get(i)).append(" ");
            }
        }

        if (game.getResult() != null) {
            pgn.append(pgnResult(game.getResult()));
        }
        return pgn.toString().trim();
    }

    private String pgnResult(GameResult result) {
        return switch (result) {
            case WHITE_WIN, WHITE_WIN_TIMEOUT, WHITE_WIN_RESIGNATION -> "1-0";
            case BLACK_WIN, BLACK_WIN_TIMEOUT, BLACK_WIN_RESIGNATION -> "0-1";
            case DRAW, DRAW_BY_AGREEMENT -> "1/2-1/2";
        };
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

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
}
