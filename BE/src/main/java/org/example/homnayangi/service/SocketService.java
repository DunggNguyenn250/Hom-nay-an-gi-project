package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.homnayangi.dto.response.MatchmakingStatusResponse;
import org.example.homnayangi.dto.response.PlaceResponse;
import org.example.homnayangi.dto.response.SocketEvent;
import org.example.homnayangi.dto.response.UserResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocketService {

    SimpMessagingTemplate messagingTemplate;

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    private static final String USER_MATCHMAKING_TOPIC_PREFIX = "/topic/matchmaking/";


    public void sendMemberJoined(UUID roomId, UserResponse user, int currentMembersCount) {
        SocketEvent<Object> event = SocketEvent.builder()
                .type("MEMBER_JOINED")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "user", user,
                        "currentMembersCount", currentMembersCount
                ))
                .build();

        sendToRoom(roomId, event);
    }

    public void sendMemberLeft(UUID roomId, UserResponse user, int remainingMembersCount, UUID newHostId) {
        SocketEvent<Object> event = SocketEvent.builder()
                .type("MEMBER_LEFT")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "user", user,
                        "currentMembersCount", remainingMembersCount,
                        "newHostId", newHostId != null ? newHostId.toString() : ""
                ))
                .build();

        sendToRoom(roomId, event);
    }


    public void sendGameStarted(UUID roomId) {
        SocketEvent<Object> event = SocketEvent.builder()
                .type("GAME_STARTED")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("message", "Phiên quẹt chọn địa điểm đã chính thức bắt đầu!"))
                .build();

        sendToRoom(roomId, event);
    }


    public void sendSwipeAction(UUID roomId, UUID userId, String username, boolean liked) {
        SocketEvent<Object> event = SocketEvent.builder()
                .type("SWIPE_ACTION")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "userId", userId.toString(),
                        "username", username,
                        "direction", liked ? "RIGHT" : "LEFT"
                ))
                .build();

        sendToRoom(roomId, event);
    }


    public void sendMatchFound(UUID roomId, PlaceResponse matchedPlace) {
        SocketEvent<Object> event = SocketEvent.builder()
                .type("MATCH_FOUND")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .data(Map.of(
                        "matchedPlace", matchedPlace,
                        "message", "Cả phòng đã khớp chung một quán ăn!"
                ))
                .build();

        sendToRoom(roomId, event);
    }


    public void sendRoomClosed(UUID roomId) {
        SocketEvent<Object> event = SocketEvent.builder()
                .type("ROOM_CLOSED")
                .roomId(roomId)
                .timestamp(LocalDateTime.now())
                .data(Map.of("message", "Phòng ăn đã đóng."))
                .build();

        sendToRoom(roomId, event);
    }


    public void sendMatchmakingSuccess(UUID userId, MatchmakingStatusResponse matchResult) {
        String destination = USER_MATCHMAKING_TOPIC_PREFIX + userId;
        try {
            messagingTemplate.convertAndSend(destination, matchResult);
            log.info("===> [WEBSOCKET] Đã phát sự kiện MATCHMAKING_SUCCESS tới user [{}] qua kênh [{}]", userId, destination);
        } catch (Exception e) {
            log.error("Không thể phát sự kiện MATCHMAKING_SUCCESS tới [{}]: {}", destination, e.getMessage());
        }
    }

    private void sendToRoom(UUID roomId, SocketEvent<?> event) {
        String destination = ROOM_TOPIC_PREFIX + roomId;
        try {
            messagingTemplate.convertAndSend(destination, event);
            log.info("===> [WEBSOCKET] Đã phát sự kiện [{}] tới kênh [{}]", event.getType(), destination);
        } catch (Exception e) {
            log.error("Không thể phát sự kiện WebSocket tới [{}]: {}", destination, e.getMessage());
        }
    }
}
