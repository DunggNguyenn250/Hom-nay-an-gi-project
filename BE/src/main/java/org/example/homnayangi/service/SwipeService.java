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
import org.example.homnayangi.helper.RedisLockHelper;
import org.example.homnayangi.mapper.SwipeMapper;
import org.example.homnayangi.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    RoomPlaceRepository roomPlaceRepository;
    SwipeMapper swipeMapper;
    RedisLockHelper redisLockHelper;
    PlatformTransactionManager transactionManager;

    public void swipe(SwipeRequest request) {
        redisLockHelper.executeWithRoomLock(request.getRoomId(), () -> {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                User user = getCurrentUser();

                Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                        .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

                if (room.getStatus() != RoomStatus.SWIPING) {
                    throw new AppException(ErrorCode.ROOM_NOT_SWIPING);
                }

                // 1. Kiểm tra User có phải thành viên của phòng không
                RoomMemberId memberId = new RoomMemberId(room.getId(), user.getId());
                if (!roomMemberRepository.existsById(memberId)) {
                    throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
                }

                Place place = placeRepository.findById(request.getPlaceId())
                        .orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND));

                // 2. Kiểm tra Place có thực sự thuộc Deck (room_places) của phòng không
                boolean isPlaceInDeck = roomPlaceRepository.existsById_RoomIdAndId_PlaceId(room.getId(), place.getId());
                if (!isPlaceInDeck) {
                    throw new AppException(ErrorCode.PLACE_NOT_IN_ROOM_DECK);
                }

                // 3. Kiểm tra xem User đã quẹt địa điểm này trong phòng chưa
                if (swipeRepository.existsByRoomIdAndUserIdAndPlaceId(room.getId(), user.getId(), place.getId())) {
                    throw new AppException(ErrorCode.ALREADY_SWIPED);
                }

                Swipe swipe = Swipe.builder()
                        .room(room)
                        .user(user)
                        .place(place)
                        .liked(request.isLiked())
                        .build();

                swipeRepository.save(swipe);

                checkForMatch(room, place, request.isLiked());
            });
        });
    }

    // 🌟 API 1: Lấy danh sách lượt quẹt cá nhân trong phòng
    @Transactional(readOnly = true)
    public List<SwipeResponse> getMySwipesInRoom(UUID roomId) {
        User currentUser = getCurrentUser();

        // Kiểm tra phòng có tồn tại không
        if (!roomRepository.existsById(roomId)) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }

        // Kiểm tra quyền thành viên trong phòng
        RoomMemberId memberId = new RoomMemberId(roomId, currentUser.getId());
        if (!roomMemberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        List<Swipe> userSwipes = swipeRepository.findAllByUserIdAndRoomId(currentUser.getId(), roomId);

        return userSwipes.stream()
                .map(swipeMapper::toSwipeResponse)
                .toList();
    }

    // 🌟 API 2: Thống kê kết quả quẹt của cả phòng (hiển thị đầy đủ tất cả quán trong Deck hoặc lịch sử quẹt/chốt khi đã cleanup)
    @Transactional(readOnly = true)
    public List<RoomPlaceResultResponse> getRoomSwipeResults(UUID roomId) {
        User currentUser = getCurrentUser();

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // Kiểm tra quyền thành viên trong phòng
        RoomMemberId memberId = new RoomMemberId(roomId, currentUser.getId());
        if (!roomMemberRepository.existsById(memberId)) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        List<Swipe> allSwipes = swipeRepository.findAllByRoomId(roomId);
        List<RoomPlace> deckPlaces = roomPlaceRepository.findAllByRoomIdWithPlace(roomId);

        Map<UUID, List<Swipe>> swipesByPlaceId = allSwipes.stream()
                .collect(Collectors.groupingBy(s -> s.getPlace().getId()));

        List<RoomPlaceResultResponse> results = new ArrayList<>();
        UUID matchedPlaceId = (room.getMatchedPlace() != null) ? room.getMatchedPlace().getId() : null;

        // Tập hợp các quán: từ bộ bài hiện tại (deckPlaces), các quán đã được quẹt (allSwipes), và quán đã chốt (matchedPlace)
        Map<UUID, Place> resultPlaces = new LinkedHashMap<>();
        deckPlaces.forEach(rp -> resultPlaces.put(rp.getPlace().getId(), rp.getPlace()));
        allSwipes.forEach(swipe -> resultPlaces.putIfAbsent(swipe.getPlace().getId(), swipe.getPlace()));
        if (matchedPlaceId != null) {
            resultPlaces.putIfAbsent(matchedPlaceId, room.getMatchedPlace());
        }

        for (Place place : resultPlaces.values()) {
            List<Swipe> placeSwipes = swipesByPlaceId.getOrDefault(place.getId(), Collections.emptyList());
            long totalLikes = placeSwipes.stream().filter(Swipe::isLiked).count();
            long totalDislikes = placeSwipes.size() - totalLikes;
            boolean isMatched = matchedPlaceId != null && matchedPlaceId.equals(place.getId());

            results.add(RoomPlaceResultResponse.builder()
                    .placeId(place.getId())
                    .placeName(place.getName())
                    .totalLikes(totalLikes)
                    .totalDislikes(totalDislikes)
                    .matched(isMatched)
                    .build());
        }

        // Sắp xếp địa điểm được yêu thích nhiều nhất lên đầu
        results.sort((a, b) -> Long.compare(b.getTotalLikes(), a.getTotalLikes()));

        return results;
    }

    private void checkForMatch(Room room, Place place, boolean isLiked) {
        // Nếu phòng không còn ở trạng thái SWIPING (đã được Host đóng hoặc chốt trước đó), không ghi đè
        if (room.getStatus() != RoomStatus.SWIPING) {
            return;
        }

        List<RoomMember> members = roomMemberRepository.findAllById_RoomId(room.getId());
        long totalMembers = members.size();
        if (totalMembers == 0) {
            return;
        }

        // 1. Luồng 1: Match tức thì khi 100% thành viên cùng bấm Like quán này
        if (isLiked) {
            long likeCount = swipeRepository.countByRoomAndPlaceAndLiked(room, place, true);
            if (likeCount == totalMembers) {
                room.setMatchedPlace(place);
                room.setStatus(RoomStatus.CLOSED);
                roomRepository.save(room);
                return;
            }
        }

        // 2. Luồng 2: Kiểm tra khi TẤT CẢ thành viên đã quẹt xong toàn bộ bộ bài (Deck)
        long totalDeckPlaces = roomPlaceRepository.countById_RoomId(room.getId());
        if (totalDeckPlaces == 0) {
            return;
        }

        long totalExpectedSwipes = totalMembers * totalDeckPlaces;
        long actualSwipes = swipeRepository.countByRoomId(room.getId());

        if (actualSwipes >= totalExpectedSwipes) {
            // Mọi người đều đã hoàn thành lượt quẹt trong bộ bài
            List<Swipe> allSwipes = swipeRepository.findAllByRoomId(room.getId());

            Map<Place, Long> likesByPlace = allSwipes.stream()
                    .filter(Swipe::isLiked)
                    .collect(Collectors.groupingBy(Swipe::getPlace, Collectors.counting()));

            if (likesByPlace.isEmpty()) {
                // Không có quán nào có lượt Like > 0 -> Hòa (toàn 0 like), chờ Host tự chọn
                return;
            }

            long maxLikes = Collections.max(likesByPlace.values());
            if (maxLikes <= 0) {
                return;
            }

            List<Place> topPlaces = likesByPlace.entrySet().stream()
                    .filter(entry -> entry.getValue() == maxLikes)
                    .map(Map.Entry::getKey)
                    .toList();

            // Nếu chỉ có DUY NHẤT 1 quán có số lượt Like cao nhất -> Tự động chốt
            if (topPlaces.size() == 1) {
                room.setMatchedPlace(topPlaces.get(0));
                room.setStatus(RoomStatus.CLOSED);
                roomRepository.save(room);
            }
            // Nếu topPlaces.size() > 1 (HÒA ĐIỂM) -> Không tự chốt, giữ nguyên để Host tự chọn qua API select-place
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
