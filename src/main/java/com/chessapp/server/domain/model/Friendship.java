package com.chessapp.server.domain.model;

import com.chessapp.server.domain.enums.FriendshipStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"requester_id", "addressee_id"})
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public Friendship() {}

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
        this.status = FriendshipStatus.PENDING;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

    public User getAddressee() { return addressee; }
    public void setAddressee(User addressee) { this.addressee = addressee; }

    public FriendshipStatus getStatus() { return status; }
    public void setStatus(FriendshipStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * Returns the other user in this friendship relative to the given user.
     */
    public User getOtherUser(User user) {
        return user.getId().equals(requester.getId()) ? addressee : requester;
    }

    public boolean involves(User user) {
        return user.getId().equals(requester.getId()) || user.getId().equals(addressee.getId());
    }
}
