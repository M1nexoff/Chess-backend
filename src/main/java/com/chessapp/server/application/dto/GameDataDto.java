package com.chessapp.server.application.dto;

import java.util.List;

public record GameDataDto(
        Long gameId,

        String whitePlayer,
        String whiteDisplayName,
        int whiteRating,
        String whiteOpponent,
        String whiteOpponentDisplayName,
        int whiteOpponentRating,
        int whiteWinDelta,
        int whiteDrawDelta,
        int whiteLossDelta,

        String blackPlayer,
        String blackDisplayName,
        int blackRating,
        String blackOpponent,
        String blackOpponentDisplayName,
        int blackOpponentRating,
        int blackWinDelta,
        int blackDrawDelta,
        int blackLossDelta,

        String boardState,
        List<String> moves,
        boolean isWhiteTurn,
        String timeControl,
        int whiteTimeLeft,
        int blackTimeLeft,
        String state,
        String result
) {}
