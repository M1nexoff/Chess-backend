package com.chessapp.server.data.model;

import com.chessapp.server.data.model.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
public class Challenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenger_id")
    private User challenger;

    @ManyToOne
    @JoinColumn(name = "challenged_id")
    private User challenged;

    @Enumerated(EnumType.STRING)
    private TimeControl timeControl;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status = ChallengeStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt;

    // Constructors
    public Challenge() {}

    public Challenge(User challenger, User challenged, TimeControl timeControl) {
        this.challenger = challenger;
        this.challenged = challenged;
        this.timeControl = timeControl;
        this.expiresAt = LocalDateTime.now().plusMinutes(2); // 2 minute expiry
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getChallenger() { return challenger; }
    public void setChallenger(User challenger) { this.challenger = challenger; }

    public User getChallenged() { return challenged; }
    public void setChallenged(User challenged) { this.challenged = challenged; }

    public TimeControl getTimeControl() { return timeControl; }
    public void setTimeControl(TimeControl timeControl) { this.timeControl = timeControl; }

    public ChallengeStatus getStatus() { return status; }
    public void setStatus(ChallengeStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}