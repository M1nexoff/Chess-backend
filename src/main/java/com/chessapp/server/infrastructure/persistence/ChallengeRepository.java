package com.chessapp.server.infrastructure.persistence;

import com.chessapp.server.domain.model.Challenge;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.domain.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    @Query("SELECT c FROM Challenge c WHERE c.challenged = :user AND c.status = 'PENDING'")
    List<Challenge> findPendingChallengesForUser(@Param("user") User user);

    @Query("SELECT c FROM Challenge c WHERE c.challenger = :user AND c.status = 'PENDING'")
    List<Challenge> findPendingChallengesByUser(@Param("user") User user);

    @Query("SELECT c FROM Challenge c WHERE c.status = 'PENDING' AND c.expiresAt < :now")
    List<Challenge> findExpiredChallenges(@Param("now") LocalDateTime now);

    Optional<Challenge> findByIdAndStatus(Long id, ChallengeStatus status);
}
