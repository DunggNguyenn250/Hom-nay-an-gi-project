package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.enums.Role;
import org.example.homnayangi.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    UUID id;
    String username;
    String email;
    String avatarUrl;
    UserStatus status;
    LocalDateTime createdAt;
    Set<Role> roles;
}
