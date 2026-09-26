package org.example.homnayangi.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // mặc định sẽ là private - nếu không khai báo rõ
public class IntrospectRequest {
    String token;
}
