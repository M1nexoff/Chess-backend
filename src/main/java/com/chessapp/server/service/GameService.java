package com.chessapp.server.service;

import com.chessapp.server.data.model.Game;
import com.chessapp.server.data.model.User;
import com.chessapp.server.data.model.enums.GameResult;
import com.chessapp.server.data.model.enums.GameState;
import com.chessapp.server.data.model.enums.MoveResult;
import com.chessapp.server.data.model.enums.TimeControl;
import com.chessapp.server.repository.GameRepository;
import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveException;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.*;

@Service
public class GameService {
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
        if (!gameOpt.isPresent()) {
            System.out.println("Game not found");
            return MoveResult.GAME_NOT_FOUND;
        }

        Game game = gameOpt.get();
        Hibernate.initialize(game.getMoves());

        if (game.getState() != GameState.IN_PROGRESS) {
            System.out.println("Game not in progress");
            return MoveResult.GAME_NOT_STARTED;
        }

        if (!game.getCurrentPlayer().getId().equals(player.getId())) {
            System.out.println("Not player's turn");
            return MoveResult.NOT_YOUR_TURN;
        }

        try {
            if (moveStr == null || (moveStr.length() != 4 && moveStr.length() != 5)) {
                System.out.println("Invalid move format: " + moveStr);
                return MoveResult.INVALID_MOVE;
            }


            Board board = new Board();
            board.loadFromFen(game.getBoardState());

            Move move;
            try {

            } catch (Exception e) {
                System.out.println("Invalid move format: " + e.getMessage());
                return MoveResult.INVALID_MOVE;
            }

            try {
                if (moveStr.length() == 5) {
                    String normalized = moveStr.toUpperCase();
                    char promoChar = normalized.charAt(4);
                    PieceType promoType;
                    switch (promoChar) {
                        case 'Q': promoType = PieceType.QUEEN; break;
                        case 'R': promoType = PieceType.ROOK; break;
                        case 'B': promoType = PieceType.BISHOP; break;
                        case 'N': promoType = PieceType.KNIGHT; break;
                        default:
                            System.out.println("Invalid promotion piece: " + promoChar);
                            return MoveResult.INVALID_MOVE;
                    }

                    // Make correct piece for the side to move
                    Side side = board.getSideToMove();
                    Piece promotionPiece = Piece.make(side, promoType);

                    move = new Move(
                            Square.fromValue(normalized.substring(0, 2)),
                            Square.fromValue(normalized.substring(2, 4)),
                            promotionPiece
                    );
                } else {
                    move = new Move(moveStr, board.getSideToMove());
                }
            } catch (Exception e) {
                System.out.println("Invalid move format: " + e.getMessage());
                return MoveResult.INVALID_MOVE;
            }

            if (board.isMoveLegal(move, true)) {
                int elapsed = (int) calculateElapsedTime(game);

                if (game.getIsWhiteTurn()) {
                    // Black just moved
                    if (game.getMoves().size() > 2) { // White and Black each have 1 move (2 total), so after that we start subtracting
                        int newBlackTime = game.getBlackTimeLeft() - elapsed;
                        game.setBlackTimeLeft(Math.max(0, newBlackTime));
                    }
                } else {
                    // White just moved
                    if (game.getMoves().size() > 1) { // First move from white doesn't consume time
                        int newWhiteTime = game.getWhiteTimeLeft() - elapsed;
                        game.setWhiteTimeLeft(Math.max(0, newWhiteTime));
                    }
                }

                board.doMove(move);
                game.setBoardState(board.getFen());
                game.addMove(moveStr);

                // Check game end conditions
                if (board.isMated()) {
                    endGame(game, game.getIsWhiteTurn() ? GameResult.BLACK_WIN : GameResult.WHITE_WIN);
                    return MoveResult.GAME_ENDED;
                } else if (board.isStaleMate() || board.isDraw()) {
                    endGame(game, GameResult.DRAW);
                    return MoveResult.GAME_ENDED;
                }

                game.setLastMoveAt(LocalDateTime.now());

                gameRepository.save(game);

                if (game.getState() == GameState.IN_PROGRESS) {
                    gameTimerService.scheduleTimeout(game);
                }
                return MoveResult.SUCCESS;
            } else {
                System.out.println("Move is not legal");
                return MoveResult.INVALID_MOVE;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return MoveResult.ERROR;
        }
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
        if (result == GameResult.WHITE_WIN || result == GameResult.WHITE_WIN_TIMEOUT || result == GameResult.WHITE_WIN_RESIGNATION) {
            winner = game.getWhitePlayer();
        } else if (result == GameResult.BLACK_WIN || result == GameResult.BLACK_WIN_TIMEOUT || result == GameResult.BLACK_WIN_RESIGNATION) {
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
        if (result == GameResult.WHITE_WIN || result == GameResult.WHITE_WIN_TIMEOUT || result == GameResult.WHITE_WIN_RESIGNATION) {
            whitePlayer.incrementWins(timeControl);
            blackPlayer.incrementLosses(timeControl);
        } else if (result == GameResult.BLACK_WIN || result == GameResult.BLACK_WIN_TIMEOUT || result == GameResult.BLACK_WIN_RESIGNATION) {
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

        GameResult result = player.equals(game.getWhitePlayer()) ?
                GameResult.BLACK_WIN_RESIGNATION : GameResult.WHITE_WIN_RESIGNATION;

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

        GameResult result = playerId.equals(game.getWhitePlayer().getId()) ?
                GameResult.BLACK_WIN_TIMEOUT : GameResult.WHITE_WIN_TIMEOUT;

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