package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaceUpdateRequest {

    String name;

    String category;

    Integer priceRange;

    String imageUrl;

    String address;

    @DecimalMin(value = "-90.0", message = "PLACE_LATITUDE_INVALID")
    @DecimalMax(value = "90.0", message = "PLACE_LATITUDE_INVALID")
    BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "PLACE_LONGITUDE_INVALID")
    @DecimalMax(value = "180.0", message = "PLACE_LONGITUDE_INVALID")
    BigDecimal longitude;
}
