package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationRequest {

    @NotNull(message = "PLACE_LATITUDE_INVALID")
    @DecimalMin(value = "-90.0", message = "PLACE_LATITUDE_INVALID")
    @DecimalMax(value = "90.0", message = "PLACE_LATITUDE_INVALID")
    BigDecimal latitude;

    @NotNull(message = "PLACE_LONGITUDE_INVALID")
    @DecimalMin(value = "-180.0", message = "PLACE_LONGITUDE_INVALID")
    @DecimalMax(value = "180.0", message = "PLACE_LONGITUDE_INVALID")
    BigDecimal longitude;

    @DecimalMin(value = "0.1", message = "Bán kính tìm kiếm phải lớn hơn 0")
    @Builder.Default
    Double radius = 5.0; // Bán kính tìm kiếm mặc định 5km
}
