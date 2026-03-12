package com.chessapp.server.application.service;

import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(String login, String password, String displayName) {
        if (userRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User();
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(password));
        user.setDisplayName(displayName);

        return userRepository.save(user);
    }

    @Override
    public Optional<User> authenticateUser(String login, String password) {
        Optional<User> userOpt = userRepository.findByLogin(login);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }

        return Optional.empty();
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Override
    public User updateDisplayName(User user, String newDisplayName) {
        user.setDisplayName(newDisplayName);
        return userRepository.save(user);
    }

    @Override
    public void setUserOnline(User user, boolean online) {
        user.setIsOnline(online);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public List<User> getOnlineUsers() {
        return userRepository.findOnlineUsers();
    }

    @Override
    public List<User> getOnlineUsersExcept(Long userId) {
        return userRepository.findOnlineUsersExcept(userId);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }
}
