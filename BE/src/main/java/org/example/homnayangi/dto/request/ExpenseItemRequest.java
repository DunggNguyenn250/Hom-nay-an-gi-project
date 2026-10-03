package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseItemRequest {

    @NotBlank(message = "Tên món không được để trống")
    String itemName;

    @NotNull(message = "Giá món không được để trống")
    @DecimalMin(value = "0.01", message = "Giá món phải lớn hơn 0")
    BigDecimal price;

    /**
     * Danh sách userId những người chia sẻ món này.
     * Nếu null hoặc rỗng -> mọi thành viên trong phòng đều chia sẻ món này.
     */
    List<UUID> sharerIds;
}
