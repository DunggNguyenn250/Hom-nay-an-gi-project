package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.enums.RoomStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {
    UUID id;
    String name;
    String roomCode;
    UserResponse host;
    RoomStatus status;
    PlaceResponse matchedPlace;
    LocalDateTime createdAt;
    List<UserResponse> members;
}
