package com.chessapp.server.application.dto;

public record FriendResponseDto(
        Long friendshipId,
        String login,
        String displayName,
        int blitzRating,
        int rapidRating,
        int bulletRating,
        boolean isOnline,
        String status
) {}
