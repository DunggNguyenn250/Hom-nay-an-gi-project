package org.example.homnayangi.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.PlaceCreationRequest;
import org.example.homnayangi.dto.request.PlaceUpdateRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.PlaceResponse;
import org.example.homnayangi.service.PlaceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlaceController {

    PlaceService placeService;

    @PostMapping
    public ApiResponse<PlaceResponse> createPlace(@RequestBody @Valid PlaceCreationRequest request) {
        return ApiResponse.<PlaceResponse>builder()
                .code(1000)
                .message("Tạo địa điểm thành công")
                .result(placeService.createPlace(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<PlaceResponse>> getPlaces(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "5.0") Double radius) { // Mặc định 5km

        List<PlaceResponse> result;
        if (latitude != null && longitude != null) {
            result = placeService.getNearbyPlaces(latitude, longitude, radius, category);
        } else if (name != null && !name.trim().isEmpty()) {
            result = placeService.searchPlacesByName(name);
        } else if (category != null && !category.trim().isEmpty()) {
            result = placeService.searchPlacesByCategory(category);
        } else {
            result = placeService.getPlaces();
        }

        return ApiResponse.<List<PlaceResponse>>builder()
                .code(1000)
                .message("Lấy danh sách địa điểm thành công")
                .result(result)
                .build();
    }

    @GetMapping("/{placeId}")
    public ApiResponse<PlaceResponse> getPlace(@PathVariable UUID placeId) {
        return ApiResponse.<PlaceResponse>builder()
                .code(1000)
                .message("Lấy thông tin địa điểm thành công")
                .result(placeService.getPlace(placeId))
                .build();
    }

    @PutMapping("/{placeId}")
    public ApiResponse<PlaceResponse> updatePlace(
            @PathVariable UUID placeId,
            @RequestBody @Valid PlaceUpdateRequest request) {
        return ApiResponse.<PlaceResponse>builder()
                .code(1000)
                .message("Cập nhật thông tin địa điểm thành công")
                .result(placeService.updatePlace(placeId, request))
                .build();
    }

    @DeleteMapping("/{placeId}")
    public ApiResponse<String> deletePlace(@PathVariable UUID placeId) {
        placeService.deletePlace(placeId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa địa điểm thành công")
                .result("Place has been deleted successfully")
                .build();
    }
}
