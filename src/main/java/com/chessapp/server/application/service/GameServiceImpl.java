package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.Game;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.domain.enums.GameResult;
import com.chessapp.server.domain.enums.GameState;
import com.chessapp.server.domain.enums.MoveResult;
import com.chessapp.server.domain.enums.TimeControl;
import com.chessapp.server.infrastructure.persistence.GameRepository;
import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.*;

@Service
public class GameServiceImpl implements GameService {
    @Autowired
    @Lazy
    private GameNotificationService notificationService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RatingService ratingService;

    @Autowired
    private GameTimerService gameTimerService;

    @Transactional
    public void endAllUnfinishedGames() {
        List<Game> unfinishedGames = gameRepository.findByStateIn(List.of(GameState.IN_PROGRESS, GameState.WAITING));
        for (Game game : unfinishedGames) {
            game.setState(GameState.ENDED);
            game.setEndedAt(LocalDateTime.now());
        }
        gameRepository.saveAll(unfinishedGames);
    }

    @Transactional
    public Game createGame(User whitePlayer, User blackPlayer, TimeControl timeControl) {
        Game game = new Game(whitePlayer, blackPlayer, timeControl);
        game.setState(GameState.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());
        System.out.println("Created game between " + whitePlayer.getLogin() + " and " + blackPlayer.getLogin());
        System.out.println("Game state: " + game.getState());

        return gameRepository.save(game);
    }

    public Optional<Game> findByIdWithMoves(Long id) {
        return gameRepository.findByIdWithMoves(id);
    }

    public Optional<Game> findById(Long gameId) {
        return gameRepository.findById(gameId);
    }

    public Optional<Game> findActiveGameByPlayer(User user) {
        return gameRepository.findActiveGameByPlayer(user);
    }

    public List<Game> findGamesByPlayer(User user) {
        return gameRepository.findByPlayerOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Map<String, Object> createGameDataMap(Long gameId) {
        Optional<Game> gameOpt = findByIdWithMoves(gameId);
        if (!gameOpt.isPresent()) {
            return null;
        }

        Game game = gameOpt.get();
        // Initialize the moves collection within the transaction
        Hibernate.initialize(game.getMoves());

        return buildGameDataMap(game);
    }

    @Transactional
    public Map<String, Object> createGameDataMap(Game game) {
        // If the game is detached, re-attach it to the session
        if (game.getId() != null) {
            Optional<Game> managedGame = findByIdWithMoves(game.getId());
            if (managedGame.isPresent()) {
                game = managedGame.get();
            }
        }

        Hibernate.initialize(game.getMoves());

        return buildGameDataMap(game);
    }

    private Map<String, Object> buildGameDataMap(Game game) {
        Map<String, Object> gameData = new HashMap<>();
        User white = game.getWhitePlayer();
        User black = game.getBlackPlayer();
        TimeControl timeControl = game.getTimeControl();

        int whiteRating = white.getRatingForTimeControl(timeControl);
        int blackRating = black.getRatingForTimeControl(timeControl);

        // Estimate rating deltas using your RatingService
        int[] whiteDeltas = ratingService.predictRatingChange(whiteRating, blackRating); // [win, draw, loss]
        int[] blackDeltas = ratingService.predictRatingChange(blackRating, whiteRating);

        gameData.put("gameId", game.getId());

        gameData.put("whitePlayer", white.getLogin());
        gameData.put("whiteDisplayName", white.getDisplayName());
        gameData.put("whiteRating", whiteRating);
        gameData.put("whiteOpponent", black.getLogin());
        gameData.put("whiteOpponentDisplayName", black.getDisplayName());
        gameData.put("whiteOpponentRating", blackRating);
        gameData.put("whiteWinDelta", whiteDeltas[0]);
        gameData.put("whiteDrawDelta", whiteDeltas[1]);
        gameData.put("whiteLossDelta", whiteDeltas[2]);

        gameData.put("blackPlayer", black.getLogin());
        gameData.put("blackDisplayName", black.getDisplayName());
        gameData.put("blackRating", blackRating);
        gameData.put("blackOpponent", white.getLogin());
        gameData.put("blackOpponentDisplayName", white.getDisplayName());
        gameData.put("blackOpponentRating", whiteRating);
        gameData.put("blackWinDelta", blackDeltas[0]);
        gameData.put("blackDrawDelta", blackDeltas[1]);
        gameData.put("blackLossDelta", blackDeltas[2]);

        gameData.put("boardState", game.getBoardState());
        gameData.put("moves", game.getMoves());
        gameData.put("isWhiteTurn", game.getIsWhiteTurn());
        gameData.put("timeControl", game.getTimeControl().name());
        gameData.put("whiteTimeLeft", game.getWhiteTimeLeft());
        gameData.put("blackTimeLeft", game.getBlackTimeLeft());
        gameData.put("state", game.getState().name());

        if (game.getResult() != null) {
            gameData.put("result", game.getResult().name());
        }

        return gameData;
    }

    @Transactional
    public MoveResult makeMove(Long gameId, User player, String moveStr) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty())
            return MoveResult.GAME_NOT_FOUND;

        Game game = gameOpt.get();
        Hibernate.initialize(game.getMoves());

        MoveResult valResult = validateMoveConstraints(game, player, moveStr);
        if (valResult != MoveResult.SUCCESS)
            return valResult;

        try {
            Board board = new Board();
            board.loadFromFen(game.getBoardState());

            Move move = parseMoveString(moveStr, board);
            if (move == null || !board.isMoveLegal(move, true)) {
                return MoveResult.INVALID_MOVE;
            }

            updatePlayerClocks(game);

            board.doMove(move);
            game.setBoardState(board.getFen());
            game.addMove(moveStr);
            game.setLastMoveAt(LocalDateTime.now());

            MoveResult endStatus = checkGameEnd(board, game);
            if (endStatus != null)
                return endStatus;

            gameRepository.save(game);

            if (game.getState() == GameState.IN_PROGRESS) {
                gameTimerService.scheduleTimeout(game);
            }
            return MoveResult.SUCCESS;

        } catch (Exception e) {
            e.printStackTrace();
            return MoveResult.ERROR;
        }
    }

    private MoveResult validateMoveConstraints(Game game, User player, String moveStr) {
        if (game.getState() != GameState.IN_PROGRESS)
            return MoveResult.GAME_NOT_STARTED;
        if (!game.getCurrentPlayer().getId().equals(player.getId()))
            return MoveResult.NOT_YOUR_TURN;
        if (moveStr == null || (moveStr.length() != 4 && moveStr.length() != 5))
            return MoveResult.INVALID_MOVE;
        return MoveResult.SUCCESS;
    }

    private Move parseMoveString(String moveStr, Board board) {
        try {
            if (moveStr.length() == 5) {
                String normalized = moveStr.toUpperCase();
                char promoChar = normalized.charAt(4);
                PieceType promoType = switch (promoChar) {
                    case 'Q' -> PieceType.QUEEN;
                    case 'R' -> PieceType.ROOK;
                    case 'B' -> PieceType.BISHOP;
                    case 'N' -> PieceType.KNIGHT;
                    default -> null;
                };
                if (promoType == null)
                    return null;

                Piece promotionPiece = Piece.make(board.getSideToMove(), promoType);
                return new Move(Square.fromValue(normalized.substring(0, 2)),
                        Square.fromValue(normalized.substring(2, 4)), promotionPiece);
            }
            return new Move(moveStr, board.getSideToMove());
        } catch (Exception e) {
            return null;
        }
    }

    private void updatePlayerClocks(Game game) {
        int elapsed = (int) calculateElapsedTime(game);
        if (game.getIsWhiteTurn()) {
            if (game.getMoves().size() > 2) {
                game.setBlackTimeLeft(Math.max(0, game.getBlackTimeLeft() - elapsed));
            }
        } else {
            if (game.getMoves().size() > 1) {
                game.setWhiteTimeLeft(Math.max(0, game.getWhiteTimeLeft() - elapsed));
            }
        }
    }

    private MoveResult checkGameEnd(Board board, Game game) {
        if (board.isMated()) {
            endGame(game, game.getIsWhiteTurn() ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
            return MoveResult.GAME_ENDED;
        } else if (board.isStaleMate() || board.isDraw()) {
            endGame(game, GameResult.DRAW);
            return MoveResult.GAME_ENDED;
        }
        return null;
    }

    private long calculateElapsedTime(Game game) {
        if (game.getLastMoveAt() == null) {
            return 0L; // First move — don't subtract any time
        }
        Duration duration = Duration.between(game.getLastMoveAt(), LocalDateTime.now());
        return duration.toMillis(); // Return elapsed time in milliseconds
    }

    @Transactional
    public void endGame(Game game, GameResult result) {
        gameTimerService.cancelTimeout(game.getId());
        System.out.println("Ending game: " + game.getId() + ", reason = " + result);
        game.setState(GameState.ENDED);
        game.setResult(result);
        game.setEndedAt(LocalDateTime.now());

        // Determine winner
        User winner = null;
        if (result == GameResult.WHITE_WIN || result == GameResult.WHITE_WIN_TIMEOUT
                || result == GameResult.WHITE_WIN_RESIGNATION) {
            winner = game.getWhitePlayer();
        } else if (result == GameResult.BLACK_WIN || result == GameResult.BLACK_WIN_TIMEOUT
                || result == GameResult.BLACK_WIN_RESIGNATION) {
            winner = game.getBlackPlayer();
        }

        game.setWinner(winner);
        gameRepository.save(game);

        // Update ratings and statistics
        updatePlayerStats(game, result);
    }

    private void updatePlayerStats(Game game, GameResult result) {
        User whitePlayer = game.getWhitePlayer();
        User blackPlayer = game.getBlackPlayer();
        TimeControl timeControl = game.getTimeControl();

        // Update win/loss/draw counts
        if (result == GameResult.WHITE_WIN || result == GameResult.WHITE_WIN_TIMEOUT
                || result == GameResult.WHITE_WIN_RESIGNATION) {
            whitePlayer.incrementWins(timeControl);
            blackPlayer.incrementLosses(timeControl);
        } else if (result == GameResult.BLACK_WIN || result == GameResult.BLACK_WIN_TIMEOUT
                || result == GameResult.BLACK_WIN_RESIGNATION) {
            blackPlayer.incrementWins(timeControl);
            whitePlayer.incrementLosses(timeControl);
        } else if (result == GameResult.DRAW) {
            whitePlayer.incrementDraws(timeControl);
            blackPlayer.incrementDraws(timeControl);
        }

        // Update ratings
        ratingService.updateRatings(whitePlayer, blackPlayer, result, timeControl);

        userService.save(whitePlayer);
        userService.save(blackPlayer);

    }

    @Transactional
    public void resignGame(Long gameId, User player) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (!gameOpt.isPresent()) {
            return;
        }

        Game game = gameOpt.get();
        Hibernate.initialize(game.getMoves());

        if (game.getState() != GameState.IN_PROGRESS || !game.isPlayerInGame(player)) {
            return;
        }

        GameResult result = player.equals(game.getWhitePlayer()) ? GameResult.BLACK_WIN_RESIGNATION
                : GameResult.WHITE_WIN_RESIGNATION;

        endGame(game, result);
    }

    @Transactional
    public void handleTimeOut(Long gameId, Long playerId) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            System.out.println("[Timeout] Game not found: " + gameId);
            return;
        }

        Game game = gameOpt.get();
        Hibernate.initialize(game.getMoves());

        if (game.getState() != GameState.IN_PROGRESS) {
            System.out.println("[Timeout] Game not in progress: " + gameId);
            return;
        }

        if (!game.isPlayerInGameById(playerId)) {
            System.out.println("[Timeout] Player not in game: " + playerId);
            return;
        }

        if (!game.getCurrentPlayer().getId().equals(playerId)) {
            System.out.println("[Timeout] Skipped — player's turn passed: " + playerId);
            return;
        }

        GameResult result = playerId.equals(game.getWhitePlayer().getId()) ? GameResult.BLACK_WIN_TIMEOUT
                : GameResult.WHITE_WIN_TIMEOUT;

        System.out.println("[Timeout] Ending game " + gameId + " due to timeout. Player: " + playerId);
        endGame(game, result);

        notificationService.notifyGameEnded(game);
    }

    @Async
    @Transactional
    public void handleTimeOutAsync(Long gameId, Long playerId) {
        handleTimeOut(gameId, playerId); // call the same logic
    }

}