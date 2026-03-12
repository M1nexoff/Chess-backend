package com.chessapp.server.application.service;

import com.chessapp.server.domain.enums.GameResult;
import com.chessapp.server.domain.enums.GameState;
import com.chessapp.server.domain.enums.MoveResult;
import com.chessapp.server.domain.enums.TimeControl;
import com.chessapp.server.domain.model.Game;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.persistence.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameServiceImplTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserService userService;

    @Mock
    private RatingService ratingService;

    @Mock
    private GameTimerService gameTimerService;

    @Mock
    private GameNotificationService notificationService;

    @InjectMocks
    private GameServiceImpl gameService;

    private User whitePlayer;
    private User blackPlayer;
    private Game activeGame;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        whitePlayer = new User();
        whitePlayer.setId(1L);
        whitePlayer.setLogin("white");

        blackPlayer = new User();
        blackPlayer.setId(2L);
        blackPlayer.setLogin("black");

        activeGame = new Game(whitePlayer, blackPlayer, TimeControl.BLITZ);
        activeGame.setId(10L);
        activeGame.setState(GameState.IN_PROGRESS);
        activeGame.setMoves(new ArrayList<>());
    }

    @Test
    void testCreateGame() {
        when(gameRepository.save(any(Game.class))).thenReturn(activeGame);

        Game created = gameService.createGame(whitePlayer, blackPlayer, TimeControl.BLITZ);

        assertEquals(GameState.IN_PROGRESS, created.getState());
        assertEquals(whitePlayer, created.getWhitePlayer());
    }

    @Test
    void testMakeMove_GameNotFound() {
        when(gameRepository.findById(10L)).thenReturn(Optional.empty());

        MoveResult result = gameService.makeMove(10L, whitePlayer, "e2e4");

        assertEquals(MoveResult.GAME_NOT_FOUND, result);
    }

    @Test
    void testMakeMove_NotYourTurn() {
        when(gameRepository.findById(10L)).thenReturn(Optional.of(activeGame));

        MoveResult result = gameService.makeMove(10L, blackPlayer, "e2e4");

        assertEquals(MoveResult.NOT_YOUR_TURN, result);
    }

    @Test
    void testMakeMove_InvalidMoveFormat() {
        when(gameRepository.findById(10L)).thenReturn(Optional.of(activeGame));

        MoveResult result = gameService.makeMove(10L, whitePlayer, "invalid");

        assertEquals(MoveResult.INVALID_MOVE, result);
    }

    @Test
    void testResignGame() {
        when(gameRepository.findById(10L)).thenReturn(Optional.of(activeGame));

        gameService.resignGame(10L, whitePlayer);

        assertEquals(GameState.ENDED, activeGame.getState());
        assertEquals(GameResult.BLACK_WIN_RESIGNATION, activeGame.getResult());
        verify(gameRepository, times(1)).save(activeGame);
    }
}
