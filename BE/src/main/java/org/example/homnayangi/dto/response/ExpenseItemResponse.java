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
public class ExpenseItemResponse {
    UUID id;
    String itemName;
    BigDecimal price;
    int sharerCount;
    BigDecimal pricePerPerson;
}
