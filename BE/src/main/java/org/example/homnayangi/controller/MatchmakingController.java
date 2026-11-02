package org.example.homnayangi.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.MatchmakingRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.MatchmakingStatusResponse;
import org.example.homnayangi.dto.response.PublicRoomResponse;
import org.example.homnayangi.service.MatchmakingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matchmaking")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MatchmakingController {

    MatchmakingService matchmakingService;

    @PostMapping("/find")
    public ApiResponse<MatchmakingStatusResponse> findOrQueueMatch(@RequestBody @Valid MatchmakingRequest request) {
        MatchmakingStatusResponse response = matchmakingService.findOrQueueMatch(request);
        return ApiResponse.<MatchmakingStatusResponse>builder()
                .code(1000)
                .message("Yêu cầu tìm phòng ghép đã được ghi nhận")
                .result(response)
                .build();
    }

    @GetMapping("/status")
    public ApiResponse<MatchmakingStatusResponse> getMatchStatus() {
        MatchmakingStatusResponse response = matchmakingService.getMatchStatus();
        return ApiResponse.<MatchmakingStatusResponse>builder()
                .code(1000)
                .message("Lấy trạng thái ghép phòng thành công")
                .result(response)
                .build();
    }

    @DeleteMapping("/cancel")
    public ApiResponse<String> cancelMatch() {
        matchmakingService.cancelMatch();
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Đã hủy tìm kiếm ghép phòng thành công")
                .result("Hủy ghép phòng thành công")
                .build();
    }

    @GetMapping("/public-rooms")
    public ApiResponse<List<PublicRoomResponse>> getNearbyPublicRooms(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false, defaultValue = "5.0") Double radius
    ) {
        List<PublicRoomResponse> responseList = matchmakingService.getNearbyPublicRooms(latitude, longitude, radius);
        return ApiResponse.<List<PublicRoomResponse>>builder()
                .code(1000)
                .message("Lấy danh sách phòng công khai quanh đây thành công")
                .result(responseList)
                .build();
    }
}
