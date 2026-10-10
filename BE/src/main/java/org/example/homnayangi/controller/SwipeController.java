package org.example.homnayangi.controller;

import lombok.RequiredArgsConstructor;
import org.example.homnayangi.dto.request.SwipeRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.RoomPlaceResultResponse;
import org.example.homnayangi.dto.response.SwipeResponse;
import org.example.homnayangi.service.SwipeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/swipes")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @PostMapping
    public ApiResponse<Void> swipe(@RequestBody SwipeRequest request) {
        swipeService.swipe(request);
        return ApiResponse.<Void>builder().build();
    }

    // 1. Lấy danh sách lượt quẹt của chính người dùng đang đăng nhập trong phòng
    @GetMapping("/rooms/{roomId}/me")
    public ApiResponse<List<SwipeResponse>> getMySwipesInRoom(@PathVariable UUID roomId) {
        return ApiResponse.<List<SwipeResponse>>builder()
                .code(1000)
                .message("Lấy lịch sử quẹt cá nhân trong phòng thành công")
                .result(swipeService.getMySwipesInRoom(roomId))
                .build();
    }

    // 2. Lấy thống kê kết quả quẹt địa điểm của tất cả thành viên trong phòng
    @GetMapping("/rooms/{roomId}/results")
    public ApiResponse<List<RoomPlaceResultResponse>> getRoomSwipeResults(@PathVariable UUID roomId) {
        return ApiResponse.<List<RoomPlaceResultResponse>>builder()
                .code(1000)
                .message("Lấy kết quả bình chọn địa điểm trong phòng thành công")
                .result(swipeService.getRoomSwipeResults(roomId))
                .build();
    }
}
