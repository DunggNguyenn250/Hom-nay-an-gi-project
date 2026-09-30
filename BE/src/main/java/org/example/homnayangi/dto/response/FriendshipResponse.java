package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.enums.FriendshipStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendshipResponse {
    UserResponse requester;
    UserResponse addressee;
    FriendshipStatus status;
    LocalDateTime createdAt;
}
