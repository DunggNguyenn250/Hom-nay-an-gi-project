package org.example.homnayangi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.example.homnayangi.dto.MatchTicket;
import org.example.homnayangi.dto.request.MatchmakingRequest;
import org.example.homnayangi.dto.response.MatchmakingStatusResponse;
import org.example.homnayangi.dto.response.PublicRoomResponse;
import org.example.homnayangi.dto.response.RoomResponse;
import org.example.homnayangi.dto.response.UserResponse;
import org.example.homnayangi.entity.*;
import org.example.homnayangi.enums.MatchStatus;
import org.example.homnayangi.enums.RoomStatus;
import org.example.homnayangi.enums.RoomType;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.helper.GeoUtils;
import org.example.homnayangi.mapper.UserMapper;
import org.example.homnayangi.repository.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MatchmakingService {

    static final String QUEUE_KEY = "matchmaking:queue";
    static final String TICKET_PREFIX = "matchmaking:ticket:";
    static final String RESULT_PREFIX = "matchmaking:result:";
    static final String LOCK_KEY = "lock:matchmaking";

    static final Duration TICKET_TTL = Duration.ofMinutes(3);
    static final Duration RESULT_TTL = Duration.ofMinutes(10);

    final StringRedisTemplate redisTemplate;
    final ObjectMapper objectMapper;
    final RoomRepository roomRepository;
    final RoomMemberRepository roomMemberRepository;
    final UserRepository userRepository;
    final RoomService roomService;
    final UserMapper userMapper;
    final PlatformTransactionManager transactionManager;
    final SocketService socketService;

    @PostConstruct
    public void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public MatchmakingStatusResponse findOrQueueMatch(MatchmakingRequest request) {
        User currentUser = getCurrentUser();
        UUID currentUserId = currentUser.getId();

        // Xóa kết quả ghép cũ khi người dùng bắt đầu tìm kiếm mới.
        redisTemplate.delete(RESULT_PREFIX + currentUserId);

        double userLat = request.getLatitude() != null ? request.getLatitude().doubleValue() : 21.0;
        double userLon = request.getLongitude() != null ? request.getLongitude().doubleValue() : 105.0;
        double userRadius = (request.getRadius() != null && request.getRadius() > 0) ? request.getRadius() : 5.0;
        int targetSize = (request.getTargetGroupSize() != null && request.getTargetGroupSize() >= 2)
                ? request.getTargetGroupSize()
                : 2;

        Room suitablePublicRoom = findSuitablePublicRoom(currentUser, userLat, userLon, userRadius);
        if (suitablePublicRoom != null) {
            log.info("Người dùng {} được ghép ngay vào phòng công khai sẵn có: {}", currentUser.getUsername(), suitablePublicRoom.getRoomCode());
            RoomResponse joinedRoom = roomService.joinRoom(suitablePublicRoom.getRoomCode());

            MatchmakingStatusResponse matchedResponse = MatchmakingStatusResponse.builder()
                    .status(MatchStatus.MATCHED)
                    .roomId(suitablePublicRoom.getId())
                    .roomCode(suitablePublicRoom.getRoomCode())
                    .message("Đã tìm thấy và tự động tham gia vào phòng ăn quanh bạn: " + suitablePublicRoom.getName())
                    .targetGroupSize(suitablePublicRoom.getMaxMembers())
                    .matchedAt(LocalDateTime.now())
                    .members(joinedRoom.getMembers())
                    .build();

            saveMatchResult(currentUserId, matchedResponse);
            removeTicket(currentUserId);

            socketService.sendMatchmakingSuccess(currentUserId, matchedResponse);

            return matchedResponse;
        }

        return processSoloQueue(currentUser, userLat, userLon, userRadius, targetSize);
    }

    public MatchmakingStatusResponse getMatchStatus() {
        try {
            User currentUser = getCurrentUser();
            UUID currentUserId = currentUser.getId();

            MatchmakingStatusResponse cachedResult = getMatchResult(currentUserId);
            if (cachedResult != null) {
                if (cachedResult.getRoomId() != null) {
                    Room room = roomRepository.findById(cachedResult.getRoomId()).orElse(null);
                    if (room == null || room.getStatus() == RoomStatus.CLOSED) {
                        log.warn("Phòng {} trong kết quả ghép cũ không còn tồn tại hoặc đã đóng, xóa cache Redis", cachedResult.getRoomId());
                        redisTemplate.delete(RESULT_PREFIX + currentUserId);
                        cachedResult = null;
                    }
                }
                if (cachedResult != null) {
                    return cachedResult;
                }
            }

            MatchTicket ticket = getTicket(currentUserId);
            if (ticket != null) {
                Long queueSize = redisTemplate.opsForSet().size(QUEUE_KEY);
                return MatchmakingStatusResponse.builder()
                        .status(MatchStatus.WAITING)
                        .message("Đang tìm người ăn cùng trong bán kính " + ticket.getRadius() + " km...")
                        .targetGroupSize(ticket.getTargetGroupSize())
                        .currentQueueSize(queueSize != null ? queueSize.intValue() : 1)
                        .build();
            }

            return MatchmakingStatusResponse.builder()
                    .status(MatchStatus.TIMEOUT)
                    .message("Không tìm thấy hàng đợi active của bạn. Bạn có thể bắt đầu tìm kiếm mới.")
                    .build();
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trạng thái ghép phòng: {}", e.getMessage(), e);
            return MatchmakingStatusResponse.builder()
                    .status(MatchStatus.TIMEOUT)
                    .message("Không thể lấy trạng thái hàng đợi lúc này.")
                    .build();
        }
    }

    public void cancelMatch() {
        User currentUser = getCurrentUser();
        UUID currentUserId = currentUser.getId();

        removeTicket(currentUserId);
        redisTemplate.delete(RESULT_PREFIX + currentUserId);
        log.info("Người dùng {} đã hủy tìm kiếm ghép phòng", currentUser.getUsername());
    }

    public List<PublicRoomResponse> getNearbyPublicRooms(Double userLat, Double userLon, Double radius) {
        double lat = userLat != null ? userLat : 21.0;
        double lon = userLon != null ? userLon : 105.0;
        double maxRadius = (radius != null && radius > 0) ? radius : 5.0;

        List<Room> publicRooms = roomRepository.findByStatusAndRoomType(RoomStatus.OPEN, RoomType.PUBLIC);
        if (publicRooms == null || publicRooms.isEmpty()) {
            return Collections.emptyList();
        }

        return publicRooms.stream()
                .filter(room -> room != null && room.getLatitude() != null && room.getLongitude() != null)
                .map(room -> {
                    double dist = GeoUtils.calculateDistance(
                            lat,
                            lon,
                            room.getLatitude().doubleValue(),
                            room.getLongitude().doubleValue()
                    );
                    int currentMembers = (int) roomMemberRepository.countById_RoomId(room.getId());
                    return new Object[]{room, dist, currentMembers};
                })
                .filter(arr -> {
                    double dist = (double) arr[1];
                    Room room = (Room) arr[0];
                    int currentMembers = (int) arr[2];
                    boolean withinRadius = dist <= maxRadius;
                    boolean notFull = room.getMaxMembers() == null || currentMembers < room.getMaxMembers();
                    return withinRadius && notFull;
                })
                .sorted(Comparator.comparingDouble(arr -> (double) arr[1]))
                .map(arr -> {
                    Room room = (Room) arr[0];
                    double dist = (double) arr[1];
                    int currentMembers = (int) arr[2];

                    UserResponse hostResponse = room.getHost() != null ? userMapper.toUserResponse(room.getHost()) : null;

                    return PublicRoomResponse.builder()
                            .roomId(room.getId())
                            .name(room.getName())
                            .roomCode(room.getRoomCode())
                            .host(hostResponse)
                            .latitude(room.getLatitude())
                            .longitude(room.getLongitude())
                            .radius(room.getRadius())
                            .distance(Math.round(dist * 100.0) / 100.0) // Sửa phép nhân * 100.0
                            .currentMembers(currentMembers)
                            .maxMembers(room.getMaxMembers())
                            .createdAt(room.getCreatedAt())
                            .build();
                })
                .toList();
    }

    private Room findSuitablePublicRoom(User currentUser, double userLat, double userLon, double userRadius) {
        List<Room> publicRooms = roomRepository.findByStatusAndRoomType(RoomStatus.OPEN, RoomType.PUBLIC);
        if (publicRooms == null) return null;

        return publicRooms.stream()
                .filter(room -> room != null && room.getLatitude() != null && room.getLongitude() != null)
                .filter(room -> !roomMemberRepository.existsById(new RoomMemberId(room.getId(), currentUser.getId())))
                .map(room -> {
                    double distance = GeoUtils.calculateDistance(
                            userLat,
                            userLon,
                            room.getLatitude().doubleValue(),
                            room.getLongitude().doubleValue()
                    );
                    return new AbstractMap.SimpleEntry<>(room, distance);
                })
                .filter(entry -> {
                    Room room = entry.getKey();
                    double distance = entry.getValue();
                    double roomRadius = room.getRadius() != null ? room.getRadius() : 5.0;

                    boolean inRange = distance <= userRadius && distance <= roomRadius;
                    if (!inRange) return false;

                    long memberCount = roomMemberRepository.countById_RoomId(room.getId());
                    return room.getMaxMembers() == null || memberCount < room.getMaxMembers();
                })
                .min(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private MatchmakingStatusResponse processSoloQueue(
            User currentUser,
            double userLat,
            double userLon,
            double userRadius,
            int targetSize
    ) {
        UUID currentUserId = currentUser.getId();

        MatchTicket currentTicket = MatchTicket.builder()
                .userId(currentUserId)
                .username(currentUser.getUsername())
                .latitude(userLat)
                .longitude(userLon)
                .radius(userRadius)
                .targetGroupSize(targetSize)
                .queuedAt(LocalDateTime.now())
                .build();

        Boolean acquiredLock = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, currentUserId.toString(), Duration.ofSeconds(5));
        try {
            Set<String> queuedUserIds = redisTemplate.opsForSet().members(QUEUE_KEY);
            List<MatchTicket> candidateGroup = new ArrayList<>();
            candidateGroup.add(currentTicket);

            if (queuedUserIds != null && !queuedUserIds.isEmpty()) {
                for (String queuedIdStr : queuedUserIds) {
                    if (queuedIdStr.equals(currentUserId.toString())) continue;

                    UUID candidateId = UUID.fromString(queuedIdStr);
                    MatchTicket candidateTicket = getTicket(candidateId);

                    if (candidateTicket == null) {
                        redisTemplate.opsForSet().remove(QUEUE_KEY, queuedIdStr);
                        continue;
                    }

                    double dist = GeoUtils.calculateDistance(
                            userLat, userLon,
                            candidateTicket.getLatitude(), candidateTicket.getLongitude()
                    );

                    double maxAllowableDist = Math.min(userRadius, candidateTicket.getRadius());
                    if (dist <= maxAllowableDist) {
                        candidateGroup.add(candidateTicket);
                        if (candidateGroup.size() >= targetSize) {
                            break;
                        }
                    }
                }
            }

            if (candidateGroup.size() >= targetSize) {
                return createMatchedRoomForGroup(candidateGroup, currentUser);
            }

            saveTicket(currentUserId, currentTicket);
            redisTemplate.opsForSet().add(QUEUE_KEY, currentUserId.toString());

            return MatchmakingStatusResponse.builder()
                    .status(MatchStatus.WAITING)
                    .message("Đang tìm người ăn cùng trong bán kính " + userRadius + " km...")
                    .targetGroupSize(targetSize)
                    .currentQueueSize(candidateGroup.size())
                    .build();

        } finally {
            if (Boolean.TRUE.equals(acquiredLock)) {
                redisTemplate.delete(LOCK_KEY);
            }
        }
    }

    private MatchmakingStatusResponse createMatchedRoomForGroup(List<MatchTicket> group, User creator) {
        double avgLat = group.stream().mapToDouble(MatchTicket::getLatitude).average().orElse(21.0);
        double avgLon = group.stream().mapToDouble(MatchTicket::getLongitude).average().orElse(105.0);
        double avgRadius = group.stream().mapToDouble(MatchTicket::getRadius).average().orElse(5.0);

        Room newRoom = new TransactionTemplate(transactionManager).execute(status -> {
            Room room = Room.builder()
                    .name("Phòng ghép tự động #" + RandomStringUtils.randomNumeric(4))
                    .roomCode(generateUniqueRoomCode())
                    .host(creator)
                    .roomType(RoomType.PUBLIC)
                    .maxMembers(group.size())
                    .latitude(BigDecimal.valueOf(avgLat))
                    .longitude(BigDecimal.valueOf(avgLon))
                    .radius(avgRadius)
                    .deckSize(15)
                    .status(RoomStatus.OPEN)
                    .createdAt(LocalDateTime.now())
                    .build();

            Room savedRoom = roomRepository.save(room);

            List<RoomMember> members = new ArrayList<>();
            for (MatchTicket ticket : group) {
                User memberUser = userRepository.findById(ticket.getUserId())
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

                boolean isHost = memberUser.getId().equals(creator.getId());
                members.add(RoomMember.builder()
                        .id(new RoomMemberId(savedRoom.getId(), memberUser.getId()))
                        .room(savedRoom)
                        .user(memberUser)
                        .ready(isHost)
                        .joinedAt(LocalDateTime.now())
                        .build());
            }
            roomMemberRepository.saveAll(members);

            return savedRoom;
        });

        if (newRoom == null) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        try {
            roomService.generateOrGetDeck(newRoom.getId());
        } catch (Exception e) {
            log.warn("Không thể tự động sinh deck quán ăn cho phòng ghép {}: {}", newRoom.getId(), e.getMessage());
        }

        List<UserResponse> memberResponses = group.stream()
                .map(t -> userRepository.findById(t.getUserId()).map(userMapper::toUserResponse).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        MatchmakingStatusResponse resultResponse = MatchmakingStatusResponse.builder()
                .status(MatchStatus.MATCHED)
                .roomId(newRoom.getId())
                .roomCode(newRoom.getRoomCode())
                .message("Đã ghép nhóm thành công! Chào mừng bạn vào phòng " + newRoom.getName())
                .targetGroupSize(group.size())
                .matchedAt(LocalDateTime.now())
                .members(memberResponses)
                .build();

        for (MatchTicket ticket : group) {
            removeTicket(ticket.getUserId());
            saveMatchResult(ticket.getUserId(), resultResponse);

            socketService.sendMatchmakingSuccess(ticket.getUserId(), resultResponse);
        }

        log.info("Ghép nhóm thành công {} người vào phòng {}", group.size(), newRoom.getRoomCode());
        return resultResponse;
    }

    private void saveTicket(UUID userId, MatchTicket ticket) {
        try {
            String json = objectMapper.writeValueAsString(ticket);
            redisTemplate.opsForValue().set(TICKET_PREFIX + userId, json, TICKET_TTL);
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse ticket sang JSON: {}", e.getMessage());
        }
    }

    private MatchTicket getTicket(UUID userId) {
        String json = redisTemplate.opsForValue().get(TICKET_PREFIX + userId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, MatchTicket.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi đọc ticket từ JSON: {}", e.getMessage());
            return null;
        }
    }

    private void removeTicket(UUID userId) {
        redisTemplate.delete(TICKET_PREFIX + userId);
        redisTemplate.opsForSet().remove(QUEUE_KEY, userId.toString());
    }

    private void saveMatchResult(UUID userId, MatchmakingStatusResponse result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(RESULT_PREFIX + userId, json, RESULT_TTL);
        } catch (JsonProcessingException e) {
            log.error("Lỗi lưu match result: {}", e.getMessage());
        }
    }

    private MatchmakingStatusResponse getMatchResult(UUID userId) {
        String json = redisTemplate.opsForValue().get(RESULT_PREFIX + userId);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, MatchmakingStatusResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Lỗi đọc match result: {}", e.getMessage());
            return null;
        }
    }

    private User getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        return userRepository.findByUsername(name)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (roomRepository.findByRoomCode(roomCode).isPresent());
        return roomCode;
    }
}
