package com.chessapp.server.repository;

import com.chessapp.server.data.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);

    @Query("SELECT u FROM User u WHERE u.isOnline = true")
    List<User> findOnlineUsers();

    @Query("SELECT u FROM User u WHERE u.isOnline = true AND u.id != :userId")
    List<User> findOnlineUsersExcept(@Param("userId") Long userId);
}