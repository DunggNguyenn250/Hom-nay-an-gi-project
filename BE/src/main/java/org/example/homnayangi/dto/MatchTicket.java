package org.example.homnayangi.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchTicket {
    UUID userId;
    String username;
    Double latitude;
    Double longitude;
    Double radius;
    Integer targetGroupSize;
    LocalDateTime queuedAt;
}
