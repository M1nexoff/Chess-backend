package com.chessapp.server.application.service;

import com.chessapp.server.application.dto.GameDataDto;
import com.chessapp.server.domain.model.Game;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.domain.enums.GameResult;
import com.chessapp.server.domain.enums.MoveResult;
import com.chessapp.server.domain.enums.TimeControl;

import java.util.List;
import java.util.Optional;

public interface GameService {
    void endAllUnfinishedGames();

    Game createGame(User whitePlayer, User blackPlayer, TimeControl timeControl);

    Optional<Game> findByIdWithMoves(Long id);

    Optional<Game> findById(Long gameId);

    Optional<Game> findActiveGameByPlayer(User user);

    List<Game> findGamesByPlayer(User user);

    GameDataDto createGameData(Long gameId);

    GameDataDto createGameData(Game game);

    MoveResult makeMove(Long gameId, User player, String moveStr);

    void endGame(Game game, GameResult result);

    void resignGame(Long gameId, User player);

    void handleTimeOut(Long gameId, Long playerId);

    void handleTimeOutAsync(Long gameId, Long playerId);

    boolean offerDraw(Long gameId, User player);

    boolean acceptDraw(Long gameId, User player);

    boolean declineDraw(Long gameId, User player);
}