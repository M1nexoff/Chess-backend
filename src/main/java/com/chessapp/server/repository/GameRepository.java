package com.chessapp.server.repository;

import com.chessapp.server.data.model.Game;
import com.chessapp.server.data.model.enums.GameState;
import com.chessapp.server.data.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("SELECT g FROM Game g WHERE (g.whitePlayer = :user OR g.blackPlayer = :user) AND g.state = :state")
    List<Game> findByPlayerAndState(@Param("user") User user, @Param("state") GameState state);

    @Query("SELECT g FROM Game g WHERE (g.whitePlayer = :user OR g.blackPlayer = :user) ORDER BY g.createdAt DESC")
    List<Game> findByPlayerOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT g FROM Game g WHERE (g.whitePlayer = :user OR g.blackPlayer = :user) AND g.state = 'IN_PROGRESS'")
    Optional<Game> findActiveGameByPlayer(@Param("user") User user);

    @Query("SELECT g FROM Game g LEFT JOIN FETCH g.moves WHERE g.id = :id")
    Optional<Game> findByIdWithMoves(@Param("id") Long id);

    @Query("SELECT g FROM Game g WHERE g.state IN :states")
    List<Game> findByStateIn(@Param("states") List<GameState> states);

}
