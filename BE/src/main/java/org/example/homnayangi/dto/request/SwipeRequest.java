package org.example.homnayangi.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwipeRequest {
    private UUID roomId;  // Đổi từ Long thành UUID
    private UUID placeId; // Đổi từ Long thành UUID
    private boolean liked;
}
