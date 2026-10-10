package org.example.homnayangi.mapper;

import org.example.homnayangi.dto.response.SwipeResponse;
import org.example.homnayangi.entity.Swipe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SwipeMapper {

    @Mapping(target = "placeId", source = "place.id")
    @Mapping(target = "placeName", source = "place.name") // Sửa 'name' thành tên thuộc tính tương ứng trong Place entity của bạn
    SwipeResponse toSwipeResponse(Swipe swipe);
}
