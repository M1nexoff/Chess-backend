package com.chessapp.server.data.model;

import com.chessapp.server.data.model.enums.TimeControl;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 100)
    @Column(unique = true)
    private String login;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(min = 3, max = 30)
    private String displayName;

    private Integer blitzRating = 1200;
    private Integer rapidRating = 1200;
    private Integer bulletRating = 1200;

    private Integer blitzWins = 0;
    private Integer blitzLosses = 0;
    private Integer blitzDraws = 0;

    private Integer rapidWins = 0;
    private Integer rapidLosses = 0;
    private Integer rapidDraws = 0;

    private Integer bulletWins = 0;
    private Integer bulletLosses = 0;
    private Integer bulletDraws = 0;

    private Boolean isOnline = false;
    private LocalDateTime lastSeen = LocalDateTime.now();
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public User() {}

    public User(String login, String password, String displayName) {
        this.login = login;
        this.password = password;
        this.displayName = displayName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getBlitzRating() { return blitzRating; }
    public void setBlitzRating(Integer blitzRating) { this.blitzRating = blitzRating; }

    public Integer getRapidRating() { return rapidRating; }
    public void setRapidRating(Integer rapidRating) { this.rapidRating = rapidRating; }

    public Integer getBulletRating() { return bulletRating; }
    public void setBulletRating(Integer bulletRating) { this.bulletRating = bulletRating; }

    public Integer getBlitzWins() { return blitzWins; }
    public void setBlitzWins(Integer blitzWins) { this.blitzWins = blitzWins; }

    public Integer getBlitzLosses() { return blitzLosses; }
    public void setBlitzLosses(Integer blitzLosses) { this.blitzLosses = blitzLosses; }

    public Integer getBlitzDraws() { return blitzDraws; }
    public void setBlitzDraws(Integer blitzDraws) { this.blitzDraws = blitzDraws; }

    public Integer getRapidWins() { return rapidWins; }
    public void setRapidWins(Integer rapidWins) { this.rapidWins = rapidWins; }

    public Integer getRapidLosses() { return rapidLosses; }
    public void setRapidLosses(Integer rapidLosses) { this.rapidLosses = rapidLosses; }

    public Integer getRapidDraws() { return rapidDraws; }
    public void setRapidDraws(Integer rapidDraws) { this.rapidDraws = rapidDraws; }

    public Integer getBulletWins() { return bulletWins; }
    public void setBulletWins(Integer bulletWins) { this.bulletWins = bulletWins; }

    public Integer getBulletLosses() { return bulletLosses; }
    public void setBulletLosses(Integer bulletLosses) { this.bulletLosses = bulletLosses; }

    public Integer getBulletDraws() { return bulletDraws; }
    public void setBulletDraws(Integer bulletDraws) { this.bulletDraws = bulletDraws; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getRatingForTimeControl(TimeControl timeControl) {
        switch (timeControl) {
            case BLITZ: return blitzRating;
            case RAPID: return rapidRating;
            case BULLET: return bulletRating;
            default: return 1200;
        }
    }

    public void updateRatingForTimeControl(TimeControl timeControl, int newRating) {
        switch (timeControl) {
            case BLITZ: this.blitzRating = newRating; break;
            case RAPID: this.rapidRating = newRating; break;
            case BULLET: this.bulletRating = newRating; break;
        }
    }

    public void incrementWins(TimeControl timeControl) {
        switch (timeControl) {
            case BLITZ: this.blitzWins++; break;
            case RAPID: this.rapidWins++; break;
            case BULLET: this.bulletWins++; break;
        }
    }

    public void incrementLosses(TimeControl timeControl) {
        switch (timeControl) {
            case BLITZ: this.blitzLosses++; break;
            case RAPID: this.rapidLosses++; break;
            case BULLET: this.bulletLosses++; break;
        }
    }

    public void incrementDraws(TimeControl timeControl) {
        switch (timeControl) {
            case BLITZ: this.blitzDraws++; break;
            case RAPID: this.rapidDraws++; break;
            case BULLET: this.bulletDraws++; break;
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return login != null && login.equalsIgnoreCase(user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login.toLowerCase());
    }

}