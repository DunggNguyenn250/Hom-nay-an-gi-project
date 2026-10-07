package org.example.homnayangi.service;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.SwipeRequest;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.repository.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PlatformTransactionManager transactionManager;

    public void swipe(SwipeRequest request) {
        String lockKey = "lock:room:" + request.getRoomId();
        boolean acquired = false;
        int retries = 5;
        while (retries > 0) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(5));
            if (Boolean.TRUE.equals(success)) {
                acquired = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            retries--;
        }

        if (!acquired) {
            throw new AppException(ErrorCode.ROOM_BUSY);
        }

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var context = SecurityContextHolder.getContext();
                String name = context.getAuthentication().getName();

                User user = userRepository.findByUsername(name)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                Room room = roomRepository.findById(request.getRoomId())
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

                if (room.getStatus() != RoomStatus.SWIPING) {
                    throw new AppException(ErrorCode.ROOM_NOT_SWIPING);
                }

                Place place = placeRepository.findById(request.getPlaceId())
                        .orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND));

                Swipe swipe = Swipe.builder()
                        .room(room)
                        .user(user)
                        .place(place)
                        .liked(request.isLiked())
                        .build();

                swipeRepository.save(swipe);

                if (request.isLiked()) {
                    checkForMatch(room, place);
                }
            });
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void checkForMatch(Room room, Place place) {
        List<RoomMember> members = roomMemberRepository.findAllById_RoomId(room.getId());
        long totalMembers = members.size();

        long likeCount = swipeRepository.countByRoomAndPlaceAndLiked(room, place, true);

        if (likeCount == totalMembers) {
            room.setMatchedPlace(place);
            room.setStatus(RoomStatus.CLOSED);
            roomRepository.save(room);
        }
    }

}
