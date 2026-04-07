package com.chessapp.server.infrastructure.persistence;

import com.chessapp.server.domain.enums.FriendshipStatus;
import com.chessapp.server.domain.model.Friendship;
import com.chessapp.server.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE " +
            "((f.requester = :user1 AND f.addressee = :user2) OR " +
            " (f.requester = :user2 AND f.addressee = :user1))")
    Optional<Friendship> findBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :user OR f.addressee = :user) AND f.status = :status")
    List<Friendship> findByUserAndStatus(@Param("user") User user, @Param("status") FriendshipStatus status);

    @Query("SELECT f FROM Friendship f WHERE f.addressee = :user AND f.status = 'PENDING'")
    List<Friendship> findPendingForUser(@Param("user") User user);

    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requester = :user OR f.addressee = :user) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("user") User user);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
            "((f.requester = :user1 AND f.addressee = :user2) OR " +
            " (f.requester = :user2 AND f.addressee = :user1)) AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
            "f.requester = :blocker AND f.addressee = :blocked AND f.status = 'BLOCKED'")
    boolean isBlocked(@Param("blocker") User blocker, @Param("blocked") User blocked);
}
