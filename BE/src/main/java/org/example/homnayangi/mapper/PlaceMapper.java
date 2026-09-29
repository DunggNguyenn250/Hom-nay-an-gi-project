package org.example.homnayangi.mapper;

import org.example.homnayangi.dto.request.PlaceCreationRequest;
import org.example.homnayangi.dto.request.PlaceUpdateRequest;
import org.example.homnayangi.dto.response.PlaceResponse;
import org.example.homnayangi.entity.Place;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PlaceMapper {

    @Mapping(target = "id", ignore = true)
    Place toPlace(PlaceCreationRequest request);

    PlaceResponse toPlaceResponse(Place place);

    @Mapping(target = "id", ignore = true)
    void updatePlace(@MappingTarget Place place, PlaceUpdateRequest request);
}
