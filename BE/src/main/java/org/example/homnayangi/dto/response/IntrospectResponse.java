package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE) // mặc định sẽ là private - nếu không khai báo rõ
public class IntrospectResponse {
    boolean valid;

}