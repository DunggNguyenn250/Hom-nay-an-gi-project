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
public class ExpenseSplitResponse {
    UUID expenseId;
    UserResponse user;
    BigDecimal amountOwed;
    boolean paid;
}
