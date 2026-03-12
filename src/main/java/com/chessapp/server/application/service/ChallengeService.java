package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.Challenge;
import com.chessapp.server.domain.model.Game;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.domain.enums.TimeControl;

import java.util.List;
import java.util.Optional;

public interface ChallengeService {
    Challenge createChallenge(User challenger, User challenged, TimeControl timeControl);

    Optional<Challenge> findById(Long challengeId);

    List<Challenge> findPendingChallengesForUser(User user);

    List<Challenge> findPendingChallengesByUser(User user);

    Game acceptChallenge(Long challengeId, User acceptingUser);

    void declineChallenge(Long challengeId, User decliningUser);

    void cleanupExpiredChallenges();
}