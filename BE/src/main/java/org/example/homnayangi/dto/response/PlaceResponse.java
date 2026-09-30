package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlaceResponse {

    UUID id;
    String name;
    String category;
    Integer priceRange;
    String imageUrl;
    String address;
    BigDecimal latitude;
    BigDecimal longitude;
}
