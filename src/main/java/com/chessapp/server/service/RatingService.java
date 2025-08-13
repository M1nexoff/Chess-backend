package com.chessapp.server.service;

import com.chessapp.server.data.model.*;
import com.chessapp.server.data.model.enums.GameResult;
import com.chessapp.server.data.model.enums.TimeControl;
import org.springframework.stereotype.Service;

@Service
public class RatingService {

    private static final int K_FACTOR = 32;

    public void updateRatings(User whitePlayer, User blackPlayer, GameResult result, TimeControl timeControl) {
        int whiteRating = whitePlayer.getRatingForTimeControl(timeControl);
        int blackRating = blackPlayer.getRatingForTimeControl(timeControl);

        double whiteScore = getScoreForResult(result, true);
        double blackScore = getScoreForResult(result, false);

        int[] newRatings = calculateNewRatings(whiteRating, blackRating, whiteScore, blackScore);

        whitePlayer.updateRatingForTimeControl(timeControl, newRatings[0]);
        blackPlayer.updateRatingForTimeControl(timeControl, newRatings[1]);
    }

    private double getScoreForResult(GameResult result, boolean isWhite) {
        switch (result) {
            case WHITE_WIN:
            case WHITE_WIN_TIMEOUT:
            case WHITE_WIN_RESIGNATION:
                return isWhite ? 1.0 : 0.0;
            case BLACK_WIN:
            case BLACK_WIN_TIMEOUT:
            case BLACK_WIN_RESIGNATION:
                return isWhite ? 0.0 : 1.0;
            case DRAW:
                return 0.5;
            default:
                return 0.5;
        }
    }

    private int[] calculateNewRatings(int whiteRating, int blackRating, double whiteScore, double blackScore) {
        double whiteExpected = 1.0 / (1.0 + Math.pow(10, (blackRating - whiteRating) / 400.0));
        double blackExpected = 1.0 / (1.0 + Math.pow(10, (whiteRating - blackRating) / 400.0));

        int newWhiteRating = (int) Math.round(whiteRating + K_FACTOR * (whiteScore - whiteExpected));
        int newBlackRating = (int) Math.round(blackRating + K_FACTOR * (blackScore - blackExpected));

        return new int[]{newWhiteRating, newBlackRating};
    }
    public int[] predictRatingChange(int playerRating, int opponentRating) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));
        int k = 32;

        int winChange = (int) Math.round(k * (1 - expectedScore));
        int drawChange = (int) Math.round(k * (0.5 - expectedScore));
        int lossChange = (int) Math.round(k * (0 - expectedScore));

        return new int[]{winChange, drawChange, lossChange};
    }

}