package org.example.homnayangi.mapper;

import org.example.homnayangi.dto.response.RoomResponse;
import org.example.homnayangi.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomResponse toRoomResponse(Room room);
}
