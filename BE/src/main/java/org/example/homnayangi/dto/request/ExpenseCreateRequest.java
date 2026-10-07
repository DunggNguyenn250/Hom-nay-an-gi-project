package org.example.homnayangi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.enums.SplitType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseCreateRequest {

    @NotNull(message = "roomId không được để trống")
    UUID roomId;

    @NotNull(message = "payerId không được để trống")
    UUID payerId;

    @NotNull(message = "Tổng tiền không được để trống")
    @DecimalMin(value = "0.01", message = "Tổng tiền phải lớn hơn 0")
    BigDecimal totalAmount;

    @NotEmpty(message = "Danh sách món không được rỗng")
    @Valid
    List<ExpenseItemRequest> items;

    /**
     * Kiểu chia tiền: EVENLY (chia đều) hoặc BY_ITEM (chia theo món).
     * Mặc định là EVENLY nếu không truyền.
     */
    @Builder.Default
    SplitType splitType = SplitType.EVENLY;
}
