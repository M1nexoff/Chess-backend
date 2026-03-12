package com.chessapp.server.application.dto;

public class UserResponseDto {
    private Long id;
    private String login;
    private String displayName;
    private Integer blitzRating;
    private Integer rapidRating;
    private Integer bulletRating;
    private Boolean isOnline;

    public UserResponseDto(Long id, String login, String displayName, Integer blitzRating, Integer rapidRating,
            Integer bulletRating, Boolean isOnline) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
        this.blitzRating = blitzRating;
        this.rapidRating = rapidRating;
        this.bulletRating = bulletRating;
        this.isOnline = isOnline;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getBlitzRating() {
        return blitzRating;
    }

    public Integer getRapidRating() {
        return rapidRating;
    }

    public Integer getBulletRating() {
        return bulletRating;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }
}
