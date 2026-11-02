package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicRoomResponse {
    UUID roomId;
    String name;
    String roomCode;
    UserResponse host;
    BigDecimal latitude;
    BigDecimal longitude;
    Double radius;
    Double distance;
    Integer currentMembers;
    Integer maxMembers;
    LocalDateTime createdAt;
}
