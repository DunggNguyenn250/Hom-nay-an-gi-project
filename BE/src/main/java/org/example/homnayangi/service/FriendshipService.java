package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.response.FriendshipResponse;
import org.example.homnayangi.dto.response.UserResponse;
import org.example.homnayangi.entity.Friendship;
import org.example.homnayangi.entity.FriendshipId;
import org.example.homnayangi.entity.User;
import org.example.homnayangi.enums.FriendshipStatus;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.FriendshipMapper;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.FriendshipRepository;
import org.example.homnayangi.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class FriendshipService {

    FriendshipRepository friendshipRepository;
    UserRepository userRepository;
    FriendshipMapper friendshipMapper;
    UserMapper userMapper;

    public FriendshipResponse sendFriendRequest(UUID addresseeId) {
        User requester = getCurrentUser();

        if (requester.getId().equals(addresseeId)) {
            throw new AppException(ErrorCode.CANNOT_FRIEND_SELF);
        }

        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var existingFriendshipOpt = friendshipRepository.findFriendshipBetween(requester.getId(), addresseeId);

        if (existingFriendshipOpt.isPresent()) {
            Friendship friendship = existingFriendshipOpt.get();
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new AppException(ErrorCode.FRIENDSHIP_ALREADY_EXISTS);
            } else if (friendship.getStatus() == FriendshipStatus.PENDING) {
                // If the other user already sent a pending request, we auto-accept it!
                if (friendship.getRequester().getId().equals(addresseeId)) {
                    friendship.setStatus(FriendshipStatus.ACCEPTED);
                    return friendshipMapper.toFriendshipResponse(friendshipRepository.save(friendship));
                }
                throw new AppException(ErrorCode.FRIENDSHIP_ALREADY_EXISTS);
            } else {
                throw new AppException(ErrorCode.FRIENDSHIP_ALREADY_EXISTS);
            }
        }

        FriendshipId friendshipId = new FriendshipId(requester.getId(), addresseeId);
        Friendship friendship = Friendship.builder()
                .id(friendshipId)
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        return friendshipMapper.toFriendshipResponse(friendshipRepository.save(friendship));
    }

    public FriendshipResponse acceptFriendRequest(UUID requesterId) {
        User currentUser = getCurrentUser();

        FriendshipId id = new FriendshipId(requesterId, currentUser.getId());
        Friendship friendship = friendshipRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(ErrorCode.FRIENDSHIP_ALREADY_EXISTS);
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return friendshipMapper.toFriendshipResponse(friendshipRepository.save(friendship));
    }

    public void declineOrRemoveFriendship(UUID targetUserId) {
        User currentUser = getCurrentUser();

        Friendship friendship = friendshipRepository.findFriendshipBetween(currentUser.getId(), targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFriends() {
        User currentUser = getCurrentUser();
        List<Friendship> friendships = friendshipRepository.findAllByUserIdAndStatus(currentUser.getId(), FriendshipStatus.ACCEPTED);
        
        return friendships.stream()
                .map(f -> {
                    User friend = f.getRequester().getId().equals(currentUser.getId()) ? f.getAddressee() : f.getRequester();
                    return userMapper.toUserResponse(friend);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getPendingIncomingRequests() {
        User currentUser = getCurrentUser();
        List<Friendship> friendships = friendshipRepository.findAllByAddresseeIdAndStatus(currentUser.getId(), FriendshipStatus.PENDING);
        
        return friendships.stream()
                .map(friendshipMapper::toFriendshipResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendshipResponse> getPendingOutgoingRequests() {
        User currentUser = getCurrentUser();
        List<Friendship> friendships = friendshipRepository.findAllByRequesterIdAndStatus(currentUser.getId(), FriendshipStatus.PENDING);
        
        return friendships.stream()
                .map(friendshipMapper::toFriendshipResponse)
                .toList();
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }
}
