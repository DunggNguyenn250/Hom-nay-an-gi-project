package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocketEvent<T> {
    String type;
    UUID roomId;
    LocalDateTime timestamp;
    T data;
}
