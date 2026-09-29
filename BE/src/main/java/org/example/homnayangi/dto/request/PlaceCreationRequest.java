package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaceCreationRequest {

    @NotBlank(message = "PLACE_NAME_INVALID")
    String name;

    String category;

    Integer priceRange;

    String imageUrl;

    String address;

    @NotNull(message = "PLACE_LATITUDE_INVALID")
    @DecimalMin(value = "-90.0", message = "PLACE_LATITUDE_INVALID")
    @DecimalMax(value = "90.0", message = "PLACE_LATITUDE_INVALID")
    BigDecimal latitude;

    @NotNull(message = "PLACE_LONGITUDE_INVALID")
    @DecimalMin(value = "-180.0", message = "PLACE_LONGITUDE_INVALID")
    @DecimalMax(value = "180.0", message = "PLACE_LONGITUDE_INVALID")
    BigDecimal longitude;
}
