package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseResponse {
    UUID id;
    RoomResponse room;
    UserResponse payer;
    BigDecimal totalAmount;
    String splitType;
    LocalDateTime createdAt;
    List<ExpenseItemResponse> items;
    List<ExpenseSplitResponse> splits;
    boolean fullyPaid;
}
