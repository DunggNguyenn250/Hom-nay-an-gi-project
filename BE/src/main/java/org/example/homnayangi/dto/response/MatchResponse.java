package org.example.homnayangi.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchResponse {
    boolean isMatch;
    PlaceResponse matchedPlace;
}
