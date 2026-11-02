package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchmakingRequest {

    @NotNull(message = "PLACE_LATITUDE_INVALID")
    @DecimalMin(value = "-90.0", message = "PLACE_LATITUDE_INVALID")
    @DecimalMax(value = "90.0", message = "PLACE_LATITUDE_INVALID")
    BigDecimal latitude;

    @NotNull(message = "PLACE_LONGITUDE_INVALID")
    @DecimalMin(value = "-180.0", message = "PLACE_LONGITUDE_INVALID")
    @DecimalMax(value = "180.0", message = "PLACE_LONGITUDE_INVALID")
    BigDecimal longitude;

    @DecimalMin(value = "0.1", message = "Bán kính tìm kiếm phải lớn hơn 0")
    Double radius;

    @Min(value = 2, message = "Số lượng người ghép tối thiểu là 2")
    @Max(value = 10, message = "Số lượng người ghép tối đa là 10")
    Integer targetGroupSize;
}
