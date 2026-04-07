package com.chessapp.server.application.dto;

public record LeaderboardEntryDto(
        int rank,
        String login,
        String displayName,
        int rating,
        int wins,
        int losses,
        int draws,
        boolean isOnline
) {}
