package com.chessapp.server.application.service;

import com.chessapp.server.application.dto.FriendResponseDto;
import com.chessapp.server.domain.enums.FriendshipStatus;
import com.chessapp.server.domain.model.Friendship;
import com.chessapp.server.domain.model.User;
import com.chessapp.server.infrastructure.persistence.FriendshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    private static final Logger logger = LoggerFactory.getLogger(FriendServiceImpl.class);

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    public FriendServiceImpl(FriendshipRepository friendshipRepository, UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public Friendship sendRequest(User requester, String targetLogin) {
        if (requester.getLogin().equalsIgnoreCase(targetLogin)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        User addressee = userService.findByLogin(targetLogin)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetLogin));

        // Check if blocked
        if (friendshipRepository.isBlocked(addressee, requester)) {
            throw new IllegalArgumentException("Cannot send friend request to this user");
        }

        // Check existing relationship
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(requester, addressee);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            switch (f.getStatus()) {
                case ACCEPTED -> throw new IllegalArgumentException("Already friends");
                case PENDING -> throw new IllegalArgumentException("Friend request already pending");
                case BLOCKED -> throw new IllegalArgumentException("Cannot send friend request to this user");
                case DECLINED -> {
                    // Allow re-sending after decline
                    f.setRequester(requester);
                    f.setAddressee(addressee);
                    f.setStatus(FriendshipStatus.PENDING);
                    return friendshipRepository.save(f);
                }
            }
        }

        Friendship friendship = new Friendship(requester, addressee);
        logger.info("Friend request sent: {} -> {}", requester.getLogin(), targetLogin);
        return friendshipRepository.save(friendship);
    }

    @Override
    @Transactional
    public Friendship acceptRequest(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the addressee can accept this request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException("Request is no longer pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        logger.info("Friend request accepted: {} <-> {}", friendship.getRequester().getLogin(), user.getLogin());
        return friendshipRepository.save(friendship);
    }

    @Override
    @Transactional
    public Friendship declineRequest(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the addressee can decline this request");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        logger.info("Friend request declined: {} by {}", friendshipId, user.getLogin());
        return friendshipRepository.save(friendship);
    }

    @Override
    @Transactional
    public void removeFriend(User user, Long friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (!friendship.involves(user)) {
            throw new IllegalArgumentException("You are not part of this friendship");
        }

        friendshipRepository.delete(friendship);
        logger.info("Friendship removed: {}", friendshipId);
    }

    @Override
    @Transactional
    public Friendship blockUser(User blocker, String targetLogin) {
        User blocked = userService.findByLogin(targetLogin)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetLogin));

        if (blocker.getLogin().equalsIgnoreCase(targetLogin)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(blocker, blocked);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            f.setRequester(blocker);
            f.setAddressee(blocked);
            f.setStatus(FriendshipStatus.BLOCKED);
            return friendshipRepository.save(f);
        }

        Friendship friendship = new Friendship(blocker, blocked);
        friendship.setStatus(FriendshipStatus.BLOCKED);
        logger.info("User blocked: {} blocked {}", blocker.getLogin(), targetLogin);
        return friendshipRepository.save(friendship);
    }

    @Override
    public List<FriendResponseDto> getFriends(User user) {
        return friendshipRepository.findAcceptedFriendships(user).stream()
                .map(f -> toDto(f, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<FriendResponseDto> getPendingRequests(User user) {
        return friendshipRepository.findPendingForUser(user).stream()
                .map(f -> toDto(f, user))
                .collect(Collectors.toList());
    }

    @Override
    public boolean areFriends(User user1, User user2) {
        return friendshipRepository.areFriends(user1, user2);
    }

    @Override
    public List<String> getOnlineFriendLogins(User user) {
        return friendshipRepository.findAcceptedFriendships(user).stream()
                .map(f -> f.getOtherUser(user))
                .filter(u -> Boolean.TRUE.equals(u.getIsOnline()))
                .map(User::getLogin)
                .collect(Collectors.toList());
    }

    private FriendResponseDto toDto(Friendship friendship, User perspective) {
        User other = friendship.getOtherUser(perspective);
        return new FriendResponseDto(
                friendship.getId(),
                other.getLogin(),
                other.getDisplayName(),
                other.getBlitzRating(),
                other.getRapidRating(),
                other.getBulletRating(),
                Boolean.TRUE.equals(other.getIsOnline()),
                friendship.getStatus().name()
        );
    }
}
