package com.chessapp.server.application.dto;

public record PlayerSearchDto(
        Long id,
        String login,
        String displayName,
        int blitzRating,
        int rapidRating,
        int bulletRating,
        boolean isOnline
) {}
