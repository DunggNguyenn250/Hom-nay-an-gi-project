package org.example.homnayangi.mapper;

import org.example.homnayangi.dto.response.FriendshipResponse;
import org.example.homnayangi.entity.Friendship;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface FriendshipMapper {
    FriendshipResponse toFriendshipResponse(Friendship friendship);
}
