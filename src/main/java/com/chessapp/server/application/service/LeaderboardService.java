package com.chessapp.server.application.service;

import com.chessapp.server.application.dto.LeaderboardEntryDto;
import com.chessapp.server.domain.enums.TimeControl;
import com.chessapp.server.domain.model.User;
import org.springframework.data.domain.Page;

public interface LeaderboardService {
    Page<LeaderboardEntryDto> getTopPlayers(TimeControl timeControl, int page, int size);
    int getPlayerRank(User user, TimeControl timeControl);
}
