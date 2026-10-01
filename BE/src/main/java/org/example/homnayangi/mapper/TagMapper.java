package org.example.homnayangi.mapper;

import org.example.homnayangi.dto.request.TagRequest;
import org.example.homnayangi.dto.response.TagResponse;
import org.example.homnayangi.dto.response.UserTagResponse;
import org.example.homnayangi.entity.Tag;
import org.example.homnayangi.entity.UserTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    @Mapping(target = "id", ignore = true)
    Tag toTag(TagRequest request);

    TagResponse toTagResponse(Tag tag);

    UserTagResponse toUserTagResponse(UserTag userTag);
}
