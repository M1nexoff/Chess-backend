package com.chessapp.server.application.dto;

import java.time.LocalDateTime;

public record GameHistoryDto(
        Long gameId,
        String opponentLogin,
        String opponentDisplayName,
        int opponentRating,
        String myColor,
        String result,
        int ratingChange,
        String timeControl,
        int movesCount,
        LocalDateTime playedAt
) {}
