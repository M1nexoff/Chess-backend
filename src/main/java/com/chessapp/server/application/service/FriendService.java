package com.chessapp.server.application.service;

import com.chessapp.server.application.dto.FriendResponseDto;
import com.chessapp.server.domain.model.Friendship;
import com.chessapp.server.domain.model.User;

import java.util.List;

public interface FriendService {
    Friendship sendRequest(User requester, String targetLogin);
    Friendship acceptRequest(User user, Long friendshipId);
    Friendship declineRequest(User user, Long friendshipId);
    void removeFriend(User user, Long friendshipId);
    Friendship blockUser(User blocker, String targetLogin);
    List<FriendResponseDto> getFriends(User user);
    List<FriendResponseDto> getPendingRequests(User user);
    boolean areFriends(User user1, User user2);
    List<String> getOnlineFriendLogins(User user);
}
