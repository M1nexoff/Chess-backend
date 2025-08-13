package com.chessapp.server.service;

import com.chessapp.server.data.model.*;
import com.chessapp.server.data.model.enums.*;
import com.chessapp.server.repository.ChallengeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChallengeService {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private GameService gameService;

    public Challenge createChallenge(User challenger, User challenged, TimeControl timeControl) {
        if (challenger.equals(challenged)) {
            throw new IllegalArgumentException("You cannot challenge yourself");
        }

        Challenge challenge = new Challenge(challenger, challenged, timeControl);
        Optional<Game> existingGame = gameService.findActiveGameByPlayer(challenge.getChallenger());
        if (existingGame.isPresent()) {
            throw new IllegalStateException("Challenger already has an active game");
        }
        return challengeRepository.save(challenge);
    }

    public Optional<Challenge> findById(Long challengeId) {
        return challengeRepository.findById(challengeId);
    }

    public List<Challenge> findPendingChallengesForUser(User user) {
        return challengeRepository.findPendingChallengesForUser(user);
    }

    public List<Challenge> findPendingChallengesByUser(User user) {
        return challengeRepository.findPendingChallengesByUser(user);
    }

    @Transactional
    public Game acceptChallenge(Long challengeId, User acceptingUser) {
        Optional<Challenge> challengeOpt = challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING);

        if (!challengeOpt.isPresent()) {
            throw new IllegalArgumentException("Challenge not found or already processed");
        }

        Challenge challenge = challengeOpt.get();

        if (!challenge.getChallenged().equals(acceptingUser)) {
            throw new IllegalArgumentException("You are not the challenged player");
        }

        if (challenge.isExpired()) {
            updateChallengeStatus(challenge, ChallengeStatus.EXPIRED);
            throw new IllegalArgumentException("Challenge has expired");
        }


        // Create game
        Game game = gameService.createGame(challenge.getChallenger(), challenge.getChallenged(), challenge.getTimeControl());

        // Update challenge status
        challenge.setStatus(ChallengeStatus.ACCEPTED);
        challengeRepository.save(challenge);

        return game;
    }

    public void declineChallenge(Long challengeId, User decliningUser) {
        Optional<Challenge> challengeOpt = challengeRepository.findByIdAndStatus(challengeId, ChallengeStatus.PENDING);

        if (!challengeOpt.isPresent()) {
            throw new IllegalArgumentException("Challenge not found or already processed");
        }

        Challenge challenge = challengeOpt.get();

        if (!challenge.getChallenged().equals(decliningUser)) {
            throw new IllegalArgumentException("You are not the challenged player");
        }

        challenge.setStatus(ChallengeStatus.DECLINED);
        challengeRepository.save(challenge);
    }

    private void updateChallengeStatus(Challenge challenge, ChallengeStatus status) {
        challenge.setStatus(status);
        challengeRepository.save(challenge);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredChallenges() {
        List<Challenge> expiredChallenges = challengeRepository.findExpiredChallenges(LocalDateTime.now());
        for (Challenge challenge : expiredChallenges) {
            challenge.setStatus(ChallengeStatus.EXPIRED);
            challengeRepository.save(challenge);
        }
    }
}