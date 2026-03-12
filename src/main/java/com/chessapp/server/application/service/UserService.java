package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(String login, String password, String displayName);

    Optional<User> authenticateUser(String login, String password);

    Optional<User> findByLogin(String login);

    User updateDisplayName(User user, String newDisplayName);

    void setUserOnline(User user, boolean online);

    List<User> getOnlineUsers();

    List<User> getOnlineUsersExcept(Long userId);

    User save(User user);
}