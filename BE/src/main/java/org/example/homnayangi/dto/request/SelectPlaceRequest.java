package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SelectPlaceRequest {
    @NotNull(message = "ID địa điểm không được để trống")
    UUID placeId;
}
