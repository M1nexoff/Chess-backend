package com.chessapp.server.data.model;

import com.chessapp.server.data.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "white_player_id")
    private User whitePlayer;

    @ManyToOne
    @JoinColumn(name = "black_player_id")
    private User blackPlayer;

    @Enumerated(EnumType.STRING)
    private GameState state = GameState.WAITING;

    @Enumerated(EnumType.STRING)
    private TimeControl timeControl;

    @Enumerated(EnumType.STRING)
    private GameResult result;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Lob
    private String boardState = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"; // FEN notation

    @ElementCollection
    @CollectionTable(name = "game_moves", joinColumns = @JoinColumn(name = "game_id"))
    private List<String> moves = new ArrayList<>();

    private Integer whiteTimeLeft; // in milliseconds
    private Integer blackTimeLeft; // in milliseconds
    private LocalDateTime lastMoveAt;

    public LocalDateTime getLastMoveAt() {
        return lastMoveAt;
    }

    public void setLastMoveAt(LocalDateTime lastMoveAt) {
        this.lastMoveAt = lastMoveAt;
    }

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private Boolean isWhiteTurn = true;


    // Constructors
    public Game() {}

    public Game(User whitePlayer, User blackPlayer, TimeControl timeControl) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.timeControl = timeControl;
        this.whiteTimeLeft = timeControl.getMilliseconds();
        this.blackTimeLeft = timeControl.getMilliseconds();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getWhitePlayer() { return whitePlayer; }
    public void setWhitePlayer(User whitePlayer) { this.whitePlayer = whitePlayer; }

    public User getBlackPlayer() { return blackPlayer; }
    public void setBlackPlayer(User blackPlayer) { this.blackPlayer = blackPlayer; }

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public TimeControl getTimeControl() { return timeControl; }
    public void setTimeControl(TimeControl timeControl) { this.timeControl = timeControl; }

    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }

    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }

    public String getBoardState() { return boardState; }
    public void setBoardState(String boardState) { this.boardState = boardState; }

    public List<String> getMoves() { return moves; }
    public void setMoves(List<String> moves) { this.moves = moves; }

    public Integer getWhiteTimeLeft() { return whiteTimeLeft; }
    public void setWhiteTimeLeft(Integer whiteTimeLeft) { this.whiteTimeLeft = whiteTimeLeft; }

    public Integer getBlackTimeLeft() { return blackTimeLeft; }
    public void setBlackTimeLeft(Integer blackTimeLeft) { this.blackTimeLeft = blackTimeLeft; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public Boolean getIsWhiteTurn() { return isWhiteTurn; }
    public void setIsWhiteTurn(Boolean isWhiteTurn) { this.isWhiteTurn = isWhiteTurn; }

    public void addMove(String move) {
        this.moves.add(move);
        this.isWhiteTurn = !this.isWhiteTurn;
    }

    public User getCurrentPlayer() {
        return isWhiteTurn ? whitePlayer : blackPlayer;
    }

    public User getOpponent(User player) {
        return player.equals(whitePlayer) ? blackPlayer : whitePlayer;
    }

    public boolean isPlayerInGame(User user) {
        return user.equals(whitePlayer) || user.equals(blackPlayer);
    }

    public User getPlayerById(Long id) {
        if (whitePlayer != null && whitePlayer.getId().equals(id)) return whitePlayer;
        if (blackPlayer != null && blackPlayer.getId().equals(id)) return blackPlayer;
        return null;
    }

    public boolean isPlayerInGameById(Long id) {
        return (whitePlayer != null && whitePlayer.getId().equals(id))
                || (blackPlayer != null && blackPlayer.getId().equals(id));
    }

}