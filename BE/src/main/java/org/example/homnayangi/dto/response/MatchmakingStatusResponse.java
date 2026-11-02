package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.enums.MatchStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchmakingStatusResponse {
    MatchStatus status;
    UUID roomId;
    String roomCode;
    String message;
    Integer targetGroupSize;
    Integer currentQueueSize;
    LocalDateTime matchedAt;
    List<UserResponse> members;
}
