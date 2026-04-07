package com.chessapp.server.application.service;

import com.chessapp.server.application.dto.GameDataDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

import java.util.*;

@Service
public class GameServiceImpl implements GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final GameNotificationService notificationService;
    private final GameRepository gameRepository;
    private final UserService userService;
    private final RatingService ratingService;
    private final GameTimerService gameTimerService;

    public GameServiceImpl(
            @Lazy GameNotificationService notificationService,
            GameRepository gameRepository,
            UserService userService,
            RatingService ratingService,
            GameTimerService gameTimerService) {
        this.notificationService = notificationService;
        this.gameRepository = gameRepository;
        this.userService = userService;
        this.ratingService = ratingService;
        this.gameTimerService = gameTimerService;
    }

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
        logger.info("Created game between {} and {}", whitePlayer.getLogin(), blackPlayer.getLogin());
        logger.debug("Game state: {}", game.getState());

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
    public GameDataDto createGameData(Long gameId) {
        Optional<Game> gameOpt = findByIdWithMoves(gameId);
        if (gameOpt.isEmpty()) {
            return null;
        }

        Game game = gameOpt.get();
        Hibernate.initialize(game.getMoves());

        return buildGameData(game);
    }

    @Transactional
    public GameDataDto createGameData(Game game) {
        if (game.getId() != null) {
            Optional<Game> managedGame = findByIdWithMoves(game.getId());
            if (managedGame.isPresent()) {
                game = managedGame.get();
            }
        }

        Hibernate.initialize(game.getMoves());

        return buildGameData(game);
    }

    private GameDataDto buildGameData(Game game) {
        User white = game.getWhitePlayer();
        User black = game.getBlackPlayer();
        TimeControl timeControl = game.getTimeControl();

        int whiteRating = white.getRatingForTimeControl(timeControl);
        int blackRating = black.getRatingForTimeControl(timeControl);

        int[] whiteDeltas = ratingService.predictRatingChange(whiteRating, blackRating);
        int[] blackDeltas = ratingService.predictRatingChange(blackRating, whiteRating);

        return new GameDataDto(
                game.getId(),
                white.getLogin(),
                white.getDisplayName(),
                whiteRating,
                black.getLogin(),
                black.getDisplayName(),
                blackRating,
                whiteDeltas[0],
                whiteDeltas[1],
                whiteDeltas[2],
                black.getLogin(),
                black.getDisplayName(),
                blackRating,
                white.getLogin(),
                white.getDisplayName(),
                whiteRating,
                blackDeltas[0],
                blackDeltas[1],
                blackDeltas[2],
                game.getBoardState(),
                new ArrayList<>(game.getMoves()),
                game.getIsWhiteTurn(),
                game.getTimeControl().name(),
                game.getWhiteTimeLeft(),
                game.getBlackTimeLeft(),
                game.getState().name(),
                game.getResult() != null ? game.getResult().name() : null
        );
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
            logger.error("Error processing move for game {}: {}", gameId, e.getMessage(), e);
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
        // Subtract elapsed time from the CURRENT player's clock (the one making the move)
        if (game.getIsWhiteTurn()) {
            if (game.getMoves().size() >= 2) {
                game.setWhiteTimeLeft(Math.max(0, game.getWhiteTimeLeft() - elapsed));
            }
        } else {
            if (game.getMoves().size() >= 1) {
                game.setBlackTimeLeft(Math.max(0, game.getBlackTimeLeft() - elapsed));
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
        logger.info("Ending game: {}, reason = {}", game.getId(), result);
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
        } else if (result == GameResult.DRAW || result == GameResult.DRAW_BY_AGREEMENT) {
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
            logger.warn("[Timeout] Game not found: {}", gameId);
            return;
        }

        Game game = gameOpt.get();
        Hibernate.initialize(game.getMoves());

        if (game.getState() != GameState.IN_PROGRESS) {
            logger.warn("[Timeout] Game not in progress: {}", gameId);
            return;
        }

        if (!game.isPlayerInGameById(playerId)) {
            logger.warn("[Timeout] Player not in game: {}", playerId);
            return;
        }

        if (!game.getCurrentPlayer().getId().equals(playerId)) {
            logger.debug("[Timeout] Skipped — player's turn passed: {}", playerId);
            return;
        }

        GameResult result = playerId.equals(game.getWhitePlayer().getId()) ? GameResult.BLACK_WIN_TIMEOUT
                : GameResult.WHITE_WIN_TIMEOUT;

        logger.info("[Timeout] Ending game {} due to timeout. Player: {}", gameId, playerId);
        endGame(game, result);

        notificationService.notifyGameEnded(game);
    }

    @Async
    @Transactional
    public void handleTimeOutAsync(Long gameId, Long playerId) {
        handleTimeOut(gameId, playerId); // call the same logic
    }

    // --- Draw Offer Management ---
    // Maps gameId -> the login of the player who offered the draw
    private final Map<Long, String> pendingDrawOffers = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    @Transactional
    public boolean offerDraw(Long gameId, User player) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) return false;

        Game game = gameOpt.get();
        if (game.getState() != GameState.IN_PROGRESS || !game.isPlayerInGame(player)) {
            return false;
        }

        // Cannot offer draw if there's already a pending offer from this player
        String existingOffer = pendingDrawOffers.get(gameId);
        if (existingOffer != null && existingOffer.equals(player.getLogin())) {
            return false;
        }

        pendingDrawOffers.put(gameId, player.getLogin());
        logger.info("Draw offered in game {} by {}", gameId, player.getLogin());
        return true;
    }

    @Override
    @Transactional
    public boolean acceptDraw(Long gameId, User player) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) return false;

        Game game = gameOpt.get();
        if (game.getState() != GameState.IN_PROGRESS || !game.isPlayerInGame(player)) {
            return false;
        }

        String offerer = pendingDrawOffers.get(gameId);
        // Only the opponent of the offerer can accept
        if (offerer == null || offerer.equals(player.getLogin())) {
            return false;
        }

        pendingDrawOffers.remove(gameId);
        Hibernate.initialize(game.getMoves());
        endGame(game, GameResult.DRAW_BY_AGREEMENT);
        logger.info("Draw accepted in game {} by {}", gameId, player.getLogin());
        return true;
    }

    @Override
    public boolean declineDraw(Long gameId, User player) {
        String offerer = pendingDrawOffers.get(gameId);
        if (offerer == null || offerer.equals(player.getLogin())) {
            return false;
        }

        pendingDrawOffers.remove(gameId);
        logger.info("Draw declined in game {} by {}", gameId, player.getLogin());
        return true;
    }

}