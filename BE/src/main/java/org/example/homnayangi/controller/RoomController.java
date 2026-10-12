package org.example.homnayangi.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.homnayangi.dto.request.CreateRoomRequest;
import org.example.homnayangi.dto.response.ApiResponse;
import org.example.homnayangi.dto.response.RoomResponse;
import org.example.homnayangi.entity.Room;
import org.example.homnayangi.mapper.RoomMapper;
import org.example.homnayangi.service.RoomService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomController {

    RoomService roomService;
    RoomMapper roomMapper;

    // 1. Tạo phòng mới
    @PostMapping
    public ApiResponse<RoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        RoomResponse roomResponse = roomService.createRoom(request);
        return ApiResponse.<RoomResponse>builder()
                .code(1000)
                .message("Tạo phòng ăn thành công")
                .result(roomResponse)
                .build();
    }

    // 2. Tham gia phòng bằng roomCode
    @PostMapping("/join/{roomCode}")
    public ApiResponse<RoomResponse> joinRoom(@PathVariable String roomCode) {
        RoomResponse roomResponse = roomService.joinRoom(roomCode);
        return ApiResponse.<RoomResponse>builder()
                .code(1000)
                .message("Tham gia phòng thành công")
                .result(roomResponse)
                .build();
    }

    // 3. Lấy danh sách phòng của tôi (các phòng đang OPEN / SWIPING)
    @GetMapping("/my-rooms")
    public ApiResponse<List<RoomResponse>> getMyRooms() {
        List<RoomResponse> responseList = roomService.getMyRooms();

        return ApiResponse.<List<RoomResponse>>builder()
                .code(1000)
                .message("Lấy danh sách phòng của tôi thành công")
                .result(responseList)
                .build();
    }

    // 4. Lấy thông tin chi tiết phòng & danh sách thành viên
    @GetMapping("/{roomId}")
    public ApiResponse<RoomResponse> getRoomDetails(@PathVariable UUID roomId) {
        RoomResponse roomResponse = roomService.getRoomDetails(roomId);
        return ApiResponse.<RoomResponse>builder()
                .code(1000)
                .message("Lấy chi tiết phòng thành công")
                .result(roomResponse)
                .build();
    }

    // 5. Bắt đầu phiên quẹt chọn địa điểm (Chỉ Host mới có quyền)
    @PostMapping("/{roomId}/start")
    public ApiResponse<Void> startSwiping(@PathVariable UUID roomId) {
        roomService.startSwiping(roomId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Bắt đầu phiên quẹt chọn địa điểm thành công")
                .build();
    }

    // 6. Thành viên tự rời khỏi phòng
    @DeleteMapping("/{roomId}/leave")
    public ApiResponse<String> leaveRoom(@PathVariable UUID roomId) {
        roomService.leaveRoom(roomId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Rời phòng thành công")
                .result("Đã rời khỏi phòng thành công")
                .build();
    }

    // 7. Host xóa (kick) thành viên ra khỏi phòng
    @DeleteMapping("/{roomId}/members/{memberUserId}")
    public ApiResponse<String> kickMember(
            @PathVariable UUID roomId,
            @PathVariable UUID memberUserId
    ) {
        roomService.kickMember(roomId, memberUserId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Đã xóa thành viên ra khỏi phòng")
                .result("Xóa thành viên thành công")
                .build();
    }

    // 8. Host xóa hoàn toàn phòng ăn
    @DeleteMapping("/{roomId}")
    public ApiResponse<String> deleteRoom(@PathVariable UUID roomId) {
        roomService.deleteRoom(roomId);
        return ApiResponse.<String>builder()
                .code(1000)
                .message("Xóa phòng ăn thành công")
                .result("Đã xóa phòng ăn thành công")
                .build();
    }
    // Host chủ động đóng phòng để kết thúc phiên quẹt / chuyển sang chia tiền
    @PostMapping("/{roomId}/close")
    public ApiResponse<Void> closeRoom(@PathVariable UUID roomId) {
        roomService.closeRoom(roomId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã đóng phòng ăn thành công")
                .build();
    }


}