package com.chessapp.server.application.service;

import com.chessapp.server.application.dto.LeaderboardEntryDto;
import com.chessapp.server.domain.enums.TimeControl;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.persistence.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserRepository userRepository;

    public LeaderboardServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Page<LeaderboardEntryDto> getTopPlayers(TimeControl timeControl, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<User> users = switch (timeControl) {
            case BLITZ -> userRepository.findTopByBlitzRating(pageable);
            case RAPID -> userRepository.findTopByRapidRating(pageable);
            case BULLET -> userRepository.findTopByBulletRating(pageable);
        };

        int startRank = page * size + 1;
        return users.map(user -> {
            int index = (int) (startRank + users.getContent().indexOf(user));
            return toDto(user, index, timeControl);
        });
    }

    @Override
    public int getPlayerRank(User user, TimeControl timeControl) {
        // Count how many users have a higher rating
        int userRating = user.getRatingForTimeControl(timeControl);
        long higherCount = userRepository.findAll().stream()
                .filter(u -> u.getRatingForTimeControl(timeControl) > userRating)
                .count();
        return (int) higherCount + 1;
    }

    private LeaderboardEntryDto toDto(User user, int rank, TimeControl timeControl) {
        int wins, losses, draws;
        switch (timeControl) {
            case BLITZ -> { wins = user.getBlitzWins(); losses = user.getBlitzLosses(); draws = user.getBlitzDraws(); }
            case RAPID -> { wins = user.getRapidWins(); losses = user.getRapidLosses(); draws = user.getRapidDraws(); }
            case BULLET -> { wins = user.getBulletWins(); losses = user.getBulletLosses(); draws = user.getBulletDraws(); }
            default -> { wins = 0; losses = 0; draws = 0; }
        }

        return new LeaderboardEntryDto(
                rank,
                user.getLogin(),
                user.getDisplayName(),
                user.getRatingForTimeControl(timeControl),
                wins, losses, draws,
                Boolean.TRUE.equals(user.getIsOnline())
        );
    }
}
