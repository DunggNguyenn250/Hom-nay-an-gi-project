package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.SwipeRequest;
import org.example.homnayangi.dto.response.RoomPlaceResultResponse;
import org.example.homnayangi.dto.response.SwipeResponse;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.SwipeMapper;
import org.example.homnayangi.repository.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SwipeService {

    SwipeRepository swipeRepository;
    RoomRepository roomRepository;
    UserRepository userRepository;
    PlaceRepository placeRepository;
    RoomMemberRepository roomMemberRepository;
    SwipeMapper swipeMapper; // 🌟 Tiêm Mapper vào Service
    RedisTemplate<String, Object> redisTemplate;
    PlatformTransactionManager transactionManager;

    public void swipe(SwipeRequest request) {
        String lockKey = "lock:room:" + request.getRoomId();
        String lockValue = UUID.randomUUID().toString();
        boolean acquired = false;
        int retries = 5;

        while (retries > 0) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(5));
            if (Boolean.TRUE.equals(success)) {
                acquired = true;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted while waiting for lock", e);
            }
            retries--;
        }

        if (!acquired) {
            throw new AppException(ErrorCode.ROOM_BUSY);
        }

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                User user = getCurrentUser();

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
            Object currentLockValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(lockKey);
            }
        }
    }

    // 🌟 API 1: Lấy danh sách lượt quẹt cá nhân trong phòng
    @Transactional(readOnly = true)
    public List<SwipeResponse> getMySwipesInRoom(UUID roomId) {
        User currentUser = getCurrentUser();

        // Kiểm tra phòng có tồn tại không
        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }

        List<Swipe> userSwipes = swipeRepository.findAllByUserIdAndRoomId(currentUser.getId(), roomId);

        return userSwipes.stream()
                .map(swipeMapper::toSwipeResponse)
                .toList();
    }

    // 🌟 API 2: Thống kê kết quả quẹt của cả phòng
    @Transactional(readOnly = true)
    public List<RoomPlaceResultResponse> getRoomSwipeResults(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }

        List<Swipe> allSwipes = swipeRepository.findAllByRoomId(roomId);

        // Gom nhóm theo Place và đếm lượt Like / Dislike
        Map<Place, List<Swipe>> swipesByPlace = allSwipes.stream()
                .collect(Collectors.groupingBy(Swipe::getPlace));

        List<RoomPlaceResultResponse> results = new ArrayList<>();

        swipesByPlace.forEach((place, swipes) -> {
            long totalLikes = swipes.stream().filter(Swipe::isLiked).count();
            long totalDislikes = swipes.size() - totalLikes;

            results.add(RoomPlaceResultResponse.builder()
                    .placeId(place.getId())
                    .placeName(place.getName()) // Thay 'getName()' bằng trường tên tương ứng của Place entity
                    .totalLikes(totalLikes)
                    .totalDislikes(totalDislikes)
                    .build());
        });

        // Sắp xếp địa điểm được yêu thích nhiều nhất lên đầu
        results.sort((a, b) -> Long.compare(b.getTotalLikes(), a.getTotalLikes()));

        return results;
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