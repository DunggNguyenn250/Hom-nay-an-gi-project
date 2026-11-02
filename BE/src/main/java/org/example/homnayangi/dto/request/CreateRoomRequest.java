package org.example.homnayangi.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.enums.RoomType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoomRequest {
    String name;

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

    @Min(value = 3, message = "Số lượng quán phải từ 3 trở lên")
    @Max(value = 50, message = "Số lượng quán không được vượt quá 50")
    Integer deckSize;

    @Min(value = 2, message = "Số lượng thành viên tối thiểu là 2")
    @Max(value = 20, message = "Số lượng thành viên tối đa không vượt quá 20")
    Integer maxMembers;

    RoomType roomType;
}
