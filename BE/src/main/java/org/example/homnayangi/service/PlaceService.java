package org.example.homnayangi.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.PlaceCreationRequest;
import org.example.homnayangi.dto.request.PlaceUpdateRequest;
import org.example.homnayangi.dto.response.PlaceResponse;
import org.example.homnayangi.entity.Place;
import org.example.homnayangi.exception.AppException;
import org.example.homnayangi.exception.ErrorCode;
import org.example.homnayangi.mapper.PlaceMapper;
import org.example.homnayangi.repository.PlaceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaceService {

    PlaceRepository placeRepository;
    PlaceMapper placeMapper;

    @CacheEvict(value = {"places_all", "places_category", "places_name", "places_nearby"}, allEntries = true)
    public PlaceResponse createPlace(PlaceCreationRequest request) {
        Place place = placeMapper.toPlace(request);
        return placeMapper.toPlaceResponse(placeRepository.save(place));
    }

    @CacheEvict(value = {"places_all", "places_category", "places_name", "places_nearby"}, allEntries = true)
    @CachePut(value = "places", key = "#id")
    public PlaceResponse updatePlace(UUID id, PlaceUpdateRequest request) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND));

        placeMapper.updatePlace(place, request);
        return placeMapper.toPlaceResponse(placeRepository.save(place));
    }

    @Cacheable(value = "places", key = "#id")
    public PlaceResponse getPlace(UUID id) {
        return placeMapper.toPlaceResponse(placeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND)));
    }

    @Cacheable(value = "places_all")
    public List<PlaceResponse> getPlaces() {
        return placeRepository.findAll().stream()
                .map(placeMapper::toPlaceResponse)
                .toList();
    }

    @Cacheable(value = "places_category", key = "#category")
    public List<PlaceResponse> searchPlacesByCategory(String category) {
        return placeRepository.findByCategoryContainingIgnoreCase(category).stream()
                .map(placeMapper::toPlaceResponse)
                .toList();
    }

    @Cacheable(value = "places_name", key = "#name")
    public List<PlaceResponse> searchPlacesByName(String name) {
        return placeRepository.findByNameContainingIgnoreCase(name).stream()
                .map(placeMapper::toPlaceResponse)
                .toList();
    }

    @Cacheable(value = "places_nearby", key = "{#latitude, #longitude, #radius, #category}")
    public List<PlaceResponse> getNearbyPlaces(double latitude, double longitude, double radius, String category) {
        List<Place> places;
        if (category != null && !category.trim().isEmpty()) {
            places = placeRepository.findNearbyPlacesByCategory(latitude, longitude, radius, category);
        } else {
            places = placeRepository.findNearbyPlaces(latitude, longitude, radius);
        }
        return places.stream()
                .map(placeMapper::toPlaceResponse)
                .toList();
    }

    @Caching(evict = {
        @CacheEvict(value = "places", key = "#id"),
        @CacheEvict(value = {"places_all", "places_category", "places_name", "places_nearby"}, allEntries = true)
    })
    public void deletePlace(UUID id) {
        if (!placeRepository.existsById(id)) {
            throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        }
        placeRepository.deleteById(id);
    }
}
