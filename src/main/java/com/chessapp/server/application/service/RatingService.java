package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.User;
import com.chessapp.server.domain.enums.GameResult;
import com.chessapp.server.domain.enums.TimeControl;

public interface RatingService {
    void updateRatings(User whitePlayer, User blackPlayer, GameResult result, TimeControl timeControl);

    int[] predictRatingChange(int playerRating, int opponentRating);
}